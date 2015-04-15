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
package io.wcm.caravan.pipeline.impl;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of JsonPipelineOutput
 */
public class JsonPipelineOutputImpl implements JsonPipelineOutput {

  private final JsonPipelineMetadata metadata;
  private final JsonNode payload;
  private final List<CaravanHttpRequest> requests;

  /**
   * Creates pipeline output with the given payload
   * @param payload
   */
  public JsonPipelineOutputImpl(JsonNode payload, List<CaravanHttpRequest> requests) {
    this.metadata = new JsonPipelineMetadata(200);
    this.payload = payload;
    this.requests = requests;
  }

  JsonPipelineOutputImpl(JsonPipelineMetadata metadata, JsonNode payload, List<CaravanHttpRequest> requests) {
    this.metadata = metadata;
    this.payload = payload;
    this.requests = requests;
  }

  JsonPipelineOutputImpl deepCopy() {
    JsonPipelineOutputImpl copy = new JsonPipelineOutputImpl(payload, requests);
    copy.getMetadata().setMaxAge(getMaxAge());
    copy.getMetadata().setStatusCode(getStatusCode());
    return copy;
  }

  @Override
  public JsonNode getPayload() {
    return payload;
  }

  public JsonPipelineMetadata getMetadata() {
    return metadata;
  }

  @Override
  public JsonPipelineOutput withPayload(JsonNode newPayload) {
    return new JsonPipelineOutputImpl(metadata, newPayload, requests);
  }

  @Override
  public int getStatusCode() {
    return metadata.getStatusCode();
  }

  @Override
  public JsonPipelineOutput withStatusCode(int statusCode) {
    JsonPipelineOutputImpl copy = deepCopy();
    copy.getMetadata().setStatusCode(statusCode);
    return copy;
  }

  @Override
  public int getMaxAge() {
    return metadata.getMaxAge();
  }

  @Override
  public JsonPipelineOutput withMaxAge(int expirySeconds) {
    JsonPipelineOutputImpl copy = deepCopy();
    copy.getMetadata().setMaxAge(expirySeconds);
    return copy;
  }

  @Override
  public List<CaravanHttpRequest> getRequests() {
    return requests;
  }
}
