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
import static org.apache.commons.lang3.Validate.isTrue;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.operators.AssertExistsOperator;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer;
import io.wcm.caravan.pipeline.impl.operators.CollectOperator;
import io.wcm.caravan.pipeline.impl.operators.ExtractOperator;
import io.wcm.caravan.pipeline.impl.operators.HandleExceptionOperator;
import io.wcm.caravan.pipeline.impl.operators.MergeTransformer;
import io.wcm.caravan.pipeline.impl.operators.ResponseHandlingOperator;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;
import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Default implementation of {@link JsonPipeline}.
 */
public final class JsonPipelineImpl implements JsonPipeline {

  private SortedSet<String> sourceServiceNames = new TreeSet<String>();
  private List<CaravanHttpRequest> requests = new LinkedList<CaravanHttpRequest>();

  private CacheAdapter caching;
  private String descriptor;

  private Observable<JsonPipelineOutput> observable;

  /**
   * @param serviceName the logical service name. Will be used as a namespace for cache keys
   * @param request the REST request that provides the soruce data
   * @param responseObservable the response observable obtained by the {@link CaravanHttpClient}
   * @param caching the caching layer to use
   */
  JsonPipelineImpl(String serviceName, CaravanHttpRequest request, Observable<CaravanHttpResponse> responseObservable, CacheAdapter caching) {

    if (isNotBlank(serviceName)) {
      this.sourceServiceNames.add(serviceName);
    }
    this.requests.add(request);

    this.caching = caching;
    this.descriptor = isNotBlank(request.url()) ? "GET(//" + serviceName + request.url() + ")" : "EMPTY()";

    this.observable = responseObservable.lift(new ResponseHandlingOperator(request.url()));
  }

  private JsonPipelineImpl() {
    // only used internally
  }

