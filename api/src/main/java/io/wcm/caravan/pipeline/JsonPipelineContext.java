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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

/**
 * The context from which a given pipeline instance was created.
 * <ul>
 * <li>the {@link JsonPipelineFactory} used to create the pipeline</li>
 * <li>its context properties</li>
 * <li>the configured {@link CacheAdapter}</li>
 * </ul>
 * This allows {@link JsonPipelineAction} instances or other users of a {@link JsonPipeline} to access those OSGI
 * services.
 */
@ProviderType
public interface JsonPipelineContext {

  /**
   * @return the factory used to create the {@link JsonPipeline}
   */
  JsonPipelineFactory getFactory();

  /**
   * @return the active {@link CacheAdapter} implementation
   */
  CacheAdapter getCacheAdapter();

  /**
   * @return the map of context properties passed to
   *         {@link JsonPipelineFactory#create(io.wcm.caravan.io.http.request.CaravanHttpRequest, Map)} when the
   *         pipeline
   */
  Map<String, String> getProperties();

}
