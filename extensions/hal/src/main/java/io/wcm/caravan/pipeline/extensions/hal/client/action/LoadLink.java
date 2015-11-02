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
package io.wcm.caravan.pipeline.extensions.hal.client.action;

import static io.wcm.caravan.io.http.request.CaravanHttpRequest.CORRELATION_ID_HEADER_NAME;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheControlUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.extensions.hal.client.ServiceIdExtractor;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.google.common.collect.Multimap;

/**
 * Action to load a HAL link.
 */
@ProviderType
public final class LoadLink extends AbstractHalClientAction {

  private final ServiceIdExtractor serviceId;
  private final Link link;
  private final Map<String, Object> parameters;
  private String httpMethod = HttpGet.METHOD_NAME;

  /**
   * @param serviceId Service ID
   * @param link Link to load
   * @param parameters URI parameters
   */
  public LoadLink(String serviceId, Link link, Map<String, Object> parameters) {
    this.serviceId = (path) -> serviceId;
    this.link = link;
    this.parameters = parameters;
  }

  /**
   * @param serviceId a function to extract the serviceid from a path
   * @param link Link to load
   * @param parameters URI parameters
   */
  public LoadLink(ServiceIdExtractor serviceId, Link link, Map<String, Object> parameters) {
    this.serviceId = serviceId;
    this.link = link;
    this.parameters = parameters;
  }

  @Override
  public String getId() {
    return "LOAD-LINK(" + httpMethod + "-" + serviceId.getServiceId(link.getHref()) + '-' + StringUtils.defaultIfBlank(link.getName(), "") + '-'
        + parameters.hashCode() + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    CaravanHttpRequest request = createRequest(previousStepOutput);
    JsonPipeline pipeline = createPipeline(pipelineContext, request);
    return pipeline.getOutput().map(jsonPipelineOutput -> {

      int maxAge = CacheControlUtils.getLowestMaxAge(jsonPipelineOutput, previousStepOutput);
      return jsonPipelineOutput.withMaxAge(maxAge);

    });

  }

  /**
   * @param httpMethodToUse the HTTP method to use when loading the link
   * @return this
   */
  public HalClientAction withHttpMethod(String httpMethodToUse) {
    this.httpMethod = httpMethodToUse;
    return this;
  }

  private CaravanHttpRequest createRequest(JsonPipelineOutput previousStepOutput) {

    CaravanHttpRequestBuilder builder = getRequestBuilder();
    builder = setCacheControlHeaderIfExists(builder, previousStepOutput);
    builder = setCorrelationIdIfExists(builder, previousStepOutput);
    builder = setAdditionalHttpHeadersIfExists(builder);
    builder = builder.method(httpMethod);
    return builder.build(parameters);

  }

  private JsonPipeline createPipeline(JsonPipelineContext context, CaravanHttpRequest request) {

    if (hasLogger()) {
      getLogger().debug("Execute request: " + request);
    }
    JsonPipeline pipeline = context.getFactory().create(request, context.getProperties());
    pipeline = setCacheStrategyIfExists(pipeline);
    pipeline = addExceptionHandlers(pipeline);
    return pipeline;

  }

  /**
   * @return URL of the current link and parameters.
   */
  public String getUrl() {
    return getRequestBuilder().build(parameters).getUrl();
  }

  private CaravanHttpRequestBuilder getRequestBuilder() {
    return new CaravanHttpRequestBuilder(serviceId.getServiceId(link.getHref())).append(link.getHref());
  }

  /**
   * Adds a cache point to the given JSON pipeline if provided.
   * @param pipeline JSON pipeline
   * @return JSON pipeline with or without cache point
   */
  private JsonPipeline setCacheStrategyIfExists(JsonPipeline pipeline) {
    CacheStrategy cacheStrategy = getCacheStrategy();
    return cacheStrategy == null ? pipeline : pipeline.addCachePoint(cacheStrategy);
  }

  /**
   * Sets the HTTP Cache-Control header to the given request builder if provided by the previous JSON pipeline output.
   * @param builder HTTP request builder
   * @param previousStepOutput Previous JSON output
   * @return HTTP request builder with or without HTTP Cache-Control header
   */
  private CaravanHttpRequestBuilder setCacheControlHeaderIfExists(CaravanHttpRequestBuilder builder, JsonPipelineOutput previousStepOutput) {

    if (previousStepOutput.getRequests().isEmpty()) {
      return builder;
    }
    CaravanHttpRequest previousRequest = previousStepOutput.getRequests().get(0);
    Collection<String> cacheControlHeader = previousRequest.getHeaders().get("Cache-Control");
    if (cacheControlHeader == null || cacheControlHeader.isEmpty()) {
      return builder;
    }
    return builder.header("Cache-Control", cacheControlHeader);

  }

  /**
   * Sets the {@code correlation-id} HTTP header to the given request builder if provided by the previous JSON pipeline
   * output.
   * @param builder HTTP request builder
   * @param previousStepOutput Previous JSON output
   * @return HTTP request builder with or without {@code correlation-id} HTTP header
   */
  private CaravanHttpRequestBuilder setCorrelationIdIfExists(CaravanHttpRequestBuilder builder, JsonPipelineOutput previousStepOutput) {
    return previousStepOutput.getCorrelationId() == null ? builder : builder.header(CORRELATION_ID_HEADER_NAME, previousStepOutput.getCorrelationId());
  }

  /**
   * Adds exception handler(s) to the given JSON pipeline if provided.
   * @param pipeline JSON pipeline
   * @return JSON pipeline with or without exception handler(s)
   */
  private JsonPipeline addExceptionHandlers(JsonPipeline pipeline) {

    JsonPipeline newPipeline = pipeline;
    for (JsonPipelineExceptionHandler handler : getExceptionHandlers()) {
      newPipeline = newPipeline.handleException(handler);
    }
    return newPipeline;

  }

  /**
   * Sets additional HTTP headers to the given request builder if provided.
   * @param builder HTTP request builder
   * @return HTTP request builder with or without headers
   */
  private CaravanHttpRequestBuilder setAdditionalHttpHeadersIfExists(CaravanHttpRequestBuilder builder) {
    Multimap<String, String> headers = getHttpHeaders();
    Streams.of(headers.keySet()).forEach(name -> builder.header(name, headers.get(name)));
    return builder;
  }

}
