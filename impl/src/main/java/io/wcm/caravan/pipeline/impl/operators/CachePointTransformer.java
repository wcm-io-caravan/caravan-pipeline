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

import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collection;

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
  private final Request request;
  private final String descriptor;
  private final String sourceServicePrefix;
  private final CacheStrategy strategy;

  /**
   * @param caching the cache adapter to use
   * @param request the outgoing request
   * @param descriptor the unique id of the pipeline (to build a cache key)
   * @param sourceServicePrefix name(s) of the services (for logging)
   * @param strategy the CacheStrategy to get storage time and refresh interval
   */
  public CachePointTransformer(CacheAdapter caching, Request request, String descriptor, String sourceServicePrefix, CacheStrategy strategy) {
    super();
    this.caching = caching;
    this.request = request;
    this.descriptor = descriptor;
    this.sourceServicePrefix = sourceServicePrefix;
    this.strategy = strategy;
  }

  @Override
  public Observable<JsonPipelineOutput> call(Observable<JsonPipelineOutput> output) {

    // if "no-cache" is set in the request headers, then ignore entries from cache
    // instead act as there was an cache-miss, i.e. fetch the resource and put into the cache
    Collection<String> cacheControl = request.headers().get("Cache-Control");
    boolean ignoreCache = (cacheControl != null && cacheControl.contains("no-cache"));

    // the code within the lambda passed to Observable#create will be executed when subscribe is called on the "cachedSource" observable
    Observable<JsonPipelineOutput> cachedSource = Observable.create((subscriber) -> {

      // construct a unique cache key from the pipeline's descriptor
      final String cacheKey = caching.getCacheKey(sourceServicePrefix, descriptor);

      // try to asynchronously(!) fetch the response from the cache (or simulate a cache miss if the headers suggest to ignore cache)
      boolean extendStorageTime = strategy.isExtendStorageTimeOnGet(request);
      int storageTIme = strategy.getStorageTime(request);

      Observable<String> cachedJsonString = (ignoreCache ? Observable.empty() : caching.get(cacheKey, extendStorageTime, storageTIme));

      // CacheResponseObserver will decide what to do when the response is ready (or could not be retrieved from cache)
      cachedJsonString.subscribe(new CacheResponseObserver(cacheKey, output, subscriber));
    });

    return cachedSource;
  }

  /**
   * an observer that is subscribed to the {@link Observable} returned by
   * {@link CacheAdapter#get(String, boolean, int)} , and is responsible for
   * <ul>
   * <li>unwrapping the JSON content from the caching envelope if it was succesfully retrieved from cache</li>
   * <li>forwarding the unwrapped repsonse to the subscriber given in the construtor</li>
   * <li>fetch the response from the Pipeline's dataSource if it couldn't be retrieved from cache</li>
   * <li>store the fetched responses to couchbase (wrapped in an envlope with metadata</li> *
   * </ul>
   */
  public final class CacheResponseObserver implements Observer<String> {

    private static final String CACHE_METADATA_PROPERTY = "metadata";
    private static final String CACHE_CONTENT_PROPERTY = "content";

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

      ObjectNode envelopeFromCache = JacksonFunctions.stringToObjectNode(cachedContent);
      if (!envelopeFromCache.has(CACHE_METADATA_PROPERTY) || !envelopeFromCache.has(CACHE_CONTENT_PROPERTY)) {
        log.warn("Ignoring cached document " + this.cacheKey + ", because it doesn't have the expected metadata/content envelope.");
        return;
      }

      cacheHit = true;

      JsonNode contentFromCache = envelopeFromCache.get(CACHE_CONTENT_PROPERTY);
      JsonNode metadataFromCache = envelopeFromCache.get(CACHE_METADATA_PROPERTY);

      String generatedDate = metadataFromCache.at("/generated").asText();
      int statusCode = metadataFromCache.at("/statusCode").asInt(HttpStatus.SC_OK);

      int responseAge = CacheDateUtils.getSecondsSince(generatedDate);
      int refreshInterval = strategy.getRefreshInterval(request);

      if (responseAge < refreshInterval) {
        // the content from cache is fresh enough to serve it

        if (statusCode == HttpStatus.SC_NOT_FOUND) {

          String reason = contentFromCache.at("/reason").asText("Not Found");
          subscriber.onError(new JsonPipelineInputException(HttpStatus.SC_NOT_FOUND, reason + SUFFIX_FOR_CACHED_404_REASON_STRING));
        }
        else {
          //  make sure to set the max-age content-header just to the time the cached content will become stale
          int maxAge = refreshInterval - responseAge;

          subscriber.onNext(new JsonPipelineOutputImpl(contentFromCache).withMaxAge(maxAge));
          subscriber.onCompleted();
        }
      }
      else {
        // this means the cached content is outdated - we better fetch the data from the backend
        log.info("Cached content for " + cacheKey + " is available, but it's " + responseAge + " seconds old and considered stale.");

        fetchAndStore(new Subscriber<JsonPipelineOutput>() {

          @Override
          public void onNext(JsonPipelineOutput fetchedOutput) {
            log.info("Instead of the stale content from cache, a brand new response has been fetched and stored for " + cacheKey);
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

            subscriber.onNext(new JsonPipelineOutputImpl(contentFromCache).withMaxAge(refreshInterval));
            subscriber.onCompleted();
          }
        });
      }
    }

    @Override
    public void onCompleted() {
      if (!cacheHit) {
        // there was no emission, so the response has to be fetched from the service
        log.debug("CACHE MISS for " + cacheKey + " fetching response from " + sourceServicePrefix + " through pipeline...");
        fetchAndStore(subscriber);
      }
    }

    @Override
    public void onError(Throwable e) {
      Exceptions.throwIfFatal(e);

      // also fall back to the actual service if the couchbase request failed
      log.warn("Failed to connect to couchbase server, falling back to direct connection to " + sourceServicePrefix, e);
      fetchAndStore(subscriber);
    }

    private void fetchAndStore(Subscriber<? super JsonPipelineOutput> backendResponseSubscriber) {

      // fetch the output with a new subscription, which will also store the response in the cache when it is retrieved
      originalSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput fetchedModel) {
          log.debug("response for " + descriptor + " has been fetched and will be put in the cache");

          int storageTime = strategy.getStorageTime(request);
          int refreshInterval = Math.max(strategy.getRefreshInterval(request), fetchedModel.getMaxAge());

          ObjectNode wrappedNode = wrapInEnvelope(fetchedModel.getPayload(), HttpStatus.SC_OK);
          caching.put(cacheKey, JacksonFunctions.nodeToString(wrappedNode), storageTime);

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

              log.info("404 response for " + descriptor + " will be stored in the cache");

              int storageTime = strategy.getStorageTime(request);

              ObjectNode content = JacksonFunctions.emptyObject().put("reason", e.getMessage());
              ObjectNode envelope = wrapInEnvelope(content, HttpStatus.SC_NOT_FOUND);
              caching.put(cacheKey, JacksonFunctions.nodeToString(envelope), storageTime);
            }
          }
          backendResponseSubscriber.onError(e);
        }

        private ObjectNode wrapInEnvelope(JsonNode fetchedNode, int statusCode) {

          ObjectNode envelope = JacksonFunctions.emptyObject();

          ObjectNode metadata = envelope.putObject(CACHE_METADATA_PROPERTY);

          metadata.put("cacheKey", cacheKey);
          metadata.set("sources", JacksonFunctions.pojoToNode(sourceServicePrefix));
          metadata.put("pipeline", descriptor);
          metadata.put("generated", CacheDateUtils.formatCurrentTime());
          metadata.put("expiry", strategy.getStorageTime(request));
          metadata.put("resetExpiryOnGet", strategy.isExtendStorageTimeOnGet(request));
          metadata.put("statusCode", statusCode);

          envelope.set(CACHE_CONTENT_PROPERTY, fetchedNode);

          return envelope;
        }
      });
    }
  }
}