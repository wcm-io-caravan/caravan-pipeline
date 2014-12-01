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
package io.wcm.dromas.pipeline.cache.spi;

import io.wcm.dromas.io.http.request.Request;
import io.wcm.dromas.pipeline.cache.CacheStrategy;
import rx.Observable;

/**
 * Common interface for Couchbase-, In-Memory- and Mock-Caches.
 */
public interface CacheAdapter {

  /**
   * Generate a unique cache key
   * @param serviceName logical name of the service
   * @param descriptor from the pipeline
   * @return the unique cache key
   */
  String getCacheKey(String serviceName, String descriptor);

  /**
   * retrieve an item from cache
   * @param cacheKey Cache key
   * @param strategy Cache strategy
   * @param request TODO:
   * @return an observable that will either emit the cached JSON string, or complete without emitting on a cache miss
   */
  Observable<String> get(String cacheKey, CacheStrategy strategy, Request request);

  /**
   * store an item in the cache
   * @param cacheKey Cache key
   * @param jsonString JSON data
   * @param strategy Cache strategy
   * @param request TODO:
   */
  void put(String cacheKey, String jsonString, CacheStrategy strategy, Request request);

}
