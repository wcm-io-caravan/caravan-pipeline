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
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Cache persistency options configure entry store requirements while read or write operations in {@link CacheAdapter}.
 * Options specify cache response refresh interval, storage time and automatical extension of storage time per stored
 * entry. See also {@link CacheStrategy}.
 */
@ProviderType
public final class CachePersistencyOptions {

  private final int refreshInterval;
  private final int storageTime;
  private final boolean extendStorageTimeOnGet;
  private final boolean shouldUseTransientCaches;

  /**
   * Default constructor for complete initialization of cache persistency and transient options.
   * @param refreshInterval cached response refresh interval in seconds
   * @param storageTime time of response storing in seconds
   * @param extendStorageTimeOnGet true if storage time should be extended
   */
  public CachePersistencyOptions(int refreshInterval, int storageTime, boolean extendStorageTimeOnGet) {
    this.refreshInterval = refreshInterval;
    this.storageTime = storageTime;
    this.extendStorageTimeOnGet = extendStorageTimeOnGet;
    this.shouldUseTransientCaches = true;
  }
  
  /**
   * Default constructor for complete initialization of cache persistency and transient options.
   * @param refreshInterval cached response refresh interval in seconds
   * @param storageTime time of response storing in seconds
   * @param extendStorageTimeOnGet true if storage time should be extended
   * @param shouldUseTransientCaches true if transient cache adapters should be enabled, false if disabled
   */
  public CachePersistencyOptions(int refreshInterval, int storageTime, boolean extendStorageTimeOnGet, boolean shouldUseTransientCaches) {
    this.refreshInterval = refreshInterval;
    this.storageTime = storageTime;
    this.extendStorageTimeOnGet = extendStorageTimeOnGet;
    this.shouldUseTransientCaches = shouldUseTransientCaches;
  }

  /**
   * Creates caching options specifying cache refresh interval and storing time per entry without next extension of
   * storing time.
   * @param refreshInterval cached response refresh interval in seconds
   * @param storageTime time of response storing in seconds
   * @return persistent and transient caching options without storage time extension
   */
  public static CachePersistencyOptions createPersistentAndTimeToLive(int refreshInterval, int storageTime) {
    return new CachePersistencyOptions(refreshInterval, storageTime, false);
  }

  /**
   * Creates caching options specifying cache refresh interval and storing time per entry with next extension of
   * storing time.
   * @param refreshInterval cached response refresh interval in seconds
   * @param storageTime time of response storing in seconds
   * @return persistent and transient caching options with storage time extension
   */
  public static CachePersistencyOptions createPersistentAndTimeToIdle(int refreshInterval, int storageTime) {
    return new CachePersistencyOptions(refreshInterval, storageTime, true);
  }

  /**
   * Creates caching options specifying cache refresh interval. No entry storing time is declared.
   * @param refreshInterval cached response refresh interval in seconds
   * @return transient caching options
   */
  public static CachePersistencyOptions createTransient(int refreshInterval) {
    return new CachePersistencyOptions(refreshInterval, 0, false);
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
   * Checks if the entry should be cached.
   * @return true if refresh interval is specified (more than 0).
   */
  public boolean isCacheable() {
    return refreshInterval > 0;
  }
  
  /**
   * Use {@link #shouldUsePersistentCaches()} instead of deprecated {@link #isPersistent()}
   * Check if the entry should be cached by persistent cache implementation.
   * A configuration to enable or disable cache operations by persistent adapters.
   * @return true if persistent adapters should be supported (storage time is specified, more than 0).
   */
  @Deprecated
  public boolean isPersistent(){
	  return storageTime > 0;
  }
  

  /**
   * Check if the entry should be cached by persistent cache implementation.
   * A configuration to enable or disable cache operations by persistent adapters.
   * @return true if persistent adapters should be supported (storage time is specified, more than 0).
   */
  public boolean shouldUsePersistentCaches() {
    return storageTime > 0;
  }
  
  /**
   * Check if the entry should be cached by transient cache implementation.
   * A configuration to enable or disable cache operations by transient adapters.
   * @return true if transient adapter should be supported
   */
  public boolean shouldUseTransientCaches(){
	return shouldUseTransientCaches;
	  
  }

  @Override
  public String toString() {
    return "CachePersistencyOptions [refreshInterval=" + this.refreshInterval + ", storageTime=" + this.storageTime + ", extendStorageTimeOnGet="
        + this.extendStorageTimeOnGet  + ", shouldUseTransientCaches=" + this.shouldUseTransientCaches + "]";
  }


}
