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

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.extensions.hal.client.ServiceIdExtractor;

import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Action to load a HAL link and replace the current resource by the loaded one.
 */
@ProviderType
public final class FollowLink extends AbstractHalClientAction {

  private final ServiceIdExtractor serviceId;

  private final LinkSelectionStrategy linkSelector;

  private final Map<String, Object> parameters;

  private String httpMethod = HttpGet.METHOD_NAME;

  /**
   * @param serviceId Service ID
   * @param relation Link relation to embed
   * @param index Index of the link to embed
   * @param parameters URI parameters
   */
  public FollowLink(String serviceId, String relation, int index, Map<String, Object> parameters) {
    this.serviceId = (href) -> serviceId;
    this.linkSelector = LinkSelectionStrategies.byRelationAndIndex(relation, index);
    this.parameters = parameters;
  }

  /**
   * @param serviceId function to extract Service ID from a given request URL
   * @param relation Link relation to embed
   * @param index Index of the link to embed
   * @param parameters URI parameters
   */
  public FollowLink(ServiceIdExtractor serviceId, String relation, int index, Map<String, Object> parameters) {
    this.serviceId = serviceId;
    this.linkSelector = LinkSelectionStrategies.byRelationAndIndex(relation, index);
    this.parameters = parameters;
  }

  /**
   * @param serviceId function to extract Service ID from a given request URL
   * @param relation Link relation to embed
   * @param name of the link to embed
   * @param parameters URI parameters
   */
  public FollowLink(ServiceIdExtractor serviceId, String relation, String name, Map<String, Object> parameters) {
    this.serviceId = serviceId;
    this.linkSelector = LinkSelectionStrategies.byRelationAndName(relation, name);
    this.parameters = parameters;
  }

  @Override
  public String getId() {
    return "FOLLOW-LINK(" + httpMethod + " - " + linkSelector.getId() + '-' + parameters.hashCode() + ")";
  }

  /**
   * @param httpMethodToUse the HTTP method to use when loading the link
   * @return this
   */
  public HalClientAction withHttpMethod(String httpMethodToUse) {
    this.httpMethod = httpMethodToUse;
    return this;
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {

    HalResource halResource = new HalResource((ObjectNode)previousStepOutput.getPayload());
    Link link = linkSelector.pickLink(halResource);
    return new LoadLink(serviceId, link, parameters)
        .withHttpMethod(httpMethod)
        .setCacheStrategy(getCacheStrategy())
        .setExceptionHandlers(getExceptionHandlers())
        .setLogger(getLogger())
        .execute(previousStepOutput, context);
  }

}
