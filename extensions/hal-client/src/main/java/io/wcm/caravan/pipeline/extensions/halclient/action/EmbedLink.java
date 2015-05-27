package io.wcm.caravan.pipeline.extensions.halclient.action;

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

/**
 * Embeds only one link for the given relation and index as embedded resource.
 */
public class EmbedLink extends AbstractEmbedLinks {

  private final int index;

  /**
   * @param serviceName Logical name of the service
   * @param relation Link relation to embed
   * @param index Index of the link to embed
   * @param parameters URI parameters
   */
  public EmbedLink(String serviceName, String relation, int index, Map<String, Object> parameters) {
    super(serviceName, relation, parameters);
    this.index = index;
  }

  @Override
  public String getId() {
    return "EMBED-LINKS(" + getRelation() + '-' + getParameters().hashCode() + "-" + index + ")";
  }

  @Override
  List<Link> getLinksForRequestedRelation(HalResource halResource) {
    List<Link> links = halResource.getLinks(getRelation());
    return links.size() > index ? Lists.newArrayList(links.get(index)) : Collections.emptyList();
  }

  @Override
  void setEmbeddedResourcesAndRemoveLink(HalResource halResource, List<Link> links, List<HalResource> resourcesToEmbed) {
    halResource.addEmbedded(getRelation(), resourcesToEmbed);
    halResource.removeLink(getRelation(), index);
  }

  /**
   * Sets the exception handler for this action.
   * @param newExceptionHandler The exceptionHandler to set.
   * @return Embed Link action
   */
  public EmbedLink setExceptionHandler(JsonPipelineExceptionHandler newExceptionHandler) {
    super.setExceptionHandlerInternal(newExceptionHandler);
    return this;
  }

  /**
   * Sets the cache strategy for this action.
   * @param newCacheStrategy Caching strategy
   * @return Embed Link action
   */
  public EmbedLink setCacheStrategy(CacheStrategy newCacheStrategy) {
    super.setCacheStrategyInternal(newCacheStrategy);
    return this;
  }

}
