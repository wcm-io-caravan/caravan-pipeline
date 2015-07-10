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

import io.wcm.caravan.hal.commons.resource.HalResource;
import io.wcm.caravan.hal.commons.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Action to load a HAL link and replace the current resource by the loaded one.
 */
@ProviderType
public final class FollowLink extends AbstractHalClientAction {

  private final String serviceId;
  private final String relation;
  private final Map<String, Object> parameters;
  private final int index;

  /**
   * @param serviceId Service ID
   * @param relation Link relation to embed
   * @param index Index of the link to embed
   * @param parameters URI parameters
   */
  public FollowLink(String serviceId, String relation, int index, Map<String, Object> parameters) {
    this.serviceId = serviceId;
    this.relation = relation;
    this.index = index;
    this.parameters = parameters;
  }

  @Override
  public String getId() {
    return "FOLLOW-LINK(" + relation + '-' + parameters.hashCode() + '-' + index + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    Link link = getLink(previousStepOutput);
    return new LoadLink(serviceId, link, parameters)
    .setCacheStrategy(getCacheStrategy())
    .setExceptionHandlers(getExceptionHandlers())
    .setLogger(getLogger())
    .execute(previousStepOutput, context);
  }

  private Link getLink(JsonPipelineOutput previousStepOutput) {

    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    List<Link> links = halResource.getLinks(relation);
    if (links.size() <= index) {
      throw new IllegalStateException("HAL resource has no link with relation " + relation + " and index " + index);
    }
    return links.get(index);

  }

}
