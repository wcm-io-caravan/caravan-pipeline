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
package io.wcm.caravan.pipeline.extensions.hal.action;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import rx.Observable;

/**
 * Embeds the items of an embedded HAL collection resource.
 */
@ProviderType
public final class InlineEmbeddedCollection implements JsonPipelineAction {

  private final String[] relations;

  /**
   * @param relations Relations of the embedded resources to embed
   */
  public InlineEmbeddedCollection(String... relations) {
    this.relations = relations;
  }

  @Override
  public String getId() {
    return "INLINE-EMBEDDED-COLLECTION(" + StringUtils.join(relations, '-') + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
    HalResource halResource = new HalResource(previousStepOutput.getPayload());
    for (String relation : relations) {
      moveEmbeddedCollection(halResource, relation);
      // delete embedded resource
      halResource.removeEmbedded(relation);
    }
    // delete self-HREF for resource
    halResource.removeLinks("self");

    return Observable.just(previousStepOutput);
  }

  private void moveEmbeddedCollection(HalResource halResource, String relation) {
    List<HalResource> embeddedResources = halResource.getEmbedded(relation);
    ObjectNode model = halResource.getModel();
    ArrayNode container = model.putArray(relation);
    // iterate on relation specific embedded resources
    embeddedResources.stream()
        // get items
        .flatMap(e -> e.getEmbedded("item").stream())
        // get state
        .map(item -> item.removeEmbedded().removeLinks().getModel())
        // add to array
        .forEach(itemState -> container.add(itemState));
  }

}
