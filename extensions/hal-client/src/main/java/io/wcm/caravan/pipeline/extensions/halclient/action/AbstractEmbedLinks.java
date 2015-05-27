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
package io.wcm.caravan.pipeline.extensions.halclient.action;

import static io.wcm.caravan.io.http.request.CaravanHttpRequest.CORRELATION_ID_HEADER_NAME;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheControlUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base link embedding action to load links for a given relation and store them as embedded resources.
 */
public abstract class AbstractEmbedLinks implements JsonPipelineAction {

  private final String serviceName;
  private final String relation;
  private final Map<String, Object> parameters;

  private CacheStrategy cacheStrategy;
  private JsonPipelineExceptionHandler exceptionHandler;

  /**
   * @param serviceName Logical name of the service
   * @param relation Link relation to embed
   * @param parameters URI parameters
   */
  public AbstractEmbedLinks(String serviceName, String relation, Map<String, Object> parameters) {
    this.serviceName = serviceName;
    this.relation = relation;
    this.parameters = parameters;
  }

  /**
   * Sets the cache strategy for this action.
   * @param newCacheStrategy Caching strategy
   */
  void setCacheStrategyInternal(CacheStrategy newCacheStrategy) {
    this.cacheStrategy = newCacheStrategy;
  }

  /**
   * Sets the exception handler for this action.
   * @param newExceptionHandler The exceptionHandler to set.
   */
  void setExceptionHandlerInternal(JsonPipelineExceptionHandler newExceptionHandler) {
    this.exceptionHandler = newExceptionHandler;
  }


  /**
   * @return Returns the serviceName.
   */
  public String getServiceName() {
    return this.serviceName;
  }


  /**
   * @return Returns the relation.
   */
  public String getRelation() {
    return this.relation;
  }


  /**
   * @return Returns the parameters.
   */
  public Map<String, Object> getParameters() {
    return this.parameters;
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = getLinksForRequestedRelation(halResource);
    if (links.isEmpty()) {
      return Observable.just(previousStepOutput);
    }

    Observable<JsonPipeline> linkPipelines = getPipelinesForLinks(links, previousStepOutput, context);
    return CacheControlUtils.zipWithLowestMaxAge(linkPipelines, (outputsToEmbed) -> {

      List<HalResource> resourcesToEmbed = Streams.of(outputsToEmbed)
          .map(output -> output.getPayload())
          .map(json -> new HalResource((ObjectNode)json))
          .collect(Collectors.toList());
      setEmbeddedResourcesAndRemoveLink(halResource, links, resourcesToEmbed);
      return previousStepOutput.withPayload(halResource.getModel());

    });

  }

  abstract List<Link> getLinksForRequestedRelation(HalResource halResource);

  abstract void setEmbeddedResourcesAndRemoveLink(HalResource halResource, List<Link> links, List<HalResource> resourcesToEmbed);


  private Observable<JsonPipeline> getPipelinesForLinks(List<Link> links, JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    Collection<String> cacheControlHeader = getCacheControlHeader(previousStepOutput);
    String correlationId = previousStepOutput.getCorrelationId();

    return Observable.from(links)
        // extract URI
        .map(link -> link.getHref())
        // create request builder
        .map(href -> new CaravanHttpRequestBuilder(serviceName).append(href))
        // add cacheControlHeader if exists
        .map(requestBuilder -> cacheControlHeader == null ? requestBuilder : requestBuilder.header("Cache-Control", cacheControlHeader))
        // add correlation ID if exists
        .map(builder -> correlationId == null ? builder : builder.header(CORRELATION_ID_HEADER_NAME, correlationId))
        // build request
        .map(builder -> builder.build(parameters))
        // create pipeline
        .map(request -> context.getFactory().create(request, context.getProperties()))
        // add Caching
        .map(pipeline -> cacheStrategy == null ? pipeline : pipeline.addCachePoint(cacheStrategy))
        // add exception handler
        .map(pipeline -> exceptionHandler == null ? pipeline : pipeline.handleException(exceptionHandler));

  }

  private Collection<String> getCacheControlHeader(JsonPipelineOutput previousStepOutput) {

    List<CaravanHttpRequest> requests = previousStepOutput.getRequests();
    return requests.isEmpty() ? null : requests.get(0).getHeaders().get("Cache-Control");

  }

}
