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

import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.Sets;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import rx.Observable;

/**
 * Removes all properties for a HAL resource and its embedded resources. (use {@link #except(String...)} to specify
 * properties you want to keep.
 */
@ProviderType
public final class RemoveAllProperties implements JsonPipelineAction {

  private final Set<String> propertiesToKeep = Sets.newHashSet();

  @Override
  public String getId() {
    return propertiesToKeep.isEmpty() ? "REMOVE-ALL-PROPERTIES" : "REMOVE-ALL-PROPERTIES(" + StringUtils.join(propertiesToKeep, '-') + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    HalResource hal = new HalResource(previousStepOutput.getPayload());

    removePropertiesRecursive(hal, propertiesToKeep);

    return Observable.just(previousStepOutput);
  }

  /**
   * @param properties name of properties that should be preserved
   * @return This action
   */
  public RemoveAllProperties except(String... properties) {

    Stream.of(properties).forEach(property -> propertiesToKeep.add(property));

    return this;
  }

  /**
   * Removes all properties except the specified ones from the given resource and all embedded resources
   * @param hal a HAL resource
   * @param propertiesToKeep all properties that should be left untouched
   */
  public static void removePropertiesRecursive(HalResource hal, Set<String> propertiesToKeep) {

    // remove properties
    hal.getStateFieldNames().stream()
        .filter(property -> !propertiesToKeep.contains(property))
        .forEach(property -> hal.getModel().remove(property));

    // check embedded resources
    hal.getEmbedded().values().stream()
        .forEach(embedded -> removePropertiesRecursive(embedded, propertiesToKeep));

  }

}
