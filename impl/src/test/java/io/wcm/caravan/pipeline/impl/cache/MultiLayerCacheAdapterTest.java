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
package io.wcm.caravan.pipeline.impl.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class MultiLayerCacheAdapterTest {

  @Mock
  private CacheAdapter secondLevelCacheAdapter;
  @Mock
  private CacheAdapter firstLevelCacheAdapter;
  private CacheAdapter cacheAdapter;
  private CachePersistencyOptions options;

  @Before
  public void setup() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, secondLevelCacheAdapter);
    options = CachePersistencyOptions.createPersistentAndTimeToIdle(100, 10);
  }

  @Test
  public void testGetCacheKeyShortestFromPersistent() {
    when(secondLevelCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("key");
    when(firstLevelCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");

    //Checks that the shortest key was chosen, when the second level cache adapters returns the shortest.
    assertEquals("key", cacheKey);

    // Checks that get cache key operations  was called at all child cache adapters
    verify(secondLevelCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
    verify(firstLevelCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyShortestFromNonPersistent() {
    when(secondLevelCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    when(firstLevelCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("key");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");

    //Checks that the shortest key was chosen, when the first level cache adapters returns the shortest.
    assertEquals("key", cacheKey);

    // Checks that get cache key operations  was called at all child cache adapters
    verify(secondLevelCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
    verify(firstLevelCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeySingleLevelCacheOnly() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, null);
    when(firstLevelCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");

    //Checks that the key was chosen, which was returned by the single cache adapter.
    assertEquals("longKey", cacheKey);

    // Checks that get cache key operation  was called at single cache adapter.
    verify(firstLevelCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyNotInitialized() {
    cacheAdapter = createMultilayerCacheAdapter(null, null);
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertEquals(MultiLayerCacheAdapter.MULTILAYER_CACHE_KEY, cacheKey);
  }

  @Test
  public void testPut() {
    cacheAdapter.put("key", "entry", options);
    verify(secondLevelCacheAdapter, times(1)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutNullOptions() {
    cacheAdapter.put("key", "entry", null);
    verify(secondLevelCacheAdapter, times(1)).put("key", "entry", null);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testPutNonPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, null);
    cacheAdapter.put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutNonPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, null);
    cacheAdapter.put("key", "entry", null);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testPutPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(null, secondLevelCacheAdapter);
    cacheAdapter.put("key", "entry", options);
    verify(secondLevelCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(null, secondLevelCacheAdapter);
    cacheAdapter.put("key", "entry", null);
    verify(secondLevelCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testFromFirstLevelWithOptions() {
    when(firstLevelCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", options, "entry");

    // Checks that get operation was called first level cache adapter only. No put operations was called.
    verify(secondLevelCacheAdapter, times(0)).get("key", options);
    verify(firstLevelCacheAdapter, times(1)).get("key", options);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetFromFirstLevelNoOptions() {
    when(firstLevelCacheAdapter.get("key", null)).thenReturn(Observable.just("entry"));

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", null, "entry");

    // Checks that get operation was called first level cache adapter only. No put operations was called.
    verify(secondLevelCacheAdapter, times(0)).get("key", null);
    verify(firstLevelCacheAdapter, times(1)).get("key", null);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", null);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetFromSecondLevelWithOptions() {
    when(secondLevelCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    when(firstLevelCacheAdapter.get("key", options)).thenReturn(Observable.empty());

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", options, "entry");

    // Checks that get operation was called at the both cache adapters. Put operation was called on the first level cache adapter only.
    verify(secondLevelCacheAdapter, times(1)).get("key", options);
    verify(firstLevelCacheAdapter, times(1)).get("key", options);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testGetFromSecondLevelNoOptions() {
    when(secondLevelCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    when(firstLevelCacheAdapter.get("key", options)).thenReturn(Observable.empty());

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", options, "entry");

    // Checks that get operation was called at the both cache adapters. Put operation was called on the first level cache adapter only.
    verify(secondLevelCacheAdapter, times(1)).get("key", options);
    verify(firstLevelCacheAdapter, times(1)).get("key", options);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testGetEmptyWithOptions() {
    when(secondLevelCacheAdapter.get("key", options)).thenReturn(Observable.empty());
    when(firstLevelCacheAdapter.get("key", options)).thenReturn(Observable.empty());

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is absent.
    assertGetEmpty("key", options);

    // Checks that get operation was called at the both cache adapters. No put operation was executed.
    verify(secondLevelCacheAdapter, times(1)).get("key", options);
    verify(firstLevelCacheAdapter, times(1)).get("key", options);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetEmptyNoOptions() {
    when(secondLevelCacheAdapter.get("key", null)).thenReturn(Observable.empty());
    when(firstLevelCacheAdapter.get("key", null)).thenReturn(Observable.empty());

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is absent.
    assertGetEmpty("key", null);

    // Checks that get operation was called at the both cache adapters. No put operation was executed.
    verify(secondLevelCacheAdapter, times(1)).get("key", null);
    verify(firstLevelCacheAdapter, times(1)).get("key", null);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", null);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetSingleLevelOnlyWithOptions() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, null);
    when(firstLevelCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", options, "entry");

    // Checks that child cache adapter was called only to get. No put operation was executed.
    verify(firstLevelCacheAdapter, times(1)).get("key", options);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetSingleLevelOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(firstLevelCacheAdapter, null);
    when(firstLevelCacheAdapter.get("key", null)).thenReturn(Observable.just("entry"));

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is equal to the expected entry.
    assertGetEquals("key", null, "entry");

    // Checks that child cache adapter was called only to get. No put operation was executed.
    verify(firstLevelCacheAdapter, times(1)).get("key", null);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testNotInitialized() {
    cacheAdapter = createMultilayerCacheAdapter(null, null);

    // Get observable from cache by key and options, and subscribe on it. Assert if subscription result is absent.
    assertGetEmpty("key", options);

    // Checks that no put or get operations were executed for other cache adapters.
    verify(secondLevelCacheAdapter, times(0)).get("key", options);
    verify(secondLevelCacheAdapter, times(0)).put("key", "entry", options);
    verify(firstLevelCacheAdapter, times(0)).get("key", options);
    verify(firstLevelCacheAdapter, times(0)).put("key", "entry", options);
  }

  public MultiLayerCacheAdapter createMultilayerCacheAdapter(CacheAdapter firstLevel, CacheAdapter secondLevel) {
    LinkedList<CacheAdapter> list = new LinkedList<CacheAdapter>();
    if (firstLevel != null) {
      list.addLast(firstLevel);
    }
    if (secondLevel != null) {
      list.addLast(secondLevel);
    }
    return new MultiLayerCacheAdapter(list);
  }

  private void assertGetEquals(String cacheKey, CachePersistencyOptions cacheOptions, String expectedEntry) {
    Observable<String> observableResult = cacheAdapter.get(cacheKey, cacheOptions);
    assertEquals(expectedEntry, observableResult.toBlocking().single());
  }

  private void assertGetEmpty(String cacheKey, CachePersistencyOptions cacheOptions) {
    Observable<String> observableResult = cacheAdapter.get(cacheKey, cacheOptions);
    assertTrue(observableResult.isEmpty().toBlocking().single());
  }


}
