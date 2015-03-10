/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.pipeline.impl.operators;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.Exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * a rather complicated transformer that implements the pipelines caching capabilities
 */
public class CachePointTransformer implements Transformer<JsonPipelineOutput, JsonPipelineOutput> {

  private static final Logger log = LoggerFactory.getLogger(CachePointTransformer.class);

  private final CacheAdapter caching;
  private final List<CaravanHttpRequest> requests;
  private final String descriptor;
  private SortedSet<String> sourceServiceNames;
  private final CacheStrategy strategy;

  /**
   * @param caching the cache adapter to use
   * @param requests the outgoing REST request(s) used to obtain the JSON data to be cached
   * @param descriptor the unique id of the pipeline (to build a cache key)
   * @param sourceServiceNames names of the services (for logging)
   * @param strategy the CacheStrategy to get storage time and refresh interval
   */
  public CachePointTransformer(CacheAdapter caching, List<CaravanHttpRequest> requests, String descriptor, SortedSet<String> sourceServiceNames,
      CacheStrategy strategy) {
    super();
    this.caching = caching;
    this.requests = requests;
    this.descriptor = descriptor;
    this.sourceServiceNames = sourceServiceNames;
    this.strategy = strategy;
  }

  private String getSourceServicePrefix() {
    return StringUtils.join(sourceServiceNames, '+');
  }

  @Override
  public Observable<JsonPipelineOutput> call(Observable<JsonPipelineOutput> output) {

    // if "no-cache" is set in the request headers, then ignore entries from cache
    // and instead act as if there was an cache-miss, i.e. fetch the resource and put into the cache
    boolean ignoreCache = requests.get(0).getHeaderAsMap("Cache-Control").containsKey("no-cache");

    // the code within the lambda passed to Observable#create will be executed when subscribe is called on the "cachedSource" observable
    Observable<JsonPipelineOutput> cachedSource = Observable.create((subscriber) -> {

      // construct a unique cache key from the pipeline's descriptor
      final String cacheKey = caching.getCacheKey(getSourceServicePrefix(), descriptor);

      // the caching strategy determines if the storage time should be extended for cache hits(i.e. Time-to-Idle behaviour)
      boolean extendStorageTime = strategy.isExtendStorageTimeOnGet(requests);
      int storageTime = strategy.getStorageTime(requests);

      // try to asynchronously(!) fetch the response from the cache (or simulate a cache miss if the headers suggested to ignore cache)
      Observable<String> cachedJsonString = (ignoreCache ? Observable.empty() : caching.get(cacheKey, extendStorageTime, storageTime));

      // CacheResponseObserver will decide what to do when the response is ready (or could not be retrieved from cache)
      cachedJsonString.subscribe(new CacheResponseObserver(cacheKey, output, subscriber));
    });

    return cachedSource;
  }

  /**
   * an observer that is subscribed to the {@link Observable} returned by {@link CacheAdapter#get(String, boolean, int)}
   * , and is responsible for
   * <ul>
   * <li>unwrapping the JSON content from the caching envelope if it was successfully retrieved from cache</li>
   * <li>forwarding the unwrapped response to the subscriber given in the constructor</li>
   * <li>fetch the response from the Pipeline's dataSource if it couldn't be retrieved from cache</li>
   * <li>store the fetched responses to couchbase (wrapped in an envelope with metadata</li> *
   * </ul>
   */
  public final class CacheResponseObserver implements Observer<String> {

    /** a suffix being appended to the reason phrase for cached 404 responses */
    public static final String SUFFIX_FOR_CACHED_404_REASON_STRING = " (Cached!)";

    private final String cacheKey;
    private final Observable<JsonPipelineOutput> originalSource;
    private final Subscriber<? super JsonPipelineOutput> subscriber;

    private boolean cacheHit;

    private CacheResponseObserver(String cacheKey, Observable<JsonPipelineOutput> originalSource, Subscriber<? super JsonPipelineOutput> subscriberToForwardTo) {
      this.cacheKey = cacheKey;
      this.originalSource = originalSource;
      this.subscriber = subscriberToForwardTo;
    }

