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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

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
    cacheAdapter = new MultiLayerCacheAdapter(persistentCacheAdapter, nonpersistentCacheAdapter);
    options = new CachePersistencyOptions(100, 10, true);
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
    verify(persistentCacheAdapter, times(0)).put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", null);
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
  public void testGetFirstPersistent() {
    when(persistentCacheAdapter.get("key", options)).thenReturn(Observable.just("entry"));
    when(nonpersistentCacheAdapter.get("key", options)).thenReturn(Observable.just(null));
    Observable<String> observableResult = cacheAdapter.get("key", options);
    assertEquals("entry", observableResult.toBlocking().single());
    verify(persistentCacheAdapter, times(1)).get("key", options);
    verify(nonpersistentCacheAdapter, times(1)).get("key", options);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", options);
    verify(nonpersistentCacheAdapter, times(1)).put("key", "entry", options);
  }

  @Test
  public void testGetFirstPersistentNoOptions() {
    when(persistentCacheAdapter.get("key", null)).thenReturn(Observable.just("entry"));
    when(nonpersistentCacheAdapter.get("key", null)).thenReturn(Observable.just(null));
    Observable<String> observableResult = cacheAdapter.get("key", null);
    assertNull(observableResult.toBlocking().single());
    verify(persistentCacheAdapter, times(0)).get("key", null);
    verify(nonpersistentCacheAdapter, times(1)).get("key", null);
    verify(persistentCacheAdapter, times(0)).put("key", "entry", null);
    verify(nonpersistentCacheAdapter, times(0)).put("key", "entry", null);
  }


}
