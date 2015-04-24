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

import io.wcm.caravan.commons.couchbase.CouchbaseClientProvider;
import io.wcm.caravan.commons.metrics.rx.HitsAndMissesCountingMetricsOperator;
import io.wcm.caravan.commons.metrics.rx.TimerMetricsOperator;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.CharEncoding;
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

  @Property(label = "Service Ranking", intValue = CouchbaseCacheAdapter.DEFAULT_RANKING,
      description = "Priority of parameter persistence providers (lower value = higher priority)",
      propertyPrivate = false)
  static final String PROPERTY_RANKING = Constants.SERVICE_RANKING;
  static final int DEFAULT_RANKING = 2000;

  @Property(label = "Cache Key Prefix", description = "Prefix for caching keys.")
  static final String CACHE_KEY_PREFIX_PROPERTY = "cacheKeyPrefix";
  private static final String CACHE_KEY_PREFIX_DEFAULT = "json-pipeline:";

  @Property(label = "Cache Timeout", description = "Timeout in ms for Coucbase cache operations.")
  static final String CACHE_TIMEOUT_PROPERTY = "cacheTimeout";
  private static final int CACHE_TIMEOUT_DEFAULT = 1000;

  @Property(label = "Cache Writable",
      description = "True if cache supports write and read operations, false if cache supports only read operations")
  static final String CACHE_WRITABLE_PROPERTY = "cacheWritable";
  private static final boolean CACHE_WRITABLE_DEFAULT = true;

  private static final int MAX_CACHE_KEY_LENGTH = 250;

  private static final Logger log = LoggerFactory.getLogger(CouchbaseCacheAdapter.class);

  @Reference
  private CouchbaseClientProvider couchbaseClientProvider;

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

  @Activate
  private void activate(Map<String, Object> config) {
    keyPrefix = PropertiesUtil.toString(config.get(CACHE_KEY_PREFIX_PROPERTY), CACHE_KEY_PREFIX_DEFAULT);
    timeout = PropertiesUtil.toInteger(config.get(CACHE_TIMEOUT_PROPERTY), CACHE_TIMEOUT_DEFAULT);
    writable = PropertiesUtil.toBoolean(config.get(CACHE_WRITABLE_PROPERTY), CACHE_WRITABLE_DEFAULT);

    getLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "get"));
    putLatencyTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "latency", "put"));
    hitsCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "hits"));
    missesCounter = metricRegistry.counter(MetricRegistry.name(getClass(), "misses"));

    healthCheckRegistry.register(MetricRegistry.name(getClass()), new HealthCheck() {

      @Override
      protected Result check() throws Exception {
        return couchbaseClientProvider != null && couchbaseClientProvider.isEnabled() ? Result.healthy() : Result.unhealthy("No cache bucket");
      }

    });

  }

  @Deactivate
  private void deactivate() {
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "get"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "latency", "put"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "hits"));
    metricRegistry.remove(MetricRegistry.name(getClass(), "misses"));
    healthCheckRegistry.unregister(MetricRegistry.name(getClass()));
  }

  @Override
  public Observable<String> get(String cacheKey, CachePersistencyOptions options) {
    if (options == null) {
      return Observable.empty();
    }

    AsyncBucket bucket = couchbaseClientProvider.getCacheBucket();

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
    if (!writable || options == null || !options.isPersistent()) {
      return;
    }

    AsyncBucket bucket = couchbaseClientProvider.getCacheBucket();

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
        log.trace("Document {} has been succesfully put into the Couchbase cache:\n {}", insertedDoc.id(), insertedDoc.content());
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

  String getCacheKey(String longKey) {
    String cacheKey = keyPrefix + longKey;

    if (cacheKey.length() < MAX_CACHE_KEY_LENGTH) {
      return cacheKey;
    }

    int charactersToKeep = MAX_CACHE_KEY_LENGTH - keyPrefix.length() - 41;

    String toKeep = longKey.substring(0, charactersToKeep);
    String toHash = longKey.substring(charactersToKeep);

    String hash = calculateHash(toHash);

    return keyPrefix + toKeep + "#" + hash;
  }

  private static String calculateHash(String message) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(message.getBytes(CharEncoding.UTF_8));
      byte[] digestBytes = digest.digest();

      return javax.xml.bind.DatatypeConverter.printHexBinary(digestBytes).toLowerCase();
    }
    catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
      throw new RuntimeException("Failed to create sha1 Hash from " + message, ex);
    }
  }

}