  private JsonPipelineImpl cloneWith(Observable<JsonPipelineOutput> newObservable, String descriptorSuffix) {
    JsonPipelineImpl clone = new JsonPipelineImpl();
    clone.sourceServiceNames.addAll(this.sourceServiceNames);
    clone.requests.addAll(this.requests);

    clone.caching = this.caching;
    clone.descriptor = this.descriptor;
    if (StringUtils.isNotBlank(descriptorSuffix)) {
      clone.descriptor += "+" + descriptorSuffix;
    }

    clone.observable = newObservable;
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
  public List<CaravanHttpRequest> getRequests() {
    return this.requests;
  }

  @Override
  public JsonPipeline assertExists(String jsonPath, int statusCode, String msg) {

    Observable<JsonPipelineOutput> assertingObservable = observable.lift(new AssertExistsOperator(jsonPath, statusCode, msg));

    return cloneWith(assertingObservable, null);
  }

  @Override
  public JsonPipeline extract(String jsonPath) {

    Observable<JsonPipelineOutput> extractingObservable = observable.lift(new ExtractOperator(jsonPath, null));
    String transformationDesc = "EXTRACT(" + jsonPath + ")";

    return cloneWith(extractingObservable, transformationDesc);
  }

  @Override
  public JsonPipeline extract(String jsonPath, String targetProperty) {

    isTrue(isNotBlank(targetProperty), "Target property is '" + targetProperty
        + "'. Please provide meaningfull targetProperty or use another extract method wothout targetProperty parameter, if any targetProperty isn't required.");

    Observable<JsonPipelineOutput> extractingObservable = observable.lift(new ExtractOperator(jsonPath, targetProperty));
    String transformationDesc = "EXTRACT(" + jsonPath + " INTO " + targetProperty + ")";

    return cloneWith(extractingObservable, transformationDesc);
  }

  @Override
  public JsonPipeline collect(String jsonPath) {

    Observable<JsonPipelineOutput> collectingObservable = observable.lift(new CollectOperator(jsonPath, null));
    String transformationDesc = "COLLECT(" + jsonPath + ")";

    return cloneWith(collectingObservable, transformationDesc);
  }

  @Override
  public JsonPipeline collect(String jsonPath, String targetProperty) {

    isTrue(isNotBlank(targetProperty), "Target property is '" + targetProperty
        + "'. Please provide meaningfull targetProperty or use another collect method wothout targetProperty parameter, if any targetProperty isn't required.");

    Observable<JsonPipelineOutput> collectingObservable = observable.lift(new CollectOperator(jsonPath, targetProperty));
    String transformationDesc = "COLLECT(" + jsonPath + " INTO " + targetProperty + ")";

    return cloneWith(collectingObservable, transformationDesc);
  }

  @Override
  public JsonPipeline merge(JsonPipeline secondarySource) {

    MergeTransformer transformer = new MergeTransformer(descriptor, secondarySource.getOutput(), null);
    Observable<JsonPipelineOutput> mergedObservable = observable.compose(transformer);
    String transformationDesc = "MERGE(" + secondarySource.getDescriptor() + ")";

    JsonPipelineImpl mergedPipeline = cloneWith(mergedObservable, transformationDesc);
    mergedPipeline.sourceServiceNames.addAll(secondarySource.getSourceServices());
    mergedPipeline.requests.addAll(secondarySource.getRequests());

    return mergedPipeline;
  }

  @Override
  public JsonPipeline merge(JsonPipeline secondarySource, String targetProperty) {

    isTrue(isNotBlank(targetProperty), "Target property is '" + targetProperty
        + "'. Please provide meaningfull targetProperty or use another merge method wothout targetProperty parameter, if any targetProperty isn't required.");

    MergeTransformer transformer = new MergeTransformer(descriptor, secondarySource.getOutput(), targetProperty);
    Observable<JsonPipelineOutput> mergedObservable = observable.compose(transformer);
    String transformationDesc = "MERGE(" + secondarySource.getDescriptor() + " INTO " + targetProperty + ")";

    JsonPipelineImpl mergedPipeline = cloneWith(mergedObservable, transformationDesc);
    mergedPipeline.sourceServiceNames.addAll(secondarySource.getSourceServices());
    mergedPipeline.requests.addAll(secondarySource.getRequests());

    return mergedPipeline;
  }

  @Override
  public JsonPipeline applyAction(JsonPipelineAction action) {
    String actionDesc = "ACTION(" + action.getId() + ")";

    Observable<JsonPipelineOutput> transformedObservable = observable.flatMap(output -> action.execute(output));

    return cloneWith(transformedObservable, actionDesc);
  }

  @Override
  public JsonPipeline applyTransformation(String transformationId, Func1<JsonNode, JsonNode> mapping) {

    Observable<JsonPipelineOutput> transformedObservable = observable.map(output -> {
      JsonNode newPayload = mapping.call(output.getPayload());
      return output.withPayload(newPayload);
    });

    String transformationDesc = "TRANSFORM(" + transformationId + ")";

    return cloneWith(transformedObservable, transformationDesc);
  }

  @Override
  public JsonPipeline followedBy(String transformationId, Func1<JsonPipelineOutput, JsonPipeline> pipelineFactoryMethod) {

    Observable<JsonPipelineOutput> followUpObservable = observable.flatMap(output -> {
      JsonPipeline followUpPipeline = pipelineFactoryMethod.call(output);
      return followUpPipeline.getOutput();
    });

    String transformationDesc = "FOLLOWEDBY(" + transformationId + ")";

    return cloneWith(followUpObservable, transformationDesc);
  }

  @Override
  public JsonPipeline addCachePoint(CacheStrategy strategy) {

    // skip all caching logic if the expiry time or refresh interval for this request is 0
    if (strategy.getStorageTime(requests) == 0 || strategy.getRefreshInterval(requests) == 0) {
      return this;
    }

    CachePointTransformer transformer = new CachePointTransformer(caching, requests, descriptor, sourceServiceNames, strategy);
    Observable<JsonPipelineOutput> cachingObservable = observable.compose(transformer);

    return cloneWith(cachingObservable, null);
  }

  @Override
  public JsonPipeline handleException(JsonPipelineExceptionHandler handler) {

    Observable<JsonPipelineOutput> exceptionHandlingObservable = observable.lift(new HandleExceptionOperator(handler));

    return cloneWith(exceptionHandlingObservable, null);
  }

  @Override
  public Observable<JsonPipelineOutput> getOutput() {
    return observable.map(o -> o);
  }

  @Override
  public Observable<JsonNode> getJsonOutput() {
    return observable.map(model -> model.getPayload());
  }

  @Override
  public Observable<String> getStringOutput() {
    return getJsonOutput().map(JacksonFunctions::nodeToString);
  }

}
