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
package io.wcm.caravan.pipeline.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.response.Response;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Default implementation of {@link JsonPipeline}.
 */
public final class JsonPipelineImpl implements JsonPipeline {


  private static final Logger log = LoggerFactory.getLogger(JsonPipelineImpl.class);

  /** the property name to use if caching metadata is also embedded in the output JSON */
  private static final String CACHE_METADATA_OUTPUT_PROPERTY = "_cacheMetadata";


  private static JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);

  private SortedSet<String> sourceServiceNames = new TreeSet<String>();
  private Request request;

  private CacheAdapter caching;
  private String descriptor;

  private Observable<JsonNode> dataSource;

  /**
   * @param serviceName the logical service name. Will be used as a namespace for cache keys
   * @param request the REST request that provides the soruce data
   * @param responseObservable the response observable obtained by the {@link ResilientHttp}
   * @param caching the caching layer to use
   */
  JsonPipelineImpl(String serviceName, Request request, Observable<Response> responseObservable, CacheAdapter caching) {

    if (isNotBlank(serviceName)) {
      this.sourceServiceNames.add(serviceName);
    }
    this.request = request;

    this.caching = caching;
    this.descriptor = isNotBlank(request.url()) ? "GET(" + request.url() + ")" : "EMPTY()";

    this.dataSource = responseObservable
        .map(response -> {
          try {
            log.debug("received " + response.status() + " response (" + response.reason() + ") with from " + request.url());
            return response.body().asString();
          }
          catch (IOException ex) {
            throw new JsonPipelineInputException("Failed to read JSON response from " + request.url(), ex);
          }
        }).map(JacksonFunctions::stringToNode);
  }

  private JsonPipelineImpl() {
    // only used internally
  }

  private JsonPipelineImpl cloneWith(Observable<JsonNode> newSource, String descriptorSuffix) {
    JsonPipelineImpl clone = new JsonPipelineImpl();
    clone.sourceServiceNames.addAll(this.sourceServiceNames);
    clone.request = this.request;

    clone.caching = this.caching;
    clone.descriptor = this.descriptor;
    if (StringUtils.isNotBlank(descriptorSuffix)) {
      clone.descriptor += "+" + descriptorSuffix;
    }

    clone.dataSource = newSource;
    return clone;
  }

  @Override
  public String getDescriptor() {
    return descriptor;
  }

  @Override
  public SortedSet<String> getSourceServices() {
    return this.sourceServiceNames;
  }

  @Override
  public JsonPipeline assertExists(String jsonPath, RuntimeException ex) {

    Observable<JsonNode> assertingSource = Observable.create(subscriber -> {

      getOutput().subscribe(new Observer<JsonNode>() {

        private boolean assertionFailed;

        @Override
        public void onNext(JsonNode responseNode) {
          try {
            ArrayNode jsonPathResult = new JsonPathSelector(jsonPath).call(responseNode);

            if (jsonPathResult.size() == 0) {
              // the JSONpath was valid, but it just didn't match to any result
              this.onError(ex);
            }
            else {
              // the responseNode has content at the given JsoNPath, so we can
              subscriber.onNext(responseNode);
            }
          }
          catch (PathNotFoundException p) {
            // the JSONpath didn't match the structure of the response
            this.onError(ex);
          }
        }

        @Override
        public void onError(Throwable e) {
          assertionFailed = true;
          subscriber.onError(e);
        }

        @Override
        public void onCompleted() {
          if (!assertionFailed) {
            subscriber.onCompleted();
          }
        }

      });
    });

    return cloneWith(assertingSource, null);
  }


  @Override
  public JsonPipeline extract(String jsonPath, String targetProperty) {

    Observable<JsonNode> resultSource = dataSource
        .map(new JsonPathSelector(jsonPath))
        .map(arrayNode -> arrayNode.size() == 0 ? null : arrayNode.get(0));

    if (isNotBlank(targetProperty)) {
      resultSource = resultSource.map(jsonNode -> JacksonFunctions.wrapInObject(targetProperty, jsonNode));
    }

    String targetSuffix = isNotBlank(targetProperty) ? " INTO " + targetProperty : "";
    String transformationDesc = "EXTRACT(" + jsonPath + targetSuffix + ")";
    return cloneWith(resultSource, transformationDesc);
  }


  @Override
  public JsonPipeline collect(String jsonPath, String targetProperty) {

    Observable<JsonNode> resultSource = dataSource
        .map(new JsonPathSelector(jsonPath));

    if (isNotBlank(targetProperty)) {
      resultSource = resultSource.map(arrayNode -> JacksonFunctions.wrapInObject(targetProperty, arrayNode));
    }

    String targetSuffix = isNotBlank(targetProperty) ? " INTO " + targetProperty : "";
    String transformationDesc = "COLLECT(" + jsonPath + targetSuffix + ")";
    return cloneWith(resultSource, transformationDesc);
  }


  @Override
  public JsonPipeline merge(JsonPipeline secondarySource, String targetProperty) {

    Observable<JsonNode> zippedSource = dataSource.zipWith(secondarySource.getOutput(), (jsonFromPrimary, jsonFromSecondary) -> {
      log.debug("zipping object from secondary source into target property " + targetProperty);

      if (!(jsonFromPrimary instanceof ObjectNode)) {
        throw new JsonPipelineOutputException("Only pipelines with JSON *Objects* can be used as a target for a merge operation, but response data for "
            + this.getDescriptor() + " contained " + jsonFromPrimary.getClass().getSimpleName());
      }

      // start with cloning the the response of the primary pipeline
      ObjectNode mergedObject = jsonFromPrimary.deepCopy();

      // if a target property is specified, the JSON to be merged is inserted into this property
      if (isNotBlank(targetProperty)) {
        mergedObject.set(targetProperty, jsonFromSecondary);
      }
      else {

        // if no target property is specified, all properties of the secondary pipeline are copied into the merged object
        if (!(jsonFromSecondary instanceof ObjectNode)) {
          throw new JsonPipelineOutputException("Only pipelines with JSON *Object* responses can be merged without specify a target property");
        }

        mergeAllPropertiesInto((ObjectNode)jsonFromSecondary, mergedObject);
      }

      return mergedObject;
    });

    String targetSuffix = isNotBlank(targetProperty) ? " INTO " + targetProperty : "";
    String transformationDesc = "MERGE(" + secondarySource.getDescriptor() + targetSuffix + ")";

    JsonPipelineImpl mergedPipeline = cloneWith(zippedSource, transformationDesc);
    mergedPipeline.sourceServiceNames.addAll(secondarySource.getSourceServices());
    return mergedPipeline;
  }

  private void mergeAllPropertiesInto(ObjectNode nodeToMerge, ObjectNode targetNode) {

    // iterate over all properties of the given node
    Iterator<Entry<String, JsonNode>> propertyIterator = nodeToMerge.fields();
    while (propertyIterator.hasNext()) {
      Entry<String, JsonNode> nextProperty = propertyIterator.next();
      String propertyName = nextProperty.getKey();

      // what to do if the property already exists?
      if (targetNode.has(propertyName)) {

        // if it's just a _cacheMetadata property: increase counter suffix until there is no collision
        if (propertyName == CACHE_METADATA_OUTPUT_PROPERTY) {
          int suffix = 0;
          do {
            suffix++;
            propertyName = nextProperty.getKey() + "#" + suffix;
          }
          while (targetNode.has(propertyName));
        }
        else {

          // otherwise throw an exception,
          throw new JsonPipelineOutputException("Target pipeline " + this.getDescriptor() + " already has a property named " + propertyName);
        }
      }

      targetNode.set(propertyName, nextProperty.getValue());
    }
  }

  @Override
  public JsonPipeline addCachePoint(CacheStrategy strategy) {

    // skip all caching logic if the expiry time for this request is 0
    if (strategy.getExpirySeconds(request) == 0) {
      return this;
    }

    // if "no-cache" is set in the request headers, then ignore entries from cache
    // instead act as there was an cache-miss, i.e. fetch the resource and put into the cache
    Collection<String> cacheControl = request.headers().get("Cache-Control");
    boolean ignoreCache = (cacheControl != null && cacheControl.contains("no-cache"));

    // the code within the lambda passed to Observable#create will be executed when subscribe is called on the "cachedSource" observable
    Observable<JsonNode> cachedSource = Observable.create((subscriber) -> {

      // construct a unique cache key from the pipeline's descriptor
      final String cacheKey = caching.getCacheKey(getSourceServicePrefix(), descriptor);

      // try to asynchronously(!) fetch the response from the cache (or simulate a cache miss if the headers suggest to ignore cache)
      Observable<String> cachedJsonString = (ignoreCache ? Observable.empty() : caching.get(cacheKey, strategy, request));

      // CacheResponseObserver will decide what to do when the response is ready (or could not be retrieved from cache)
      cachedJsonString.subscribe(new CacheResponseObserver(cacheKey, strategy, subscriber));
    });

    return cloneWith(cachedSource, null);
  }

  @Override
  public Observable<JsonNode> getOutput() {
    return dataSource;
  }

  @Override
  public Observable<String> getStringOutput() {
    return dataSource.map(JacksonFunctions::nodeToString);
  }

  @Override
  public <T> Observable<T> getTypedOutput(Class<T> clazz) {
    return dataSource.map(JacksonFunctions.nodeToPojo(clazz));
  }

  private String getSourceServicePrefix() {
    return StringUtils.join(sourceServiceNames, '+');
  }


  /**
   * an observer that is subscribed to the {@link Observable} returned by
   * {@link CacheAdapter#get(String, CacheStrategy, Request)}, and is responsible for
   * <ul>
   * <li>unwrapping the JSON content from the caching envelope if it was succesfully retrieved from cache</li>
   * <li>forwarding the unwrapped repsonse to the subscriber given in the construtor</li>
   * <li>fetch the response from the Pipeline's dataSource if it couldn't be retrieved from cache</li>
   * <li>store the fetched responses to couchbase (wrapped in an envlope with metadata</li> *
   * </ul>
   * TODO: this should be moved into a top-level class, but it still contains lots of references to {@link JsonPipeline}
   * internal's
   */
  private final class CacheResponseObserver implements Observer<String> {

    private static final String CACHE_METADATA_PROPERTY = "metadata";
    private static final String CACHE_CONTENT_PROPERTY = "content";

    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    private final String cacheKey;
    private final CacheStrategy strategy;
    private final Subscriber<? super JsonNode> subscriber;

    private boolean cacheHit;
    private boolean includeCacheMetadataInResponse;

    private CacheResponseObserver(String cacheKey, CacheStrategy strategy, Subscriber<? super JsonNode> subscriberToForwardTo) {
      this.cacheKey = cacheKey;
      this.strategy = strategy;
      this.subscriber = subscriberToForwardTo;
    }

    @Override
    public void onNext(String jsonFromCache) {

      // the document could be retrieved, so forward it (parsed as a JsonNode) to the actual subscriber to the cachedSource
      log.debug("CACHE HIT for " + this.cacheKey);

      ObjectNode envelopeFromCache = JacksonFunctions.stringToObjectNode(jsonFromCache);
      if (!envelopeFromCache.has(CACHE_METADATA_PROPERTY) || !envelopeFromCache.has(CACHE_CONTENT_PROPERTY)) {
        log.warn("Ignoring cached document " + this.cacheKey + ", because it doesn't have the expected metadata/content envelope.");
        return;
      }

      JsonNode contentFromCache = envelopeFromCache.get(CACHE_CONTENT_PROPERTY);

      // optionally, the cache metadata can also be included in the response to help debugging
      if (includeCacheMetadataInResponse && contentFromCache instanceof ObjectNode) {
        ObjectNode decoratedNode = contentFromCache.deepCopy();

        decoratedNode.set(CACHE_METADATA_OUTPUT_PROPERTY, envelopeFromCache.get(CACHE_METADATA_PROPERTY));
        contentFromCache = decoratedNode;
      }

      cacheHit = true;

      subscriber.onNext(contentFromCache);
      subscriber.onCompleted();
    }

    @Override
    public void onCompleted() {
      if (!cacheHit) {
        // there was no emission, so the response has to be fetched from the service
        log.debug("CACHE MISS for " + this.cacheKey + " fetching response from " + getSourceServicePrefix() + " through pipeline...");
        fetchAndStore();
      }
    }

    @Override
    public void onError(Throwable e) {
      // also fall back to the actual service if the couchbase request failed
      log.warn("Failed to connect to couchbase server, falling back to direct connection to " + getSourceServicePrefix());
      fetchAndStore();
    }

    private void fetchAndStore() {

      // fetch the output with a new subscription, which will also store the response in the cache when it is retrieved
      getOutput().subscribe(new Observer<JsonNode>() {

        @Override
        public void onNext(JsonNode fetchedNode) {
          log.debug("response for " + descriptor + " has been fetched and will be put in the cache");

          ObjectNode wrappedNode = wrapInEnvelope(fetchedNode);
          caching.put(cacheKey, JacksonFunctions.nodeToString(wrappedNode), strategy, request);

          // everything else is just forwarding to the subscriber to the cachedSource
          subscriber.onNext(fetchedNode);
        }

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          subscriber.onError(e);
        }

        private ObjectNode wrapInEnvelope(JsonNode fetchedNode) {

          ObjectNode envelope = nodeFactory.objectNode();

          ObjectNode metadata = envelope.putObject(CACHE_METADATA_PROPERTY);

          metadata.put("cacheKey", cacheKey);
          metadata.set("sources", JacksonFunctions.pojoToNode(getSourceServices()));
          metadata.put("pipeline", descriptor);
          metadata.put("generated", new SimpleDateFormat(PATTERN_RFC1123, Locale.US).format(new Date()));
          metadata.put("expiry", strategy.getExpirySeconds(request));
          metadata.put("resetExpiryOnGet", strategy.isResetExpiryOnGet(request));

          envelope.set(CACHE_CONTENT_PROPERTY, fetchedNode);

          return envelope;
        }
      });
    }
  }
}
