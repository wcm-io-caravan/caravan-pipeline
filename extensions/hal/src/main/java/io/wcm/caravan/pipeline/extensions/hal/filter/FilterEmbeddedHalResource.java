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
package io.wcm.caravan.pipeline.extensions.hal.filter;

import io.wcm.caravan.hal.commons.resource.HalResource;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Walks recursive through the HAL resource and its embedded resources. Given {@code matcher} determines which embedded HAL resource should get filtered by the
 * given {@code predicate}.
 */
@ProviderType
public final class FilterEmbeddedHalResource implements JsonPipelineAction {

  private final HalResourcePredicate matcher;
  private final HalResourcePredicate predicate;

  /**
   * @param matcher HAL resource matcher
   * @param predicate HAL resource predicate
   */
  public FilterEmbeddedHalResource(HalResourcePredicate matcher, HalResourcePredicate predicate) {
    this.matcher = matcher;
    this.predicate = predicate;
  }

  @Override
  public String getId() {
    return "FILTER-EMBEDDED-HAL-RESOURCE(" + matcher.getId() + "-" + predicate.getId() + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    HalResource hal = new HalResource((ObjectNode)previousStepOutput.getPayload());
    process(new HalPath(), hal);
    return Observable.just(previousStepOutput);

  }

  private void process(HalPath halPath, HalResource hal) {

    for (String relation : hal.getEmbedded().keySet()) {
      HalPath newHalPath = halPath.add(relation);
      filterEmbeddedResources(newHalPath, hal);
      processEmbeddedResources(newHalPath, hal);
    }

  }

  private void filterEmbeddedResources(HalPath halPath, HalResource hal) {

    int index = 0;
    String relation = halPath.current();
    for (HalResource resource : hal.getEmbedded(relation)) {
      if (matcher.apply(halPath, resource) && !predicate.apply(halPath, resource)) {
        hal.removeEmbedded(relation, index);
      }
      else {
        index++;
      }
    }

  }

  private void processEmbeddedResources(HalPath halPath, HalResource hal) {

    Streams.of(hal.getEmbedded(halPath.current()))
    .forEach(resource -> process(halPath, resource));

  }

}
