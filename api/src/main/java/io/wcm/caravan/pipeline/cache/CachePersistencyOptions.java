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

import io.wcm.caravan.pipeline.JsonPipeline;

/**
 * Cache persistency options declare cache response refresh interval, storage time and automatical extension of
 * storage time. See also {@link CacheStrategy}.
 */
public class CachePersistencyOptions {

  private final int refreshInterval;
  private final int storageTime;
  private final boolean extendStorageTimeOnGet;

  /**
   * Default constructor for complete initialization of cache persistency options.
   * @param refreshInterval cached response refresh interval in seconds
   * @param storageTime time of response storing in seconds
   * @param extendStorageTimeOnGet true if storage time should be extended
   */
  public CachePersistencyOptions(int refreshInterval, int storageTime, boolean extendStorageTimeOnGet) {
    this.refreshInterval = refreshInterval;
    this.storageTime = storageTime;
    this.extendStorageTimeOnGet = extendStorageTimeOnGet;
  }

  /**
   * Defines the time (in seconds) after which a cached response for the given request should be considered stale. After
   * this time, the {@link JsonPipeline} will try to fetch a fresh response from the origin server, and will only use
   * the stale content from cache as a fallback if this attempt fails.
   * @return the duration in seconds until a cached response is considered stale, and should be re-validated
   */
  public int getRefreshInterval() {
    return refreshInterval;
  }

  /**
   * Defines the time (in seconds) for which the response to the given request should be at least stored in the cache.
   * The actual storage time can be longer if {@link #isExtendStorageTimeOnGet()} or for other technical reasons.
   * @return the minimum duration in seconds to keep the response in the cache
   */
  public int getStorageTime() {
    return storageTime;
  }

  /**
   * Determines whether to automatically extend the storage time (to the duration returned by {@link #getStorageTime()}
   * whenever a response was successfully retrieved from cache, resulting in a "time-to-idle" cache behavior, where the
   * total storage time is unlimited, but instead data is kept in the cache as long as it is still being requested.
   * @return true if the storage value for this request should be extended on every cache hit
   */
  public boolean isExtendStorageTimeOnGet() {
    return extendStorageTimeOnGet;
  }

  /**
   * Checks if the caching logic is not available. Skip cache usage if no caching logic (storage time or refresh
   * interval) is specified.
   * @return true if storage time or refresh interval are not specified (are equivalent to 0).
   */
  public boolean isInvalidCachingLogic() {
    return storageTime == 0 || refreshInterval == 0;
  }

  @Override
  public String toString() {
    return "CachePersistencyOptions [refreshInterval=" + this.refreshInterval + ", storageTime=" + this.storageTime + ", extendStorageTimeOnGet="
        + this.extendStorageTimeOnGet + "]";
  }


}