    @Override
    public void onNext(String cachedContent) {

      // the document could be retrieved, so forward it (parsed as a JsonNode) to the actual subscriber to the cachedSource
      log.debug("CACHE HIT for " + this.cacheKey);

      CacheEnvelope cacheEntry = CacheEnvelope.fromEnvelopeString(cachedContent, cacheKey);
      if (cacheEntry == null) {
        // ignore cache envelopes that can not be parsed
        return;
      }
      cacheHit = true;

      int responseAge = cacheEntry.getResponseAge();
      int refreshInterval = strategy.getRefreshInterval(requests);

      // check if the content from cache is fresh enough to serve it
      if (responseAge < refreshInterval) {

        serveCachedContent(cacheEntry, refreshInterval);
      }
      else {
        // this means the cached content is outdated - we better fetch the data from the backend
        log.debug("Cached content for " + cacheKey + " is available, but it's " + responseAge + " seconds old and considered stale.");

        fetchAndStore(new Subscriber<JsonPipelineOutput>() {

          @Override
          public void onNext(JsonPipelineOutput fetchedOutput) {
            log.debug("Instead of the stale content from cache, a brand new response has been fetched and stored for " + cacheKey);
            subscriber.onNext(fetchedOutput);
          }

          @Override
          public void onCompleted() {
            subscriber.onCompleted();
          }

          @Override
          public void onError(Throwable e) {
            Exceptions.throwIfFatal(e);

            log.warn("Using stale content from cache as a fallback after failing to fresh content for " + cacheKey, e);

            subscriber.onNext(new JsonPipelineOutputImpl(cacheEntry.getContentNode()).withMaxAge(refreshInterval));
            subscriber.onCompleted();
          }
        });
      }
    }

    private void serveCachedContent(CacheEnvelope cacheEntry, int refreshInterval) {

      if (cacheEntry.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        // the cache entry is a 404 response that should be thrown as an exception to be handled by the subscriber
        subscriber.onError(new JsonPipelineInputException(HttpStatus.SC_NOT_FOUND, cacheEntry.getReasonString() + SUFFIX_FOR_CACHED_404_REASON_STRING));
      }
      else {
        //  make sure to set the max-age content-header just to the time the cached content will become stale
        int maxAge = refreshInterval - cacheEntry.getResponseAge();

        subscriber.onNext(new JsonPipelineOutputImpl(cacheEntry.getContentNode()).withMaxAge(maxAge));
        subscriber.onCompleted();
      }
    }

    @Override
    public void onCompleted() {
      if (!cacheHit) {
        // there was no emission, so the response has to be fetched from the service
        log.debug("CACHE MISS for " + cacheKey + " fetching response from " + getSourceServicePrefix() + " through pipeline...");
        fetchAndStore(subscriber);
      }
    }

    @Override
    public void onError(Throwable e) {
      Exceptions.throwIfFatal(e);

      // also fall back to the actual service if the couchbase request failed
      log.warn("Failed to connect to couchbase server, falling back to direct connection to " + getSourceServicePrefix(), e);
      fetchAndStore(subscriber);
    }

    private void fetchAndStore(Subscriber<? super JsonPipelineOutput> backendResponseSubscriber) {

      // fetch the output with a new subscription, which will also store the response in the cache when it is retrieved
      originalSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput fetchedModel) {
          log.debug("response for " + descriptor + " has been fetched and will be put in the cache");

          int storageTime = strategy.getStorageTime(requests);
          int refreshInterval = Math.max(strategy.getRefreshInterval(requests), fetchedModel.getMaxAge());

          CacheEnvelope cacheEntry = CacheEnvelope.from200Response(fetchedModel.getPayload(), sourceServiceNames, requests, cacheKey, descriptor);
          caching.put(cacheKey, cacheEntry.getEnvelopeString(), storageTime);

          // everything else is just forwarding to the subscriber to the cachedSource
          backendResponseSubscriber.onNext(fetchedModel.withMaxAge(refreshInterval));
        }

