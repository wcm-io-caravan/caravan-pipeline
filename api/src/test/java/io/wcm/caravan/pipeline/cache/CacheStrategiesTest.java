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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CacheStrategiesTest {

  @Test
  public void testTimeToLive() {
    CacheStrategy underTest = CacheStrategies.timeToLive(55, TimeUnit.SECONDS);
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertTrue(options.isCacheable());
    assertTrue(options.shouldUsePersistentCaches());
    assertEquals(55, options.getStorageTime());
    assertEquals(55, options.getRefreshInterval());
    assertFalse(options.isExtendStorageTimeOnGet());

  }

  @Test
  public void testTimeToLive_Minutes() {
    CacheStrategy underTest = CacheStrategies.timeToLive(55, TimeUnit.MINUTES);
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertEquals(55 * 60, options.getStorageTime());
  }

  @Test
  public void testTimeToLive_Hours() {
    CacheStrategy underTest = CacheStrategies.timeToLive(24, TimeUnit.HOURS);
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertEquals(24 * 60 * 60, options.getStorageTime());
  }

  @Test
  public void testTimeToLive_Days() {
    CacheStrategy underTest = CacheStrategies.timeToLive(2, TimeUnit.DAYS);
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertEquals(2 * 24 * 60 * 60, options.getStorageTime());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTimeToLive_Milliseconds() {
    CacheStrategies.timeToLive(10, TimeUnit.MILLISECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTimeToLive_Microseconds() {
    CacheStrategies.timeToLive(10, TimeUnit.MICROSECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTimeToLive_Nanoseconds() {
    CacheStrategies.timeToLive(10, TimeUnit.NANOSECONDS);
  }

  @Test
  public void testTimeToIdle() {
    CacheStrategy underTest = CacheStrategies.timeToIdle(55, TimeUnit.SECONDS);
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertTrue(options.isCacheable());
    assertTrue(options.shouldUsePersistentCaches());
    assertEquals(DAYS.toSeconds(365), options.getRefreshInterval());
    assertEquals(55, options.getStorageTime());
    assertTrue(options.isExtendStorageTimeOnGet());
  }

  @Test
  public void testNoCache() {
    CacheStrategy underTest = CacheStrategies.noCache();
    CachePersistencyOptions options = underTest.getCachePersistencyOptions(null);
    assertFalse(options.isCacheable());
    assertFalse(options.shouldUsePersistentCaches());
    assertEquals(0, options.getRefreshInterval());
    assertEquals(0, options.getStorageTime());
    assertFalse(options.isExtendStorageTimeOnGet());
  }

}
