/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2015 wcm.io
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.Lists;

/**
 * Embeds only one link for the given relation and index as embedded resource.
 */
@ProviderType
public final class EmbedLink extends AbstractEmbedLinks {

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
    return "EMBED-LINK(" + super.getId() + "-" + index + ")";
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

}
