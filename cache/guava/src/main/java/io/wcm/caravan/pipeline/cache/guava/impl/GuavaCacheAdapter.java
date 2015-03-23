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

import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import rx.Observable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * {@link CacheAdapter} implementation for Guava.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Guava",
description = "Configure pipeline caching in guava.")
@Service(CacheAdapter.class)
public class GuavaCacheAdapter implements CacheAdapter {

  private Cache<String, String> guavaCache;

  /**
   * Default constructor
   */
  public GuavaCacheAdapter() {
    this.guavaCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build();
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    return servicePrefix + descriptor;

  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {

    Observable<String> entryObservable = Observable.create(subscriber -> {
      subscriber.onNext(guavaCache.getIfPresent(cacheKey));
      subscriber.onCompleted();
    });

    return entryObservable;
  }

  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    guavaCache.put(cacheKey, jsonString);
  }

}
