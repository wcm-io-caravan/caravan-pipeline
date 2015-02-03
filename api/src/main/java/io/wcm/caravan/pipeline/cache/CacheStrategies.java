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

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * Default implementations of differente cache strategies.
 */
public final class CacheStrategies {

  private static final EnumSet SUPPORTED_TIME_UNITS = EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES,
      TimeUnit.HOURS, TimeUnit.DAYS);

  private CacheStrategies() {
    // static methods only
  }

  /**
   * Invalidate item after a fixed time-to-live interval.
   * @param duration Time-to-live duration
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy timeToLive(int duration, TimeUnit unit) {
    return new CacheStrategyImpl(toSeconds(duration, unit), false);
  }

  /**
   * Invalidate item after a time-to-idle interval, prolong expery on each get operation on this item.
   * @param duration Time-to-live duration
   * @param unit Time unit
   * @return Cache strategy
   */
  public static CacheStrategy timeToIdle(int duration, TimeUnit unit) {
    return new CacheStrategyImpl(toSeconds(duration, unit), true);
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
    return new CacheStrategyImpl(0, false);
  }

  private static class CacheStrategyImpl implements CacheStrategy {

    private final int expirySeconds;
    private final boolean resetExpiryOnGet;

    public CacheStrategyImpl(int expirySeconds, boolean resetExpiryOnGet) {
      this.expirySeconds = expirySeconds;
      this.resetExpiryOnGet = resetExpiryOnGet;
    }

    @Override
    public int getStaleSeconds(Request request) {
      return this.expirySeconds;
    }

    @Override
    public int getExpirySeconds(Request request) {
      return this.expirySeconds;
    }

    @Override
    public boolean isResetExpiryOnGet(Request request) {
      return this.resetExpiryOnGet;
    }
  }

}
