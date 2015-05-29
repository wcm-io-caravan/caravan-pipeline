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
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheControlUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Action to load a HAL link and replace the current resource by the loaded one.
 */
@ProviderType
public final class FollowLink implements JsonPipelineAction {

  private final String serviceName;
  private final String relation;
  private final Map<String, Object> parameters;
  private final int index;

  private CacheStrategy cacheStrategy;

  /**
   * @param serviceName Logical name of the service
   * @param relation Link relation to embed
   * @param index Index of the link to embed
   * @param parameters URI parameters
   */
  public FollowLink(String serviceName, String relation, int index, Map<String, Object> parameters) {
    this.serviceName = serviceName;
    this.relation = relation;
    this.index = index;
    this.parameters = parameters;
  }

  /**
   * @param newCacheStrategy Caching strategy
   * @return Follow link action
   */
  public FollowLink setCacheStrategy(CacheStrategy newCacheStrategy) {
    this.cacheStrategy = newCacheStrategy;
    return this;
  }

  @Override
  public String getId() {
    return "FOLLOW-LINK(" + relation + '-' + parameters.hashCode() + '-' + index + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
    CaravanHttpRequest request = getRequest(previousStepOutput);
    JsonPipeline pipeline = createPipeline(context, request);
    return pipeline.getOutput().map(jsonPipelineOutput ->
        jsonPipelineOutput.withMaxAge(CacheControlUtils.getLowestMaxAge(jsonPipelineOutput, previousStepOutput)));
  }

  private CaravanHttpRequest getRequest(JsonPipelineOutput previousStepOutput) {

    String href = getHref(previousStepOutput);
    CaravanHttpRequestBuilder builder = new CaravanHttpRequestBuilder(serviceName).append(href);
    builder = setCacheControlHeaderIfExists(builder, previousStepOutput);
    builder = setCorrelationIdIfExists(builder, previousStepOutput);
    return builder.build(parameters);

  }

  private String getHref(JsonPipelineOutput previousStepOutput) {
    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = halResource.getLinks(relation);
    if (links.size() <= index) {
      throw new IllegalStateException("HAL resource has no link with relation " + relation + " and index " + index);
    }
    return links.get(index).getHref();
  }

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

  private CaravanHttpRequestBuilder setCorrelationIdIfExists(CaravanHttpRequestBuilder builder, JsonPipelineOutput previousStepOutput) {

    if (previousStepOutput.getCorrelationId() == null) {
      return builder;
    }
    return builder.header(CORRELATION_ID_HEADER_NAME, previousStepOutput.getCorrelationId());

  }

  private JsonPipeline createPipeline(JsonPipelineContext context, CaravanHttpRequest request) {
    JsonPipeline pipeline = context.getFactory().create(request, context.getProperties());
    if (cacheStrategy != null) {
      pipeline = pipeline.addCachePoint(cacheStrategy);
    }
    return pipeline;
  }

}
