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
package io.wcm.caravan.pipeline.cache.spi;

import rx.Observable;

/**
 * Common interface for Couchbase-, In-Memory- and Mock-Caches.
 */
public interface CacheAdapter {

  /**
   * Generate a unique cache key
   * @param servicePrefix logical name of the source service(s)
   * @param descriptor from the pipeline
   * @return the unique cache key
   */
  String getCacheKey(String servicePrefix, String descriptor);

  /**
   * retrieve an item from cache
   * @param cacheKey Cache key
   * @param extendExpiry whether to reset the expiry time for cache-hits, to achieve a "time-to-idle" caching behaviour
   * @param expirySeconds the value to reset the expiry time to if extendExpiry is true
   * @return an observable that will either emit the cached JSON string, or complete without emitting on a cache miss
   */
  Observable<String> get(String cacheKey, boolean extendExpiry, int expirySeconds);

  /**
   * store an item in the cache
   * @param cacheKey Cache key
   * @param jsonString JSON data
   * @param expirySeconds how long to keep the entry in the cache
   */
  void put(String cacheKey, String jsonString, int expirySeconds);

}
