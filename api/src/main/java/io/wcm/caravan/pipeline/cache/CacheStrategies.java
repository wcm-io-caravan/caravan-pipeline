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
 * Default implementations of differente cache strategies.
 */
public final class CacheStrategies {

  private CacheStrategies() {
    // static methods only
  }

  /**
   * Invalidate item after a fixed time-to-live interval.
   * @param seconds Time-to-live interval in seconds
   * @return Cache strategy
   */
  public static CacheStrategy timeToLive(int seconds) {
    return new CacheStrategyImpl(seconds, false);
  }

  /**
   * Invalidate item after a time-to-idle interval, prolong expery on each get operation on this item.
   * @param seconds Time-to-idle interval in seconds
   * @return Cache strategy
   */
  public static CacheStrategy timeToIdle(int seconds) {
    return new CacheStrategyImpl(seconds, true);
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
    public int getExpirySeconds(Request request) {
      return this.expirySeconds;
    }

    @Override
    public boolean isResetExpiryOnGet(Request request) {
      return this.resetExpiryOnGet;
    }

  }

}
