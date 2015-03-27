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

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

/**
 *
 */
public class MultiLayerCacheAdapter implements CacheAdapter {

  private List<CacheAdapter> cacheAdapters;

  /**
   * @param cacheAdapters
   */
  public MultiLayerCacheAdapter(List<CacheAdapter> cacheAdapters) {
    this.cacheAdapters = new ArrayList<CacheAdapter>(cacheAdapters);

  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    String cacheKey = null;

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
    return Observable.create(subscriber -> {
      Observable<String> result = Observable.empty();
      CacheAdapter actualCacheAdapter = null;
      for (CacheAdapter cacheAdapter : cacheAdapters) {
        result = cacheAdapter.get(cacheKey, options).cache();
        if (!result.isEmpty().toBlocking().first()) {
          actualCacheAdapter = cacheAdapter;
          break;
        }
      }
      if (actualCacheAdapter != null) {
        String cachedValue = getCacheEntry(result);
        if (cachedValue != null) {
          subscriber.onNext(cachedValue);
          put(cacheKey, cachedValue, options, actualCacheAdapter);
        }
      }
      subscriber.onCompleted();
    });
  }

  private String getCacheEntry(Observable<String> observable) {
    return observable.count().toBlocking().single() > 0 ? observable.toBlocking().first() : null;
  }

  /*
   * Puts cache entry into the caches, which are specified to be earlier than actual CacheAdapter. It is expected, the
   * actual CacheAdapter and later caches already have stored entry.
   */
  private void put(String cacheKey, String jsonString, CachePersistencyOptions options, CacheAdapter actualCacheAdapter) {
    for (CacheAdapter cacheAdapter : cacheAdapters) {
      if (cacheAdapter != actualCacheAdapter) {
        cacheAdapter.put(cacheKey, jsonString, options);
      }
      else {
        break;
      }
    }
  }

  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    for (CacheAdapter cacheAdapter : cacheAdapters) {
      cacheAdapter.put(cacheKey, jsonString, options);
    }
  }

}
