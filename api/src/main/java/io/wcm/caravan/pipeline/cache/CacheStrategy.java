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

import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.pipeline.JsonPipeline;


/**
 * Allows the orchestrating service to specify caching-behavior (in addition to the cache header from the request).
 */
public interface CacheStrategy {

  /**
   * Defines the time (in seconds) after which a cached response for the given request should be considered stale. After
   * this time, the {@link JsonPipeline} will try to fetch a fresh response from the origin server, and will only use
   * the stale content from cache as a fallback if this attempt fails.
   * @param request the REST request whose response to store in the cache
   * @return the duration in seconds until a cached response is considered stale, and should be re-validated
   */
  int getRefreshInterval(Request request);

  /**
   * Defines the time (in seconds) for which the response to the given request should be at least stored in the cache.
   * The actual storage time can be longer if {@link #isExtendStorageTimeOnGet(Request)} or for other technical reasons.
   * @param request the REST request whose response to store in the cache
   * @return the minimum duration in seconds to keep the response in the cache
   */
  int getStorageTime(Request request);

  /**
   * Can be used to automatically extend the storage time (to the duration returned by {@link #getStorageTime(Request)}
   * whenever a response was successfully retrieved from cache, i.e. use a "time-to-idle" approach.
   * @param request Request the REST request whose response to store in the cache
   * @return true if the storage value for this request should be extended on every cache hit
   */
  boolean isExtendStorageTimeOnGet(Request request);
}
