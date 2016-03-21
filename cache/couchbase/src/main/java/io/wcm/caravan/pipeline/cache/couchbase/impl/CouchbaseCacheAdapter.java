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
package io.wcm.caravan.pipeline.cache.couchbase.impl;

import io.wcm.caravan.commons.metrics.rx.HitsAndMissesCountingMetricsOperator;
import io.wcm.caravan.commons.metrics.rx.TimerMetricsOperator;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.nosql.couchbase.client.CouchbaseClient;
import org.apache.sling.nosql.couchbase.client.CouchbaseKey;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.RawJsonDocument;

/**
 * {@link CacheAdapter} implementation for Couchbase.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Couchbase",
description = "Configure pipeline caching in couchbase.")
@Service(CacheAdapter.class)
public class CouchbaseCacheAdapter implements CacheAdapter {

  private static final String COUCHBASE_CLIENT_ID = "caravan-pipeline-cacheadapter-couchbase";

  @Property(label = "Service Ranking",
      description = "Used to determine the of caching layers if you are using multiple Cache Adapters. "
          + "Fast system-internal caches should have a lower service than slower network caches, so that they are queried first.",
          intValue = CouchbaseCacheAdapter.DEFAULT_RANKING,
          propertyPrivate = false)
  static final String PROPERTY_RANKING = Constants.SERVICE_RANKING;
  static final int DEFAULT_RANKING = 2000;

  @Property(label = "Cache Key Prefix", description = "Prefix for caching keys.",
      value = CouchbaseCacheAdapter.CACHE_KEY_PREFIX_DEFAULT)
  static final String CACHE_KEY_PREFIX_PROPERTY = "cacheKeyPrefix";
  private static final String CACHE_KEY_PREFIX_DEFAULT = "json-pipeline:";

  @Property(label = "Cache Timeout", description = "Timeout in ms for Coucbase cache operations.",
      intValue = CouchbaseCacheAdapter.CACHE_TIMEOUT_DEFAULT)
  static final String CACHE_TIMEOUT_PROPERTY = "cacheTimeout";
  private static final int CACHE_TIMEOUT_DEFAULT = 1000;

  @Property(label = "Cache Writable",
      description = "Determines if this system should be allowed to write into the couchbase cache. If disabled it will only be able to read existing entries.",
      boolValue = CouchbaseCacheAdapter.CACHE_WRITABLE_DEFAULT)
  static final String CACHE_WRITABLE_PROPERTY = "cacheWritable";
  private static final boolean CACHE_WRITABLE_DEFAULT = true;

  @Property(label = "Cache Isolated",
      description = "If enabled, this system's hostname will be appended to all cache keys. " +
          "This can be used in development or test environments to share the same couchbase bucket without actually sharing any cached data",
          boolValue = CouchbaseCacheAdapter.CACHE_ISOLATED_DEFAULT)
  static final String CACHE_ISOLATED_PROPERTY = "cacheIsolated";
  private static final boolean CACHE_ISOLATED_DEFAULT = false;

  @Property(label = "Enabled",
      description = "Enables or disables the whole cache adapter and all operations.",
      boolValue = CouchbaseCacheAdapter.CACHE_ENABLED_DEFAULT)
  static final String CACHE_ENABLED_PROPERTY = "enabled";
  private static final boolean CACHE_ENABLED_DEFAULT = true;


  private static final Logger log = LoggerFactory.getLogger(CouchbaseCacheAdapter.class);

  @Reference(target = "(" + CouchbaseClient.CLIENT_ID_PROPERTY + "=" + COUCHBASE_CLIENT_ID + ")")
  private CouchbaseClient couchbaseClient;

  @Reference
  private MetricRegistry metricRegistry;
  private Timer getLatencyTimer;
  private Timer putLatencyTimer;
  private Counter hitsCounter;
  private Counter missesCounter;

  @Reference
  private HealthCheckRegistry healthCheckRegistry;

  private String keyPrefix;
  private int timeout;
  private boolean writable;
  private boolean isolated;
  private boolean enabled;

  private long time = System.currentTimeMillis();

  @Activate
  private void activate(ComponentContext componentContext, Map<String, Object> config) {
    keyPrefix = PropertiesUtil.toString(config.get(CACHE_KEY_PREFIX_PROPERTY), CACHE_KEY_PREFIX_DEFAULT);
    timeout = PropertiesUtil.toInteger(config.get(CACHE_TIMEOUT_PROPERTY), CACHE_TIMEOUT_DEFAULT);
    writable = PropertiesUtil.toBoolean(config.get(CACHE_WRITABLE_PROPERTY), CACHE_WRITABLE_DEFAULT);
    isolated = PropertiesUtil.toBoolean(config.get(CACHE_ISOLATED_PROPERTY), CACHE_ISOLATED_DEFAULT);
    enabled = PropertiesUtil.toBoolean(config.get(CACHE_ENABLED_PROPERTY), CACHE_ENABLED_DEFAULT);

    getLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "get"));
    putLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "put"));
    hitsCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "hits"));
    missesCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "misses"));

    healthCheckRegistry.register(MetricRegistry.name(getClass()), new HealthCheck() {

      @Override
      protected Result check() throws Exception {
        return couchbaseClient != null && couchbaseClient.isEnabled() ? Result.healthy() : Result.unhealthy("No cache bucket");
      }
    });

    if (isolated) {
      log.info("================================================================================================================");
      log.info("= Add suffix '" + time + "' to cache, so that all cached responses will not be used again.");
      log.info("================================================================================================================");
    }
  }

  @Deactivate
  private void deactivate(ComponentContext componentContext) {
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "get"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "put"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "hits"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "misses"));
    healthCheckRegistry.unregister(MetricRegistry.name(getClass()));
  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {
    if (!enabled || options == null) {
      return Observable.empty();
    }

    if (!couchbaseClient.isEnabled()) {
      log.warn("Couchbase client '{}' is disabled, please check the configuration.", COUCHBASE_CLIENT_ID);
      return Observable.empty();
    }

    AsyncBucket bucket = couchbaseClient.getAsyncBucket();
    if (bucket == null) {
      log.error("Failed to obtain couchase bucket from " + couchbaseClient.getBucketName() + ", couchbase client " + couchbaseClient.getClientId());
      return Observable.empty();
    }

    Observable<RawJsonDocument> fromCache;
    if (options.isExtendStorageTimeOnGet()) {
      fromCache = bucket.getAndTouch(getCacheKey(cacheKey), options.getStorageTime(), RawJsonDocument.class);
    }
    else {
      fromCache = bucket.get(getCacheKey(cacheKey), RawJsonDocument.class);
    }

    return fromCache
        .timeout(timeout, TimeUnit.MILLISECONDS, Observable.create(f -> {
          log.warn("Timeout accessing Couchbase cache");
          f.onCompleted();
        }))
        .lift(new TimerMetricsOperator<RawJsonDocument>(getLatencyTimer))
        .lift(new HitsAndMissesCountingMetricsOperator<RawJsonDocument>(hitsCounter, missesCounter))
        .map(doc -> {
          String content = doc.content();
          log.trace("Succesfully retrieved document with id {}: {}", doc.id(), doc.content());
          return content;
        });
  }

  @Override
  public void put(String cacheKey, String jsonString, CachePersistencyOptions options) {
    if (!enabled || !writable || options == null || !options.shouldUsePersistentCaches()) {
      return;
    }

    if (!couchbaseClient.isEnabled()) {
      log.warn("Couchbase client '{}' is disabled, please check the configuration.", COUCHBASE_CLIENT_ID);
      return;
    }

    AsyncBucket bucket = couchbaseClient.getAsyncBucket();
    if (bucket == null) {
      log.error("Failed to obtain couchase bucket from " + couchbaseClient.getBucketName() + ", couchbase client " + couchbaseClient.getClientId());
      return;
    }

    RawJsonDocument doc = RawJsonDocument.create(getCacheKey(cacheKey), options.getStorageTime(), jsonString);
    Observable<RawJsonDocument> insertionObservable = bucket.upsert(doc);

    insertionObservable
    .timeout(timeout, TimeUnit.MILLISECONDS, Observable.create(f -> {
      log.warn("Timeout writing into Couchbase cache");
      f.onCompleted();
    }))
    .lift(new TimerMetricsOperator<RawJsonDocument>(putLatencyTimer))
    .subscribe(new Observer<RawJsonDocument>() {

      @Override
      public void onNext(RawJsonDocument insertedDoc) {
        log.trace("Succesfully put into Couchbase cache document with id {}:\n{}", insertedDoc.id(), insertedDoc.content());
      }

      @Override
      public void onCompleted() {
        // nothing
      }

      @Override
      public void onError(Throwable e) {
        log.error("Failed to put document " + getCacheKey(cacheKey) + " into the Couchbase cache", e);
      }

    });
  }

  private String getCacheKey(String cacheKey) {

    String fullCacheKey = cacheKey;

    if (isolated) {
      try {
        String hostName = InetAddress.getLocalHost().getHostName();
        fullCacheKey += "_" + hostName+ "__" + time;
      }
      catch (UnknownHostException ex) {
        log.error("Failed to obtain this system's own host name to append it to the cache key", ex);
        isolated = false;
      }
    }

    return CouchbaseKey.build(fullCacheKey, keyPrefix);
  }

}
