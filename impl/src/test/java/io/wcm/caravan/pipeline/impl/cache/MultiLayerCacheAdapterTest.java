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
import static org.junit.Assert.assertNull;
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
  private CacheAdapter persistentCacheAdapter;
  @Mock
  private CacheAdapter nonpersistentCacheAdapter;
  private CacheAdapter cacheAdapter;
  private CachePersistencyOptions options;

  @Before
  public void setup() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, nonpersistentCacheAdapter);
    options = CachePersistencyOptions.createPersistentAndTimeToIdle(100, 10);
  }

  @Test
  public void testGetCacheKeyShortestFromPersistent() {
    when(persistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("key");
    when(nonpersistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertEquals("key", cacheKey);
    verify(persistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
    verify(nonpersistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyShortestFromNonPersistent() {
    when(persistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    when(nonpersistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("key");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertEquals("key", cacheKey);
    verify(persistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
    verify(nonpersistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyPersistentCacheOnly() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, null);
    when(persistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertEquals("longKey", cacheKey);
    verify(persistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyNonPersistentCacheOnly() {
    cacheAdapter = createMultilayerCacheAdapter(null, nonpersistentCacheAdapter);
    when(nonpersistentCacheAdapter.getCacheKey("servicePrefix", "descriptor")).thenReturn("longKey");
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertEquals("longKey", cacheKey);
    verify(nonpersistentCacheAdapter, times(1)).getCacheKey("servicePrefix", "descriptor");
  }

  @Test
  public void testGetCacheKeyNotInitialized() {
    cacheAdapter = createMultilayerCacheAdapter(null, null);
    String cacheKey = cacheAdapter.getCacheKey("servicePrefix", "descriptor");
    assertNull(cacheKey);
  }

  @Test
  public void testPut() {
    cacheAdapter.put("key", "entry", options);
    verify(persistentCacheAdapter, times(1)).put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutNullOptions() {
    cacheAdapter.put("key", "entry", null);
    verify(persistentCacheAdapter, times(1)).put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testPutNonPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(null, nonpersistentCacheAdapter);
    cacheAdapter.put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutNonPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(null, nonpersistentCacheAdapter);
    cacheAdapter.put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testPutPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, null);
    cacheAdapter.put("key", "entry", options);
    verify(persistentCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testPutPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, null);
    cacheAdapter.put("key", "entry", null);
    verify(persistentCacheAdapter, times(1)).put("key", "entry", null);
  }

  @Test
  public void testPutNotInitialized() {
    cacheAdapter = createMultilayerCacheAdapter(null, null);
    cacheAdapter.put("key", "entry", null);
    cacheAdapter.put("key", "entry", options);
    // ANY EXCEPTIONAL BEHAVIOUR?
  }

  @Test
  public void testGetNonPersistent() {
    when(nonpersistentCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(persistentCacheAdapter, times(0)).get("key", options);
    verify(nonpersistentCacheAdapter, times(1)).get("key", options);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetNonPersistentNoOptions() {
    when(nonpersistentCacheAdapter.get("key", null)).thenReturn(Observable.just("entry"));
    Observable<String> observableResult = cacheAdapter.get("key", null);

    assertEquals("entry", observableResult.toBlocking().single());

    verify(persistentCacheAdapter, times(0)).get("key", null);
    verify(nonpersistentCacheAdapter, times(1)).get("key", null);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetPersistent() {
    when(persistentCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    when(nonpersistentCacheAdapter.get("key", options)).thenReturn(Observable.empty());
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(persistentCacheAdapter, times(1)).get("key", options);
    verify(nonpersistentCacheAdapter, times(1)).get("key", options);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testGetPersistentNoOptions() {
    when(persistentCacheAdapter.get("key", null)).thenReturn(Observable.empty());
    when(nonpersistentCacheAdapter.get("key", null)).thenReturn(Observable.empty());
    Observable<String> observableResult = cacheAdapter.get("key", null);
    assertTrue(observableResult.isEmpty().toBlocking().single());
    verify(persistentCacheAdapter, times(1)).get("key", null);
    verify(nonpersistentCacheAdapter, times(1)).get("key", null);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, null);
    when(persistentCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(persistentCacheAdapter, times(1)).get("key", options);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(persistentCacheAdapter, null);
    when(persistentCacheAdapter.get("key", null)).thenReturn(Observable.empty());
    Observable<String> observableResult = cacheAdapter.get("key", null);
    assertTrue(observableResult.isEmpty().toBlocking().single());
    verify(persistentCacheAdapter, times(1)).get("key", null);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetNonPersistentOnly() {
    cacheAdapter = createMultilayerCacheAdapter(null, nonpersistentCacheAdapter);
    when(nonpersistentCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(nonpersistentCacheAdapter, times(1)).get("key", options);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", options);
  }

  @Test
  public void testGetNonPersistentOnlyNoOptions() {
    cacheAdapter = createMultilayerCacheAdapter(null, nonpersistentCacheAdapter);
    when(nonpersistentCacheAdapter.get("key", null)).thenReturn(Observable.just("entry"));
    Observable<String> observableResult = cacheAdapter.get("key", null);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(nonpersistentCacheAdapter, times(1)).get("key", null);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", null);
  }

  @Test
  public void testGetNotInitialized() {
    cacheAdapter = createMultilayerCacheAdapter(null, null);
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertTrue(observableResult.isEmpty().toBlocking().single());
    verify(persistentCacheAdapter, times(0)).get("key", options);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(0)).get("key", options);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", options);
  }

  public MultiLayerCacheAdapter createMultilayerCacheAdapter(CacheAdapter persistent, CacheAdapter nonPersistent) {
    LinkedList<CacheAdapter> list = new LinkedList<CacheAdapter>();
    if (nonPersistent != null) {
      list.addLast(nonPersistent);
    }
    if (persistent != null) {
      list.addLast(persistent);
    }
    return new MultiLayerCacheAdapter(list);
  }


}
