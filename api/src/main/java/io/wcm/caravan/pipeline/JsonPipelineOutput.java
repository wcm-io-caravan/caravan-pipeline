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
package io.wcm.caravan.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The immutable output of a {@link JsonPipeline}'s processing step: the
 * main JSON content payload, and additional metadata about the status and cachability of the content.
 */
public interface JsonPipelineOutput {

  /**
   * @return the HTTP status code
   */
  int getStatusCode();

  /**
   * @return the duration in seconds for which the content can be stored in cache, before it should be re-validated
   */
  int getMaxAge();

  /**
   * @return the JSON content
   */
  JsonNode getPayload();

  /**
   * @param newPayload the JSON output
   * @return a new instance with the same metadata, but new JSON content
   */
  JsonPipelineOutput withPayload(JsonNode newPayload);

  /**
   * @param newStatusCode the HTTP response status code to return to clients
   * @return a new instance with the same JSON content but updated metadata
   */
  JsonPipelineOutput withStatusCode(int newStatusCode);

  /**
   * @param newMaxAge for how many seconds can the response be cached
   * @return a new instance with the same JSON content but updated metadata
   */
  JsonPipelineOutput withMaxAge(int newMaxAge);
}
