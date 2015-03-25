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
package io.wcm.caravan.pipeline.cache;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipeline;

import java.util.Collection;


/**
 * A cache-strategy implements the core decisions about storage time and lifetime of responses that are temporarily or
 * permanently cached by inserting cache points into a JsonPipeline (via
 * {@link JsonPipeline#addCachePoint(CacheStrategy)}). The decisions can vary depending on the REST requests that were
 * executed to obtain the JSON data to be cached.
 */
public interface CacheStrategy {

  /**
   * @param requests the REST requests that were used to obtain the data to be stored in the cache
   * @return cache persistency options for persistent cache strategies
   */
  CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests);

}
