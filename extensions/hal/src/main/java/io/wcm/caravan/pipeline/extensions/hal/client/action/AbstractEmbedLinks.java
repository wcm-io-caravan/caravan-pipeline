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

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheControlUtils;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base link embedding action to load links for a given relation and store them as embedded resources.
 */
@ConsumerType
public abstract class AbstractEmbedLinks extends AbstractHalClientAction {

  private final String serviceId;
  private final String relation;
  private final Map<String, Object> parameters;

  /**
   * @param serviceId Service ID
   * @param relation Link relation to embed
   * @param parameters URI parameters
   */
  public AbstractEmbedLinks(String serviceId, String relation, Map<String, Object> parameters) {
    this.serviceId = serviceId;
    this.relation = relation;
    this.parameters = parameters;
  }

  @Override
  public String getId() {
    return relation + '-' + parameters.hashCode();
  }

  @Override
  public final Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = getLinksForRequestedRelation(halResource);
    if (links.isEmpty()) {
      return Observable.just(previousStepOutput);
    }

    Observable<JsonPipeline> linkPipelines = getPipelinesForLinks(previousStepOutput, context, links);
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

  private Observable<JsonPipeline> getPipelinesForLinks(JsonPipelineOutput previousStepOutput, JsonPipelineContext context, List<Link> links) {

    JsonPipeline pipeline = context.getFactory().createEmpty(context.getProperties());
    return Observable.from(links)
        .map(link -> {
          return new LoadLink(serviceId, link, parameters)
          .setCacheStrategy(getCacheStrategy())
          .setExceptionHandlers(getExceptionHandlers())
          .setLogger(getLogger());
        })
        .map(action -> pipeline.applyAction(action));

  }

  /**
   * @return Returns the relation.
   */
  protected final String getRelation() {
    return this.relation;
  }

}
