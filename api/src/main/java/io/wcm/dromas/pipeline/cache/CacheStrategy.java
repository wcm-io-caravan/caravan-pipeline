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
package io.wcm.dromas.pipeline.cache;

import io.wcm.dromas.io.http.request.Request;


/**
 * Allows the orchestrating service to specify caching-behavior (in addition to the cache header from the request).
 */
public interface CacheStrategy {

  /**
   * @param request Request
   * @return the duration in seconds to keep the response in the cache
   */
  int getExpirySeconds(Request request);

  /**
   * Can be used to automatically reset the expiry time (to the duration returned by {@link #getExpirySeconds(Request)}
   * whenever a response was successfully retrieved from cache
   * @param request Request
   * @return true if the expiry value for this request should be extended on every cache hit
   */
  boolean isResetExpiryOnGet(Request request);

}
