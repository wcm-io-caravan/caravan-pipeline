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
 * Added handling of minimal maxAge value via utility.
 * JsonPipelineOutputUtil.minMaxAge should return JsonPipelineOutput, which has the minimal maxAge value of two
 * arguments
 * JsonPipelineOutputUtil.enrichWithLowestMaxAge should return the first JsonPipelineOutput, if it has minimal maxAge,
 * or create a new one using first JsonPipelineOutput argument and maxAge from second JsonPipelineOutput, if second has
 * the lowest maxAge
 */
public final class JsonPipelineOutputUtil {

  private JsonPipelineOutputUtil() {
    // static methods only
  }

  /**
   * Checks if the actual {@link JsonPipelineOutput} has the lowest max age value in comparison to the next
   * {@link JsonPipelineOutput} argument. Returns the actual {@link JsonPipelineOutput} without any modification, if it
   * has the lowest or equal max age value to the max age value of the next output. Otherwise creates a new
   * {@link JsonPipelineOutput} instance from the actual output enriched with the max age value of the
   * next output.
   * @param actualPipelineOutput an JSON pipeline output to be checked
   * @param nextPipelineOutput an JSON pipeline output to be compared with
   * @return JSON pipeline output with the lowest max age value
   */
  public static JsonPipelineOutput enrichWithLowestAge(JsonPipelineOutput actualPipelineOutput,
      JsonPipelineOutput nextPipelineOutput) {

    if (actualPipelineOutput == null || nextPipelineOutput == null) {
      return actualPipelineOutput;
    }

    return actualPipelineOutput.getMaxAge() <= nextPipelineOutput.getMaxAge()
        ? actualPipelineOutput
            : actualPipelineOutput.withMaxAge(nextPipelineOutput.getMaxAge());
  }

  /**
   * Checks if the actual {@link JsonPipelineOutput} has the lowest max age value in comparison to the array of
   * {@link JsonPipelineOutput} arguments. Returns the actual {@link JsonPipelineOutput} without any modification, if it
   * has the lowest or equal max age value to the lowest max age value of array elements. Otherwise creates a new
   * {@link JsonPipelineOutput} instance from the actual output enriched with the lowest max age value of the array
   * outputs.
   * @param actualPipelineOutput an JSON pipeline output to be checked
   * @param jsonPipelineOutputs an array of JSON pipeline outputs to be compared with
   * @return JSON pipeline output with the lowest max age value
   */
  public static JsonPipelineOutput enrichWithLowestAge(JsonPipelineOutput actualPipelineOutput,
      JsonPipelineOutput... jsonPipelineOutputs) {

    if (actualPipelineOutput == null || jsonPipelineOutputs == null) {
      return actualPipelineOutput;
    }

    return enrichWithLowestAge(actualPipelineOutput, minAge(jsonPipelineOutputs));
  }

  /**
   * Compares all JSON pipeline outputs specifying as input parameters and returns the earliest of them, which has the
   * lowest max age value {@link JsonPipelineOutput#getMaxAge()}.
   * @param jsonPipelineOutputs multiple JSON pipeline outputs to compare max age value
   * @return JSON pipeline output with the lowest max age value
   */
  public static JsonPipelineOutput minAge(JsonPipelineOutput... jsonPipelineOutputs) {

    JsonPipelineOutput lowestAgeOutput = null;

    if (jsonPipelineOutputs != null) {
      for (JsonPipelineOutput actualOutput : jsonPipelineOutputs) {
        if (actualOutput != null) {
          lowestAgeOutput = (lowestAgeOutput == null) ? actualOutput : (lowestAgeOutput.getMaxAge() <= actualOutput.getMaxAge() ? lowestAgeOutput
              : actualOutput);
        }
      }
    }

    return lowestAgeOutput;
  }
}
