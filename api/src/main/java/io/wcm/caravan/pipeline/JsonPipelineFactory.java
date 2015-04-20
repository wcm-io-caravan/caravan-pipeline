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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * JSON Pipeline factory service interface.
 */
@ProviderType
public interface JsonPipelineFactory {

  /**
   * Creates a new {@link JsonPipeline} to process the response from a {@link CaravanHttpClient} request for the given
   * service
   * @param request the REST request to execute
   * @return the new pipeline
   */
  JsonPipeline create(final CaravanHttpRequest request);

  /**
   * Creates a new {@link JsonPipeline} to process the response from a {@link CaravanHttpClient} request for the given
   * service and predefined cache meta data.
   * @param request the REST request to execute
   * @param contextProperties a map with cache meta data
   * @return the new pipeline
   */
  JsonPipeline create(final CaravanHttpRequest request, Map<String, String> contextProperties);

  /**
   * Creates an empty pipeline. It could be used as a root to merge other pipeline's responses
   * @return a new {@link JsonPipeline} that produces an empty JSON object
   */
  JsonPipeline createEmpty();

  /**
   * Creates an empty pipeline with predefined cache meta data. It could be used as a root to merge other pipeline's
   * responses.
   * @param contextProperties a map with cache meta data
   * @return a new {@link JsonPipeline} that produces an empty JSON object
   */
  JsonPipeline createEmpty(Map<String, String> contextProperties);

}