        @Override
        public void onCompleted() {
          backendResponseSubscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          Exceptions.throwIfFatal(e);

          if (e instanceof JsonPipelineInputException) {
            if (((JsonPipelineInputException)e).getStatusCode() == HttpStatus.SC_NOT_FOUND) {

              log.debug("404 response for " + descriptor + " will be stored in the cache");

              int storageTime = strategy.getStorageTime(requests);

              CacheEnvelope cacheEntry = CacheEnvelope.from404Response(e.getMessage(), sourceServiceNames, requests, cacheKey, descriptor);
              caching.put(cacheKey, cacheEntry.getEnvelopeString(), storageTime);
            }
          }
          backendResponseSubscriber.onError(e);
        }
      });
    }
  }

  /**
   * Implements generation and parsing of the cache "envelope" document, that wraps the JSON output of the pipeline to
   * be able to store additional metadata in the cache
   */
  public static final class CacheEnvelope {

    private static final String CACHE_METADATA_PROPERTY = "metadata";
    private static final String CACHE_CONTENT_PROPERTY = "content";

    private final ObjectNode envelopeNode;
    private final ObjectNode metadataNode;
    private final JsonNode contentNode;

    private CacheEnvelope(ObjectNode envelopeNode) {
      this.envelopeNode = envelopeNode;
      metadataNode = (ObjectNode)envelopeNode.get(CACHE_METADATA_PROPERTY);
      contentNode = envelopeNode.get(CACHE_CONTENT_PROPERTY);
    }

    /**
     * Parse a JSON string that was obtained from the couchbase cache
     * @param jsonString
     * @param cacheKey
     * @return the CacheEntry - or null if the json String was not in the expected format
     */
    public static CacheEnvelope fromEnvelopeString(String jsonString, String cacheKey) {
      try {
        ObjectNode envelopeFromCache = JacksonFunctions.stringToObjectNode(jsonString);
        if (!envelopeFromCache.has(CACHE_METADATA_PROPERTY) || !envelopeFromCache.has(CACHE_CONTENT_PROPERTY)) {
          log.warn("Ignoring cached document " + cacheKey + ", because it doesn't have the expected metadata/content envelope.");
          return null;
        }

        return new CacheEnvelope(envelopeFromCache);
      }
      catch (JsonPipelineInputException e) {
        log.warn("Failed parse cached JSON document frmo " + cacheKey, e);
        return null;
      }
    }

    /**
     * Create a new CacheEnvelope to store in the couchbase cache
     * @param contentNode
     * @param sourceServiceNames
     * @param cacheKey
     * @param pipelineDescriptor
     * @return the new CacheEnvelope instance
     */
    public static CacheEnvelope from200Response(JsonNode contentNode, SortedSet<String> sourceServiceNames, List<CaravanHttpRequest> requests, String cacheKey,
        String pipelineDescriptor) {

      ObjectNode envelope = createEnvelopeNode(contentNode, HttpStatus.SC_OK, sourceServiceNames, requests, cacheKey, pipelineDescriptor, null);
      return new CacheEnvelope(envelope);
    }

    /**
     * Create a new CacheEnvelope to store in the couchbase cache
     * @param reason
     * @param sourceServiceNames
     * @param cacheKey
     * @param pipelineDescriptor
     * @return the new CacheEnvelope instance
     */
    public static CacheEnvelope from404Response(String reason, SortedSet<String> sourceServiceNames, List<CaravanHttpRequest> requests, String cacheKey,
        String pipelineDescriptor) {

      JsonNode contentNode = JacksonFunctions.emptyObject();
      int statusCode = HttpStatus.SC_NOT_FOUND;

      ObjectNode envelope = createEnvelopeNode(contentNode, statusCode, sourceServiceNames, requests, cacheKey, pipelineDescriptor, reason);
      return new CacheEnvelope(envelope);
    }

    private static ObjectNode createEnvelopeNode(JsonNode contentNode, int statusCode, SortedSet<String> sourceServiceNames, List<CaravanHttpRequest> requests,
        String cacheKey,
        String pipelineDescriptor, String reason) {

      ObjectNode envelope = JacksonFunctions.emptyObject();
      ObjectNode metadata = envelope.putObject(CACHE_METADATA_PROPERTY);

      metadata.put("cacheKey", cacheKey);
      metadata.set("sources", JacksonFunctions.pojoToNode(sourceServiceNames));
      metadata.put("pipeline", pipelineDescriptor);
      metadata.put("generated", CacheDateUtils.formatCurrentTime());

      metadata.put("statusCode", statusCode);

      List<String> sourcePaths = new ArrayList<String>();
      for (CaravanHttpRequest req : requests) {
        sourcePaths.add(StringUtils.substringBefore(req.url(), "?"));
      }
      metadata.set("sourcePaths", JacksonFunctions.pojoToNode(sourcePaths));


      if (StringUtils.isNotBlank(reason)) {
        metadata.put("reason", reason);
      }

      envelope.set(CACHE_CONTENT_PROPERTY, contentNode);
      return envelope;
    }


    /**
     * @return the full envelope (as JSON string) to be stored in the cache
     */
    public String getEnvelopeString() {
      return JacksonFunctions.nodeToString(envelopeNode);
    }

    JsonNode getContentNode() {
      return contentNode;
    }

    int getStatusCode() {
      return metadataNode.at("/statusCode").asInt(HttpStatus.SC_OK);
    }

    String getReasonString() {
      return metadataNode.at("/reason").asText("Not Found");
    }

    int getResponseAge() {
      String generatedDate = metadataNode.at("/generated").asText();
      return CacheDateUtils.getSecondsSince(generatedDate);
    }

    /**
     * @param newDate
     */
    public void setGeneratedDate(String newDate) {
      metadataNode.put("generated", newDate);
    }
  }
}
