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


class JsonPipelineMetadata {

  private int statusCode;
  private int maxAge;

  JsonPipelineMetadata(int responseCode) {
    this.statusCode = responseCode;
    this.maxAge = 0;
  }

  JsonPipelineMetadata(JsonPipelineMetadata toClone) {
    this.statusCode = toClone.statusCode;
    this.maxAge = toClone.maxAge;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public int getMaxAge() {
    return this.maxAge;
  }

  public void setMaxAge(int maxAge) {
    this.maxAge = maxAge;
  }

}
