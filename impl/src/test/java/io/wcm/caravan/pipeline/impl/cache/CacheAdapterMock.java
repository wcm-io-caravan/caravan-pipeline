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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;

import rx.Observable;

@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Mock",
description = "Configure pipeline caching in Mock.")
@Service(CacheAdapter.class)
public class CacheAdapterMock implements CacheAdapter {

  @Property(label = "Service Ranking", intValue = CacheAdapterMock.DEFAULT_RANKING,
      description = "Priority of parameter persistence providers (higher value = higher priority)",
      propertyPrivate = false)
  static final String PROPERTY_RANKING = Constants.SERVICE_RANKING;
  static final int DEFAULT_RANKING = 2000;

  private Map<String, String> map = new ConcurrentHashMap<String, String>();

  private String name;

  public CacheAdapterMock(String name) {
    this.name = name;
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    return servicePrefix + descriptor;
  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {
    return map.containsKey(cacheKey) ? Observable.just(map.get(cacheKey)) : Observable.empty();
  }

  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    map.put(cacheKey, jsonString);
  }

  @Override
  public String toString() {
    return "CacheAdapterMock [name=" + this.name + "]";
  }


}
