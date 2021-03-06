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

import static java.util.concurrent.TimeUnit.DAYS;

import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

/**
 * Default implementations of different cache strategies.
 */
@ProviderType
public final class CacheStrategies {

  private static final EnumSet SUPPORTED_TIME_UNITS = EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES,
      TimeUnit.HOURS, TimeUnit.DAYS);

  private CacheStrategies() {
    // static methods only
  }

  /**
   * Invalidate item after a fixed time-to-live interval, using the same duration as storage time and
   * revalidation-interval
   * @param duration Time-to-live duration
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy timeToLive(int duration, TimeUnit unit) {
    int refreshInterval = toSeconds(duration, unit);
    int storageTime = toSeconds(duration, unit);
    return new CacheStrategy() {

      @Override
      public CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests) {
        return CachePersistencyOptions.createPersistentAndTimeToLive(refreshInterval, storageTime);
      }
    };
  }

  /**
   * Invalidate item after a time-to-idle interval: The content is considered immutable, and storage time will be
   * extended to specified value on each get operation on this item, so it is kept in cache as long as it is requested
   * @param duration Time-to-idle duration
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy timeToIdle(int duration, TimeUnit unit) {
    int refreshInterval = toSeconds(365, DAYS);
    int storageTime = toSeconds(duration, unit);
    return new CacheStrategy() {

      @Override
      public CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests) {
        return CachePersistencyOptions.createPersistentAndTimeToIdle(refreshInterval, storageTime);
      }
    };
  }
  
  /**
   * Invalidate item after a time-to-idle interval: The content is considered immutable, and storage time will be
   * extended to specified value on each get operation on this item, so it is kept in cache as long as it is requested.
   * Cache operations should be ignored by transient adapters and passed to the persistent adapters only.
   * @param duration Time-to-idle duration
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy nonTransientAndTimeToIdle(int duration, TimeUnit unit) {
    int refreshInterval = toSeconds(365, DAYS);
    int storageTime = toSeconds(duration, unit);
    return new CacheStrategy() {

      @Override
      public CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests) {
        return new CachePersistencyOptions(refreshInterval, storageTime, true, false);
      }
    };
  }

  /**
   * Stores items only in the local, non-persistent {@link CacheAdapter} for the given maximum duration.
   * @param duration maximum Time-to-live
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy temporary(int duration, TimeUnit unit) {
    int refreshInterval = toSeconds(duration, unit);
    return new CacheStrategy() {

      @Override
      public CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests) {
        return CachePersistencyOptions.createTransient(refreshInterval);
      }
    };
  }

  private static int toSeconds(int duration, TimeUnit unit) {
    if (!SUPPORTED_TIME_UNITS.contains(unit)) {
      throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
    long seconds = unit.toSeconds(duration);
    if (seconds > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Duration is too long: " + seconds + " seconds");
    }
    return (int)unit.toSeconds(duration);
  }

  /**
   * No caching. Can be used to disable caching in a {@link JsonPipeline} that already has some Cachepoints set.
   * @return Cache strategy
   */
  public static CacheStrategy noCache() {
    return new CacheStrategy() {

      @Override
      public CachePersistencyOptions getCachePersistencyOptions(Collection<CaravanHttpRequest> requests) {
        return CachePersistencyOptions.createTransient(0);
      }
    };
  }


}
