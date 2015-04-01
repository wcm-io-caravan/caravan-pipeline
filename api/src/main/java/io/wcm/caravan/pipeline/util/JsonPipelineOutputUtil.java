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
package io.wcm.caravan.pipeline.util;

import io.wcm.caravan.pipeline.JsonPipelineOutput;

/**
 *
 */
public final class JsonPipelineOutputUtil {

  private JsonPipelineOutputUtil() {
    // static methods only
  }

  /**
   * @param actualPipelineOutput
   * @param toComparePipelineOutput
   * @return
   */
  public static JsonPipelineOutput enrichWithLowestMaxAge(JsonPipelineOutput actualPipelineOutput,
      JsonPipelineOutput toComparePipelineOutput) {
    JsonPipelineOutput result = null;
    if (actualPipelineOutput != null) {
      if (toComparePipelineOutput != null) {
        result = actualPipelineOutput.getMaxAge() <= toComparePipelineOutput.getMaxAge()
            ? actualPipelineOutput
                : actualPipelineOutput.withMaxAge(toComparePipelineOutput.getMaxAge());
      }
      else {
        result = actualPipelineOutput;
      }
    }
    return result;
  }

  /**
   * @param actualPipelineOutput
   * @param toComparePipelineOutput
   * @return
   */
  public static JsonPipelineOutput minMaxAge(JsonPipelineOutput actualPipelineOutput,
      JsonPipelineOutput toComparePipelineOutput) {
    JsonPipelineOutput result = null;
    if (actualPipelineOutput != null) {
      if (toComparePipelineOutput != null) {
        result = actualPipelineOutput.getMaxAge() <= toComparePipelineOutput.getMaxAge() ? actualPipelineOutput : toComparePipelineOutput;
      }
      else {
        result = actualPipelineOutput;
      }
    }
    else if (toComparePipelineOutput != null) {
      result = toComparePipelineOutput;
    }
    return result;
  }
}
