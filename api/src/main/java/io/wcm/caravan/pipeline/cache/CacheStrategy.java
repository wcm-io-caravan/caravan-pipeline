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

import java.util.Collection;


/**
 * A cache-strategy implements the core decisions about storage time and lifetime of responses that are temporarily or
 * permanently cached by inserting cache points into a JsonPipeline (via
 * {@link JsonPipeline#addCachePoint(CacheStrategy)}). The decisions can vary depending on the REST requests that were
 * executed to obtain the JSON data to be cached.
 */
public interface CacheStrategy {

  /**
   * Defines the time (in seconds) after which a cached response for the given request should be considered stale. After
   * this time, the {@link JsonPipeline} will try to fetch a fresh response from the origin server, and will only use
   * the stale content from cache as a fallback if this attempt fails.
   * @param requests the REST requests that were used to obtain the data to be stored in the cache
   * @return the duration in seconds until a cached response is considered stale, and should be re-validated
   */
  int getRefreshInterval(Collection<Request> requests);

  /**
   * Defines the time (in seconds) for which the response to the given request should be at least stored in the cache.
   * The actual storage time can be longer if {@link #isExtendStorageTimeOnGet(Collection)} or for other
   * technical reasons.
   * @param requests the REST requests that were used to obtain the data to be stored in the cache
   * @return the minimum duration in seconds to keep the response in the cache
   */
  int getStorageTime(Collection<Request> requests);

  /**
   * Determines whether to automatically extend the storage time (to the duration returned by
   * {@link #getStorageTime(Collection)} whenever a response was successfully retrieved from cache, resulting in a
   * "time-to-idle" cache behavior, where the total storage time is unlimited, but instead data is kept in the cache
   * as long as it is still being requested.
   * @param requests the REST requests that were used to obtain the data to be stored in the cache
   * @return true if the storage value for this request should be extended on every cache hit
   */
  boolean isExtendStorageTimeOnGet(Collection<Request> requests);
}
