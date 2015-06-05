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
package io.wcm.caravan.pipeline.extensions.hal.crawler;

import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.List;

/**
 * Processes the current JSON Pipeline output and combines it with the JSON Pipeline output of the loaded links.
 */
public interface OutputProcessor {

  /**
   * @param previousStepOutput Current JSON Pipeline output
   * @param loadedLinkOutputs JSON Pipeline output of loaded links
   * @return Combined JSON pipeline output
   */
  JsonPipelineOutput process(JsonPipelineOutput previousStepOutput, List<JsonPipelineOutput> loadedLinkOutputs);

  /**
   * @return Unique ID
   */
  String getId();

}
