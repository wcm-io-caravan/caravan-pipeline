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

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;

/**
 * {@link CacheAdapter} implementation for Guava.
 * Provides guava {@link Cache}, which size is specified in bytes. Default cache size is 10 MB. Provide higher property
 * value {@value #CACHE_MAXIMUM_WEIGHT_IN_BYTES} to set up higher cache capacity.
 * items life time depends on the amount and size of stored cache items. Items, which capacity is higher than 1/4 of the
 * declared cache size will not be stored.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Guava",
description = "Configure pipeline caching in guava.")
@Service(CacheAdapter.class)
public class GuavaCacheAdapter implements CacheAdapter {

  private static final Logger log = LoggerFactory.getLogger(GuavaCacheAdapter.class);

  @Property(label = "Service Ranking", intValue = GuavaCacheAdapter.DEFAULT_RANKING,
      description = "Priority of parameter persistence providers (lower = higher priority)",
      propertyPrivate = false)
  static final String PROPERTY_RANKING = Constants.SERVICE_RANKING;
  static final int DEFAULT_RANKING = 2000;

  @Property(label = "Cache Maximum Weight In Bytes",
      description = "Declares the weight of the cache. Each cache entry could not be larger than 1/4 of the declared cache weight")
  static final String CACHE_MAXIMUM_WEIGHT_IN_BYTES = "cacheMaximumWeightInBytes";
  private static final Long CACHE_DEFAULT_WEIGHT_IN_BYTES = 1058816L; // 10 MB

  private Cache<String, String> guavaCache;
  private long cacheWeight;

  @Reference
  private MetricRegistry metricRegistry;
  private Timer getLatencyTimer;
  private Timer putLatencyTimer;
  private Counter hitsCounter;
  private Counter missesCounter;

  @Activate
  void activate(Map<String, Object> config) {
    cacheWeight = PropertiesUtil.toLong(config.get(CACHE_MAXIMUM_WEIGHT_IN_BYTES), CACHE_DEFAULT_WEIGHT_IN_BYTES);
    this.guavaCache = CacheBuilder.newBuilder().weigher(new Weigher<String, String>() {
      @Override
      public int weigh(String key, String value) {
        return 8 * ((((value.length()) * 2) + 45) / 8);
      }
    }).maximumWeight(cacheWeight).build();

    getLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "get"));
    putLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "put"));
    hitsCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "hits"));
    missesCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "misses"));

  }

  @Deactivate
  void deactivate() {
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "get"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "put"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "hits"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "misses"));
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    return servicePrefix + descriptor;

  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {

    return Observable.create(subscriber -> {
      Timer.Context context = getLatencyTimer.time();
      String cacheEntry = guavaCache.getIfPresent(cacheKey);
      if (cacheEntry != null) {
        hitsCounter.inc();
        subscriber.onNext(cacheEntry);
      }
      else {
        missesCounter.inc();
      }
      context.stop();
      log.trace("Succesfully retrieved document with id {}: {}", cacheKey, cacheEntry);
      subscriber.onCompleted();
    });

  }

  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    Timer.Context context = putLatencyTimer.time();
    guavaCache.put(cacheKey, jsonString);
    context.stop();
    log.trace("Document {} has been succesfully put into the Couchbase cache:\n {}", cacheKey, jsonString);

  }

}
