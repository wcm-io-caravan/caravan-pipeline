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

import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.util.LinkedList;
import java.util.List;

import rx.Observable;

/**
 *
 */
public class MultiLayerCacheAdapter implements CacheAdapter {

  private List<CacheAdapter> persistentCacheAdapters;

  private List<CacheAdapter> nonPersistentCacheAdapters;

  /**
   * @param persistentCacheAdapters
   * @param nonPersistentCacheAdapters
   */
  public MultiLayerCacheAdapter(List<CacheAdapter> persistentCacheAdapters, List<CacheAdapter> nonPersistentCacheAdapters) {
    this.persistentCacheAdapters = new LinkedList<CacheAdapter>(persistentCacheAdapters);
    this.nonPersistentCacheAdapters = new LinkedList<CacheAdapter>(nonPersistentCacheAdapters);
  }

  /**
   * @param persistentCacheAdapter
   * @param nonPersistentCacheAdapter
   */
  public MultiLayerCacheAdapter(CacheAdapter persistentCacheAdapter, CacheAdapter nonPersistentCacheAdapter) {
    persistentCacheAdapters = new LinkedList<CacheAdapter>();
    persistentCacheAdapters.add(persistentCacheAdapter);
    nonPersistentCacheAdapters = new LinkedList<CacheAdapter>();
    nonPersistentCacheAdapters.add(nonPersistentCacheAdapter);
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    String cacheKey = null;

    LinkedList<CacheAdapter> cacheAdapters = new LinkedList<CacheAdapter>();
    cacheAdapters.addAll(persistentCacheAdapters);
    cacheAdapters.addAll(nonPersistentCacheAdapters);

    for (CacheAdapter cacheAdapter : cacheAdapters) {
      String nextCacheKey = cacheAdapter.getCacheKey(servicePrefix, descriptor);
      cacheKey = chooseTheShortestCacheKey(cacheKey, nextCacheKey);
    }

    return cacheKey;
  }

  private String chooseTheShortestCacheKey(String actualCacheKey, String nextCacheKey) {
    return actualCacheKey == null ? nextCacheKey :
      (actualCacheKey.length() > nextCacheKey.length() ? nextCacheKey : actualCacheKey);
  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {
    Observable<String> result = get(cacheKey, options, nonPersistentCacheAdapters);

    if (result.toBlocking().single() == null && options != null) {
      result = get(cacheKey, options, persistentCacheAdapters);
      put(cacheKey, result.toBlocking().single(), options, nonPersistentCacheAdapters);
    }

    return result;
  }

  private Observable<String> get(String cacheKey, CachePersistencyOptions options, List<CacheAdapter> cacheAdapters) {
    Observable<String> result = null;

    for (CacheAdapter cacheAdapter : cacheAdapters) {
      result = cacheAdapter.get(cacheKey, options);
      if (result != null) {
        break;
      }
    }

    return result;
  }


  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    LinkedList<CacheAdapter> cacheAdapters = new LinkedList<CacheAdapter>();

    cacheAdapters.addAll(nonPersistentCacheAdapters);
    if (options != null) {
      cacheAdapters.addAll(persistentCacheAdapters);
    }

    put(cacheKey, jsonString, options, cacheAdapters);
  }

  private void put(String cacheKey, String jsonString, CachePersistencyOptions options, List<CacheAdapter> cacheAdapters) {
    for (CacheAdapter cacheAdapter : cacheAdapters) {
      cacheAdapter.put(cacheKey, jsonString, options);
    }
  }

}
