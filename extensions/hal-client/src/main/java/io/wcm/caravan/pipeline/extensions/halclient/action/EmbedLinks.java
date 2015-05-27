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

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Action to load one or all links of the main HAL resource and insert the content as embedded resource(s).
 */
@ProviderType
public final class EmbedLinks extends AbstractEmbedLinks {

  /**
   * @param serviceName Logical name of the service
   * @param relation Link relation to embed
   * @param parameters URI parameters
   */
  public EmbedLinks(String serviceName, String relation, Map<String, Object> parameters) {
    super(serviceName, relation, parameters);
  }

  @Override
  public String getId() {
    return "EMBED-LINKS(" + getRelation() + '-' + getParameters().hashCode() + ")";
  }

  @Override
  List<Link> getLinksForRequestedRelation(HalResource halResource) {
    return halResource.getLinks(getRelation());
  }

  @Override
  void setEmbeddedResourcesAndRemoveLink(HalResource halResource, List<Link> links, List<HalResource> resourcesToEmbed) {
    halResource.addEmbedded(getRelation(), resourcesToEmbed);
    halResource.removeLinks(getRelation());
  }

  /**
   * Sets the exception handler for this action.
   * @param newExceptionHandler The exceptionHandler to set.
   * @return Embed Links action
   */
  public EmbedLinks setExceptionHandler(JsonPipelineExceptionHandler newExceptionHandler) {
    super.setExceptionHandlerInternal(newExceptionHandler);
    return this;
  }

  /**
   * Sets the cache strategy for this action.
   * @param newCacheStrategy Caching strategy
   * @return Embed Links action
   */
  public EmbedLinks setCacheStrategy(CacheStrategy newCacheStrategy) {
    super.setCacheStrategyInternal(newCacheStrategy);
    return this;
  }

}
