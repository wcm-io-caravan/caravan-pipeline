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

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Action to load all links for a given relation in a HAL document and store them as embedded resources. In opposite to {@link EmbedLinks} this action takes the
 * links of the main and all embedded resources.
 */
@ProviderType
public final class DeepEmbedLinks extends AbstractEmbedLinks {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeepEmbedLinks.class);

  /**
   * @param serviceId Service ID
   * @param relation Link relation to embed
   * @param parameters URI parameters
   */
  public DeepEmbedLinks(String serviceId, String relation, Map<String, Object> parameters) {
    super(serviceId, relation, parameters);
  }

  @Override
  public String getId() {
    return "DEEP-EMBED(" + super.getId() + ")";
  }

  @Override
  List<Link> getLinksForRequestedRelation(HalResource halResource) {

    List<Link> links = Lists.newArrayList(halResource.getLinks(getRelation()));
    Streams.of(halResource.getEmbedded().values())
    .map(embedded -> getLinksForRequestedRelation(embedded))
    .forEach(embeddedLinks -> links.addAll(embeddedLinks));
    return links;

  }

  @Override
  void setEmbeddedResourcesAndRemoveLink(HalResource halResource, List<Link> links, List<HalResource> resourcesToEmbed) {
    Map<String, HalResource> index = createIndex(links, resourcesToEmbed);
    recursiveLinkReplacement(halResource, index);
  }

  private Map<String, HalResource> createIndex(List<Link> links, List<HalResource> resourcesToEmbed) {

    Map<String, HalResource> index = Maps.newHashMap();
    for (int i = 0; i < links.size(); i++) {
      index.put(links.get(i).getHref(), resourcesToEmbed.get(i));
    }
    return index;

  }


  private void recursiveLinkReplacement(HalResource halResource, Map<String, HalResource> index) {

    for (Link link : halResource.getLinks(getRelation())) {
      HalResource resourceToEmbed = index.get(link.getHref());
      if (resourceToEmbed != null) {
        halResource.addEmbedded(getRelation(), resourceToEmbed);
        halResource.removeLinkWithHref(getRelation(), link.getHref());
      }
      else {
        LOGGER.debug("Did not find resource for href " + link.getHref());
      }
    }

    for (HalResource embedded : halResource.getEmbedded().values()) {
      recursiveLinkReplacement(embedded, index);
    }

  }

}
