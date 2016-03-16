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
 * Removes all links for a HAL resource and it's embedded resources which don't fit the given relation names.
 */
@ProviderType
public final class RemoveAllLinks implements JsonPipelineAction {

  private final Set<String> relationsToIgnore = Sets.newHashSet();

  @Override
  public String getId() {
    return relationsToIgnore.isEmpty() ? "REMOVE-AL-LINKS" : "REMOVE-ALL-LINKS-EXCEPT(" + StringUtils.join(relationsToIgnore, '-') + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    HalResource hal = new HalResource(previousStepOutput.getPayload());
    removeLinksRecursive(hal);
    return Observable.just(previousStepOutput);

  }

  /**
   * @param relations Relation name of links not to delete
   * @return This action
   */
  public RemoveAllLinks except(String... relations) {

    Stream.of(relations).forEach(relation -> relationsToIgnore.add(relation));
    return this;

  }

  private void removeLinksRecursive(HalResource hal) {

    // remove links
    hal.getLinks().keySet().stream()
        .filter(relation -> !relationsToIgnore.contains(relation))
        .forEach(relation -> hal.removeLinks(relation));

    // check embedded resources
    hal.getEmbedded().values().stream()
        .forEach(embedded -> removeLinksRecursive(embedded));

  }

}
