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
package io.wcm.dromas.pipeline.impl;

import io.wcm.dromas.io.http.ResilientHttp;
import io.wcm.dromas.io.http.request.Request;
import io.wcm.dromas.io.http.response.Response;
import io.wcm.dromas.pipeline.JsonPipeline;
import io.wcm.dromas.pipeline.JsonPipelineInputException;
import io.wcm.dromas.pipeline.JsonPipelineOutputException;
import io.wcm.dromas.pipeline.cache.CacheStrategy;
import io.wcm.dromas.pipeline.cache.spi.CacheAdapter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Default implementation of {@link JsonPipeline}.
 */
public final class JsonPipelineImpl implements JsonPipeline {

  private static final Logger log = LoggerFactory.getLogger(JsonPipelineImpl.class);

  private String serviceName;
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
    this.serviceName = serviceName;
    this.request = request;

    this.caching = caching;
    this.descriptor = "GET(" + request.url() + ")";

    this.dataSource = responseObservable
        .map(response -> {
          try {
            log.info("received " + response.status() + " response (" + response.reason() + ") with from " + request.url());
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
    clone.serviceName = this.serviceName;
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
        .map(arrayNode -> arrayNode.size() == 0 ? null : arrayNode.get(0))
        .map(jsonNode -> JacksonFunctions.wrapInObject(targetProperty, jsonNode));

    String transformationDesc = "EXTRACT(" + jsonPath + " INTO " + targetProperty + ")";
    return cloneWith(resultSource, transformationDesc);
  }


  @Override
  public JsonPipeline collect(String jsonPath, String targetProperty) {

    Observable<JsonNode> resultSource = dataSource
        .map(new JsonPathSelector(jsonPath))
        .map(arrayNode -> JacksonFunctions.wrapInObject(targetProperty, arrayNode));

    String transformationDesc = "COLLECT(" + jsonPath + " INTO " + targetProperty + ")";
    return cloneWith(resultSource, transformationDesc);
  }


  @Override
  public JsonPipeline merge(JsonPipeline secondarySource, String targetProperty) {

    Observable<JsonNode> zippedSource = dataSource.zipWith(secondarySource.getOutput(), (jsonFromPrimary, jsonFromSecondardy) -> {
      log.info("zipping object from secondary source into target property " + targetProperty);

      if (!(jsonFromPrimary instanceof ObjectNode)) {
        throw new JsonPipelineOutputException("Only pipelines with JSON *Objects* can be used as a target for a merge operation, but response data for "
            + this.getDescriptor() + " contained " + jsonFromPrimary.getClass().getSimpleName());
      }

      ObjectNode mergedObject = ((ObjectNode)jsonFromPrimary).deepCopy();
      mergedObject.set(targetProperty, jsonFromSecondardy);
      return mergedObject;
    });

    String transformationDesc = "MERGE(" + secondarySource.getDescriptor() + " INTO " + targetProperty + ")";
    return cloneWith(zippedSource, transformationDesc);
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
      final String cacheKey = caching.getCacheKey(serviceName, descriptor);

      // try to asynchronously(!) fetch the response from the cache (or simulate a cache miss if the headers suggest to ignore cache)
      Observable<String> cachedJsonString = (ignoreCache ? Observable.empty() : caching.get(cacheKey, strategy, request));

      // define what to do when the response is ready (or could not be retrieved from cache)
      cachedJsonString.subscribe(new Observer<String>() {

        private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

        private boolean cacheHit;

        @Override
        public void onNext(String jsonFromCache) {

          // the document could be retrieved, so forward it (parsed as a JsonNode) to the actual subscriber to the cachedSource
          cacheHit = true;
          log.info("CACHE HIT for " + cacheKey);
          subscriber.onNext(JacksonFunctions.stringToNode(jsonFromCache));
          subscriber.onCompleted();
        };

        @Override
        public void onCompleted() {
          if (!cacheHit) {
            // there was no emission, so the response has to be fetched from the service
            log.info("CACHE MISS for " + cacheKey + " fetching response through pipeline...");
            fetchAndStore();
          }
        }

        @Override
        public void onError(Throwable e) {
          // also fall back to the actual service if the couchbase request failed
          log.warn("Failed to connect to couchbase server, falling back to direct connection to " + serviceName);
          fetchAndStore();
        }

        private void fetchAndStore() {

          // fetch the output with a new subscription, which will also store the response in the cache when it is retrieved
          getOutput().subscribe(new Observer<JsonNode>() {

            @Override
            public void onNext(JsonNode fetchedNode) {
              log.info("response for " + descriptor + " has been fetched and will be put in the cache");

              JsonNode decoratedNode = decorateWithCacheInfo(fetchedNode);
              caching.put(cacheKey, JacksonFunctions.nodeToString(decoratedNode), strategy, request);

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

            private JsonNode decorateWithCacheInfo(JsonNode fetchedNode) {

              if (!(fetchedNode instanceof ObjectNode)) {
                log.warn("Can't add cache info to " + descriptor + ": fetchedNode is " + fetchedNode);
                return fetchedNode;
              }

              ObjectNode clone = fetchedNode.deepCopy();
              clone.putObject("_cacheInfo")
              .put("cacheKey", cacheKey)
              .put("pipeline", descriptor)
              .put("date", new SimpleDateFormat(PATTERN_RFC1123, Locale.US).format(new Date()));

              return clone;
            }

          });
        }
      });
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

}
