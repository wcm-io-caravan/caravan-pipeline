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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CacheStrategiesTest {

  @Test
  public void testTimeToLive() {
    CacheStrategy underTest = CacheStrategies.timeToLive(55, TimeUnit.SECONDS);
    assertEquals(55, underTest.getExpirySeconds(null));
    assertFalse(underTest.isResetExpiryOnGet(null));
  }

  @Test
  public void testTimeToLive_Minutes() {
    CacheStrategy underTest = CacheStrategies.timeToLive(55, TimeUnit.MINUTES);
    assertEquals(55 * 60, underTest.getExpirySeconds(null));
  }

  @Test
  public void testTimeToLive_Hours() {
    CacheStrategy underTest = CacheStrategies.timeToLive(24, TimeUnit.HOURS);
    assertEquals(24 * 60 * 60, underTest.getExpirySeconds(null));
  }

  @Test
  public void testTimeToLive_Days() {
    CacheStrategy underTest = CacheStrategies.timeToLive(2, TimeUnit.DAYS);
    assertEquals(2 * 24 * 60 * 60, underTest.getExpirySeconds(null));
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
    assertEquals(55, underTest.getExpirySeconds(null));
    assertTrue(underTest.isResetExpiryOnGet(null));
  }

  @Test
  public void testNoCache() {
    CacheStrategy underTest = CacheStrategies.noCache();
    assertEquals(0, underTest.getExpirySeconds(null));
    assertFalse(underTest.isResetExpiryOnGet(null));
  }

}
