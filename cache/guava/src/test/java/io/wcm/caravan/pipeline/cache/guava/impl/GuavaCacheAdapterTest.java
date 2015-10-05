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
package io.wcm.caravan.pipeline.cache.guava.impl;

import static org.junit.Assert.assertTrue;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;

import org.junit.Test;

import rx.Observable;

public class GuavaCacheAdapterTest extends AbstractGuavaTestCase {

  @Test
  public void testPutAndGetWithOptions() {
    cacheAdapter.put("key", "value", options);
    assertGet("key", options, "value");
  }

  @Test
  public void testPutWithDisabledShouldUseTransientCachesAndGet() {
    CachePersistencyOptions optionsDisabledShouldUseTransientCaches = new CachePersistencyOptions(100, 10, true, false);
    cacheAdapter.put("key", "value", optionsDisabledShouldUseTransientCaches);
    Observable<String> cachedValueObservable = cacheAdapter.get("key", options);
    assertTrue(cachedValueObservable.isEmpty().toBlocking().first());
  }

  @Test
  public void testPutAndGetWithDisabledShouldUseTransientCaches() {
    CachePersistencyOptions optionsDisabledShouldUseTransientCaches = new CachePersistencyOptions(100, 10, true, false);
    cacheAdapter.put("key", "value", options);
    Observable<String> cachedValueObservable = cacheAdapter.get("key", optionsDisabledShouldUseTransientCaches);
    assertTrue(cachedValueObservable.isEmpty().toBlocking().first());
  }

  @Test(expected = NullPointerException.class)
  public void testPutAndGetNullOptions() {
    cacheAdapter.put("key", "value", null);
    assertGet("key", null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void testPutNullOptionsAndGetWithOptions() {
    cacheAdapter.put("key", "value", null);
    assertGet("key", options, "value");
  }

  @Test(expected = NullPointerException.class)
  public void testPutWithOptionsAndGetNoOptions() {
    cacheAdapter.put("key", "value", options);
    assertGet("key", null, "value");
  }


}
