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
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.response.Response;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

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

  private static JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);

  private SortedSet<String> sourceServiceNames = new TreeSet<String>();
  private Request request;

  private CacheAdapter caching;
  private String descriptor;

  private Observable<JsonPipelineOutput> dataSource;

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

    this.dataSource = Observable.create(subscriber -> {

      responseObservable.subscribe(new Observer<Response>() {

        @Override
        public void onNext(Response response) {
          try {
            int statusCode = response.status();
            log.debug("received " + statusCode + " response (" + response.reason() + ") with from " + request.url());
            if (statusCode == HttpServletResponse.SC_OK) {

              JsonNode payload = JacksonFunctions.stringToNode(response.body().asString());
              JsonPipelineOutput model = new JsonPipelineOutputImpl(payload);

              if (response.headers() != null && response.headers().get("Cache-Control") != null) {
                // TODO: this extracting of specific cache-control should be moved into Response class
                for (String cacheControl : response.headers().get("Cache-Control")) {
                  if (cacheControl.startsWith("max-age:")) {
                    // if the response already contain a max-age header then respect that value
                    int maxAge = NumberUtils.toInt(StringUtils.substringAfter(cacheControl, ":").trim());
                    if (maxAge > 0) {
                      model = model.withMaxAge(maxAge);
                    }
                  }
                }
              }

              subscriber.onNext(model);
            }
            else {

              String msg = "Request for " + request.url() + " failed with HTTP status code: " + statusCode + " (" + response.reason() + ")";
              log.warn(msg);

              ObjectNode errorObj = JacksonFunctions.emptyObject();
              errorObj.putObject("error")
              .put("status", statusCode)
              .put("reason", response.reason())
              .put("url", request.url());

              JsonPipelineOutput model = new JsonPipelineOutputImpl(errorObj).withStatusCode(statusCode).withMaxAge(0);
              subscriber.onNext(model);

              //subscriber.onError(new JsonPipelineInputException(statusCode, msg));
            }
          } catch (IOException ex) {
            subscriber.onError(new JsonPipelineInputException(500, "Failed to read JSON response from " + request.url(), ex));
          }
        }

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          /* TODO: consider to also wrap these exceptions in an JsonPipelineInputException with the following code
          int statusCode = 500;
          if (e instanceof IllegalResponseRuntimeException) {
            statusCode = ((IllegalResponseRuntimeException)e).getResponseStatusCode();
          }
          subscriber.onError(new JsonPipelineInputException(statusCode, "An exception occured connecting to " + request.url(), e));
           */
          subscriber.onError(e);
        }
      });

    });
  }

  private JsonPipelineImpl() {
    // only used internally
  }

  private JsonPipelineImpl cloneWith(Observable<JsonPipelineOutput> newSource, String descriptorSuffix) {
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

    Observable<JsonPipelineOutput> assertingSource = Observable.create(subscriber -> {

      dataSource.subscribe(new Observer<JsonPipelineOutput>() {

        private boolean assertionFailed;

        @Override
        public void onNext(JsonPipelineOutput responseModel) {

          JsonNode responseNode = responseModel.getPayload();

          // if this #assertExist is chained after an #extract call, the responseNode can be null and we should bail out early
          if (responseNode == null) {
            this.onError(ex);
            return;
          }

          try {
            ArrayNode jsonPathResult = new JsonPathSelector(jsonPath).call(responseNode);

            if (jsonPathResult.size() == 0) {
              // the JSONpath was valid, but it just didn't match to any result
              this.onError(ex);
            }
            else {
              // the responseNode has content at the given JsonPath, so we can continue processing processing the response
              subscriber.onNext(responseModel);
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

    Observable<JsonPipelineOutput> extractedModel = Observable.create(subscriber -> {

      dataSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput t) {
          ArrayNode result = new JsonPathSelector(jsonPath).call(t.getPayload());

          JsonNode extractedPayload = result.size() == 0 ? null : result.get(0);

          if (isNotBlank(targetProperty)) {
            extractedPayload = JacksonFunctions.wrapInObject(targetProperty, extractedPayload);
          }

          subscriber.onNext(t.withPayload(extractedPayload));
        }

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          subscriber.onError(e);
        }

      });
    });

    String targetSuffix = isNotBlank(targetProperty) ? " INTO " + targetProperty : "";
    String transformationDesc = "EXTRACT(" + jsonPath + targetSuffix + ")";
    return cloneWith(extractedModel, transformationDesc);
  }


  @Override
  public JsonPipeline collect(String jsonPath, String targetProperty) {

    Observable<JsonPipelineOutput> extractedModel = Observable.create(subscriber -> {

      dataSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput t) {
          JsonNode extractedPayload = new JsonPathSelector(jsonPath).call(t.getPayload());

          if (isNotBlank(targetProperty)) {
            extractedPayload = JacksonFunctions.wrapInObject(targetProperty, extractedPayload);
          }

          subscriber.onNext(t.withPayload(extractedPayload));
        }

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          subscriber.onError(e);
        }
      });
    });

    String targetSuffix = isNotBlank(targetProperty) ? " INTO " + targetProperty : "";
    String transformationDesc = "COLLECT(" + jsonPath + targetSuffix + ")";
    return cloneWith(extractedModel, transformationDesc);
  }


  @Override
  public JsonPipeline merge(JsonPipeline secondarySource, String targetProperty) {

    Observable<JsonPipelineOutput> zippedSource = dataSource.zipWith(secondarySource.getOutput(), (primaryModel, secondaryModel) -> {

      log.debug("zipping object from secondary source into target property " + targetProperty);

      JsonNode jsonFromPrimary = primaryModel.getPayload();
      JsonNode jsonFromSecondary = secondaryModel.getPayload();

      if (!(jsonFromPrimary.isObject())) {
        throw new JsonPipelineOutputException("Only pipelines with JSON *Objects* can be used as a target for a merge operation, but response data for "
            + this.getDescriptor() + " contained " + jsonFromPrimary.getClass().getSimpleName());
      }

      // start with cloning the the response of the primary pipeline
      ObjectNode mergedObject = jsonFromPrimary.deepCopy();

      // if a target property is specified, the JSON to be merged is inserted into this property
      if (isNotBlank(targetProperty)) {

        if (!mergedObject.has(targetProperty)) {
          // the target property does not exist yet, so we just can set the property
          mergedObject.set(targetProperty, jsonFromSecondary);
        }
        else {

          // the target property already exists - let's hope we can merge!
          JsonNode targetNode = mergedObject.get(targetProperty);

          if (!targetNode.isObject()) {
            throw new JsonPipelineOutputException("When merging two pipelines into the same target property, both most contain JSON *Object* responses");
          }

          if (!(jsonFromSecondary.isObject())) {
            throw new JsonPipelineOutputException("Only pipelines with JSON *Object* responses can be merged into an existing target property");
          }

          mergeAllPropertiesInto((ObjectNode)jsonFromSecondary, (ObjectNode)targetNode);
        }
      }
      else {

        // if no target property is specified, all properties of the secondary pipeline are copied into the merged object
        if (!(jsonFromSecondary.isObject())) {
          throw new JsonPipelineOutputException("Only pipelines with JSON *Object* responses can be merged without specify a target property");
        }

        mergeAllPropertiesInto((ObjectNode)jsonFromSecondary, mergedObject);
      }

      return primaryModel.withPayload(mergedObject).withMaxAge(Math.min(primaryModel.getMaxAge(), secondaryModel.getMaxAge()));
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

      if (targetNode.has(propertyName)) {
        // what to do if the property already exists? for now, just throw an exception,
        throw new JsonPipelineOutputException("Target pipeline " + this.getDescriptor() + " already has a property named " + propertyName);
      }

      targetNode.set(propertyName, nextProperty.getValue());
    }
  }

  @Override
  public JsonPipeline applyTransformation(String transformationId, Func1<JsonNode, JsonNode> mapping) {

    Observable<JsonPipelineOutput> transformedOutput = dataSource.map(output -> {
      JsonNode newPayload = mapping.call(output.getPayload());
      return output.withPayload(newPayload);
    });

    String transformationDesc = "TRANSFORM(" + transformationId + ")";
    return cloneWith(transformedOutput, transformationDesc);
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
    Observable<JsonPipelineOutput> cachedSource = Observable.create((subscriber) -> {

      // construct a unique cache key from the pipeline's descriptor
      final String cacheKey = caching.getCacheKey(getSourceServicePrefix(), descriptor);

      // try to asynchronously(!) fetch the response from the cache (or simulate a cache miss if the headers suggest to ignore cache)
      boolean resetExpiry = strategy.isResetExpiryOnGet(request);
      int expirySeconds = strategy.getExpirySeconds(request);

      Observable<String> cachedJsonString = (ignoreCache ? Observable.empty() : caching.get(cacheKey, resetExpiry, expirySeconds));

      // CacheResponseObserver will decide what to do when the response is ready (or could not be retrieved from cache)
      cachedJsonString.subscribe(new CacheResponseObserver(cacheKey, strategy, subscriber));
    });

    return cloneWith(cachedSource, null);
  }

  @Override
  public JsonPipeline handleException(JsonPipelineExceptionHandler handler) {

    // the code within the lambda passed to Observable#create will be executed when subscribe is called on the "wrappedSource" observable
    Observable<JsonPipelineOutput> wrappedSource = Observable.create((subscriber) -> {

      dataSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput t) {
          subscriber.onNext(t);
        }

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          int statusCode = 500;

          // extract the HTTP status code from the exceptions known to contain such information

          if (e instanceof JsonPipelineInputException) {
            statusCode = ((JsonPipelineInputException)e).getStatusCode();
          }

          if (e instanceof IllegalResponseRuntimeException) {
            statusCode = ((IllegalResponseRuntimeException)e).getResponseStatusCode();
          }

          if (e instanceof RuntimeException) {
            try {
              Observable<JsonNode> fallbackResponse = handler.rethrowOrReturnFallback(statusCode, (RuntimeException)e);

              // use the given fallback response
              fallbackResponse.subscribe(new Observer<JsonNode>() {

                @Override
                public void onNext(JsonNode t) {

                  // TODO: let the handler decide the max-age of the fallback content!
                  subscriber.onNext(new JsonPipelineOutputImpl(t).withMaxAge(55));
                }

                @Override
                public void onCompleted() {
                  subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable ex) {
                  subscriber.onError(ex);
                }
              });

            }
            catch (Throwable rethrown) {
              subscriber.onError(rethrown);
            }
          }
          else {
            subscriber.onError(e);
          }
        }
      });

    });

    return cloneWith(wrappedSource, null);
  }

  @Override
  public JsonPipeline handleNotFound(Func1<JsonPipelineOutput, JsonPipelineOutput> fallbackContent) {

    Observable<JsonPipelineOutput> fallbackSource = dataSource.map(output -> {

      if (output.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return fallbackContent.call(output);
      }

      return output;
    });

    return cloneWith(fallbackSource, null);
  }

  @Override
  public Observable<JsonPipelineOutput> getOutput() {
    return dataSource.map(o -> o);
  }

  @Override
  public Observable<JsonNode> getJsonOutput() {
    return dataSource.map(model -> model.getPayload());
  }

  @Override
  public Observable<String> getStringOutput() {
    return getJsonOutput().map(JacksonFunctions::nodeToString);
  }

  @Override
  public <T> Observable<T> getTypedOutput(Class<T> clazz) {
    return getJsonOutput().map(JacksonFunctions.nodeToPojo(clazz));
  }

  private String getSourceServicePrefix() {
    return StringUtils.join(sourceServiceNames, '+');
  }


  /**
   * an observer that is subscribed to the {@link Observable} returned by {@link CacheAdapter#get(String, boolean, int)}
   * , and is responsible for
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


    private final String cacheKey;
    private final CacheStrategy strategy;
    private final Subscriber<? super JsonPipelineOutput> subscriber;

    private boolean cacheHit;

    private CacheResponseObserver(String cacheKey, CacheStrategy strategy, Subscriber<? super JsonPipelineOutput> subscriberToForwardTo) {
      this.cacheKey = cacheKey;
      this.strategy = strategy;
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

      JsonNode contentFromCache = envelopeFromCache.get(CACHE_CONTENT_PROPERTY);

      cacheHit = true;

      String generatedDate = envelopeFromCache.get(CACHE_METADATA_PROPERTY).get("generated").asText();
      int cacheHitAge = CacheDateUtils.getSecondsSince(generatedDate);
      int staleSeconds = strategy.getStaleSeconds(request);

      if (cacheHitAge < staleSeconds) {

        // the content from cache is fresh enough to serve it - just make sure to set the max-age content-header just
        // to the time the cached content will become stale
        int maxAge = staleSeconds - cacheHitAge;

        subscriber.onNext(new JsonPipelineOutputImpl(contentFromCache).withMaxAge(maxAge));
        subscriber.onCompleted();
      }
      else {
        // this means the cached content is outdated - we better fetch the data from the backend
        log.info("Cached content for " + this.cacheKey + " is available, but it's " + cacheHitAge + " seconds old and considered stale.");

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
            log.warn("Using stale content from cache as a fallback after failing to fresh content for " + cacheKey, e);

            subscriber.onNext(new JsonPipelineOutputImpl(contentFromCache).withMaxAge(staleSeconds));
            subscriber.onCompleted();
          }
        });
      }
    }

    @Override
    public void onCompleted() {
      if (!cacheHit) {
        // there was no emission, so the response has to be fetched from the service
        log.debug("CACHE MISS for " + this.cacheKey + " fetching response from " + getSourceServicePrefix() + " through pipeline...");
        fetchAndStore(subscriber);
      }
    }

    @Override
    public void onError(Throwable e) {
      // also fall back to the actual service if the couchbase request failed
      log.warn("Failed to connect to couchbase server, falling back to direct connection to " + getSourceServicePrefix(), e);
      fetchAndStore(subscriber);
    }

    private void fetchAndStore(Subscriber<? super JsonPipelineOutput> backendResponseSubscriber) {

      // fetch the output with a new subscription, which will also store the response in the cache when it is retrieved
      dataSource.subscribe(new Observer<JsonPipelineOutput>() {

        @Override
        public void onNext(JsonPipelineOutput fetchedModel) {
          log.debug("response for " + descriptor + " has been fetched and will be put in the cache");

          int expirySeconds = strategy.getExpirySeconds(request);
          int staleSeconds = Math.max(strategy.getStaleSeconds(request), fetchedModel.getMaxAge());

          ObjectNode wrappedNode = wrapInEnvelope(fetchedModel.getPayload());
          caching.put(cacheKey, JacksonFunctions.nodeToString(wrappedNode), expirySeconds);

          // everything else is just forwarding to the subscriber to the cachedSource
          backendResponseSubscriber.onNext(fetchedModel.withMaxAge(staleSeconds));
        }

        @Override
        public void onCompleted() {
          backendResponseSubscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          backendResponseSubscriber.onError(e);
        }

        private ObjectNode wrapInEnvelope(JsonNode fetchedNode) {

          ObjectNode envelope = nodeFactory.objectNode();

          ObjectNode metadata = envelope.putObject(CACHE_METADATA_PROPERTY);

          metadata.put("cacheKey", cacheKey);
          metadata.set("sources", JacksonFunctions.pojoToNode(getSourceServices()));
          metadata.put("pipeline", descriptor);
          metadata.put("generated", CacheDateUtils.formatCurrentTime());
          metadata.put("expiry", strategy.getExpirySeconds(request));
          metadata.put("resetExpiryOnGet", strategy.isResetExpiryOnGet(request));

          envelope.set(CACHE_CONTENT_PROPERTY, fetchedNode);

          return envelope;
        }
      });
    }
  }


}
