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

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;

/**
 * JSON Pipeline factory service interface.
 */
public interface JsonPipelineFactory {

  /**
   * Creates a new {@link JsonPipeline} to process the response from a {@link CaravanHttpClient} request for the given
   * service
   * @param serviceName the logical service name (is mapped to configured real host names).
   * @param request the REST request to execute
   * @return the new pipeline
   */
  JsonPipeline create(String serviceName, CaravanHttpRequest request);

  /**
   * Can be used to create an pipeline that can be used as a root to merge other pipeline's responses
   * @return a new {@link JsonPipeline} that produces an empty JSON object
   */
  JsonPipeline createEmpty();

}
