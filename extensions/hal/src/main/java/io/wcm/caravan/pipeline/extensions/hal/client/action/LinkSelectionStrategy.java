/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineInputException;

/**
 * Implements the logic to pick one of the links in a given hal resoucen (by index or name)
 */
public interface LinkSelectionStrategy {

  /**
   * @param resource the HAL resource that contains links
   * @return the link that should be followed or embedded
   * @throws IllegalStateException if no link could be selected (because no links were found at all, or no match the
   *           criteria of the strategy)
   */
  Link pickLink(HalResource resource) throws JsonPipelineInputException;

  /**
   * @return a unique ID used to generate the {@link JsonPipelineAction#getId()}
   */
  String getId();
}
