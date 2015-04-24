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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;
import io.wcm.caravan.commons.couchbase.CouchbaseClientProvider;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.RawJsonDocument;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class CouchbaseCacheAdapterTest {

  private static final String JSON_DOC = "{\"key\":\"value\"}";

  private static final String NO_PREFIX_CACHE_KEY = "example:/a/b/c";

  private static final String CACHE_KEY = "prefix:" + NO_PREFIX_CACHE_KEY;

  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private AsyncBucket bucket;
  @Mock
  private CouchbaseClientProvider couchbaseClientProvider;

  private CachePersistencyOptions cachePersistencyOptions;

  private MetricRegistry metricRegistry;

  private HealthCheckRegistry healthCheckRegistry;

  private CouchbaseCacheAdapter adapter;

  @Before
  public void setUp() {
    when(couchbaseClientProvider.getCacheBucket()).thenReturn(bucket);
    context.registerService(CouchbaseClientProvider.class, couchbaseClientProvider);
    metricRegistry = new MetricRegistry();
    context.registerService(MetricRegistry.class, metricRegistry);
    healthCheckRegistry = new HealthCheckRegistry();
    context.registerService(HealthCheckRegistry.class, healthCheckRegistry);
    adapter = context.registerInjectActivateService(new CouchbaseCacheAdapter(), ImmutableMap.<String, Object>builder()
        .put(CouchbaseCacheAdapter.CACHE_KEY_PREFIX_PROPERTY, "prefix:")
        .put(CouchbaseCacheAdapter.CACHE_TIMEOUT_PROPERTY, 100)
        .build());
    cachePersistencyOptions = CachePersistencyOptions.createPersistentAndTimeToLive(100, 10);
  }

  @Test
  public void getShortCacheKey() {
    String cacheKey = adapter.getCacheKey(NO_PREFIX_CACHE_KEY);
    assertEquals(CACHE_KEY, cacheKey);
  }

  @Test
  public void getLongCacheKey() {
    String cacheKey = adapter.getCacheKey("example:" + StringUtils.repeat("/a/b/c", 500));
    assertEquals(250, cacheKey.length());
    assertTrue("beginning of cache key is left untouched?", cacheKey.startsWith(CACHE_KEY));
  }

  @Test
  public void testGet_hit() throws Exception {

    Mockito.when(bucket.get(CACHE_KEY, RawJsonDocument.class))
    .thenReturn(Observable.just(RawJsonDocument.create("1", JSON_DOC)).delay(50, TimeUnit.MILLISECONDS));

    Observable<String> observable = adapter.get(NO_PREFIX_CACHE_KEY, cachePersistencyOptions);

    assertEquals(0, getHitCounter().getCount());
    assertEquals(0, getMissesCounter().getCount());
    assertEquals(0, getGetLatencyTimer().getCount());
    assertTrue(getGetLatencyTimer().getMeanRate() == 0);

    assertEquals(JSON_DOC, observable.toBlocking().single());

    assertEquals(1, getHitCounter().getCount());
    assertEquals(0, getMissesCounter().getCount());
    assertEquals(1, getGetLatencyTimer().getCount());
    assertTrue(getGetLatencyTimer().getSnapshot().getMean() / 1000000 >= 50);
  }

  private Counter getHitCounter() {
    return metricRegistry.getCounters().get(MetricRegistry.name(CouchbaseCacheAdapter.class, "hits"));
  }

  private Counter getMissesCounter() {
    return metricRegistry.getCounters().get(MetricRegistry.name(CouchbaseCacheAdapter.class, "misses"));
  }

  private Timer getGetLatencyTimer() {
    return metricRegistry.getTimers().get(MetricRegistry.name(CouchbaseCacheAdapter.class, "latency", "get"));
  }

  @Test
  public void testGet_miss() throws Exception {

    when(bucket.get(CACHE_KEY, RawJsonDocument.class))
    .thenReturn(Observable.<RawJsonDocument>empty().delay(50, TimeUnit.MILLISECONDS));

    Observable<String> observable = adapter.get(NO_PREFIX_CACHE_KEY, cachePersistencyOptions);

    assertEquals(0, getHitCounter().getCount());
    assertEquals(0, getMissesCounter().getCount());
    assertTrue(getGetLatencyTimer().getMeanRate() == 0);

    assertNull(observable.toBlocking().singleOrDefault(null));

    assertEquals(0, getHitCounter().getCount());
    assertEquals(1, getMissesCounter().getCount());
    assertEquals(1, getGetLatencyTimer().getCount());
    assertTrue(getGetLatencyTimer().getSnapshot().getMean() / 1000000 >= 50);
  }

  @Test
  public void testGetNoOptions() throws Exception {
    Observable<String> observable = adapter.get(CACHE_KEY, null);

    // Checks that the get method returns empty Observable after input parameters validation.
    // Get operations with absent options should be ignored.
    assertTrue(observable.isEmpty().toBlocking().first());
  }

  @Test
  public void testPut() throws Exception {
    when(bucket.upsert(Matchers.any(RawJsonDocument.class)))
    .thenReturn(just(RawJsonDocument.create("test-id", JSON_DOC)).delay(50, TimeUnit.MILLISECONDS));
    adapter.put(CACHE_KEY, JSON_DOC, cachePersistencyOptions);

    // we currently have no proper way to detecting that the put was completed, so we wait (up to one second)
    // until the put-latency timer has been stopped
    int millisWaited = 0;
    while (getPutLatencyTimer().getCount() == 0 && millisWaited < 1000) {
      Thread.sleep(1);
      millisWaited++;
    }

    assertEquals(1, getPutLatencyTimer().getCount());
    assertTrue(getPutLatencyTimer().getSnapshot().getMean() / 1000000 >= 50);
    assertTrue(getPutLatencyTimer().getSnapshot().getMean() / 1000000 <= 100);
  }

  private Timer getPutLatencyTimer() {
    return metricRegistry.getTimers().get(MetricRegistry.name(CouchbaseCacheAdapter.class, "latency", "put"));
  }

  @Test
  public void testGet_timeout() {
    when(bucket.get(CACHE_KEY, RawJsonDocument.class))
    .thenReturn(Observable.just(RawJsonDocument.create("1", JSON_DOC)).delay(500, TimeUnit.MILLISECONDS));
    String output = adapter.get(NO_PREFIX_CACHE_KEY, cachePersistencyOptions).toBlocking().singleOrDefault(null);
    assertNull(output);
    assertEquals(0, getHitCounter().getCount());
    assertEquals(1, getMissesCounter().getCount());
    assertEquals(1, getGetLatencyTimer().getCount());
    assertTrue(getGetLatencyTimer().getSnapshot().getMean() / 1000000 >= 100);
  }

  @Test
  public void testPut_timeout() throws InterruptedException {
    when(bucket.upsert(Matchers.any(RawJsonDocument.class)))
    .thenReturn(Observable.just(RawJsonDocument.create("test-id", JSON_DOC)).delay(500, TimeUnit.MILLISECONDS));
    adapter.put(CACHE_KEY, JSON_DOC, cachePersistencyOptions);

    // we currently have no proper way to detecting that the put was completed, so we wait (up to one second)
    // until the put-latency timer has been stopped
    int millisWaited = 0;
    while (getPutLatencyTimer().getCount() == 0 && millisWaited < 1000) {
      Thread.sleep(1);
      millisWaited++;
    }

    assertEquals(1, getPutLatencyTimer().getCount());
    assertTrue(getPutLatencyTimer().getSnapshot().getMean() / 1000000 >= 100);
  }


  @Test
  public void testPutTimeToLive() throws Exception {

    when(bucket.upsert(Matchers.any(RawJsonDocument.class)))
    .thenReturn(Observable.just(RawJsonDocument.create("test-id", JSON_DOC)));

    CachePersistencyOptions options = CachePersistencyOptions.createPersistentAndTimeToLive(500, 1000);
    adapter.put(CACHE_KEY, JSON_DOC, options);

    // make sure the storage time of the CachePersistenceOptions is used for the expiry value
    ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
    verify(bucket).upsert(doc.capture());
    assertEquals(options.getStorageTime(), doc.getValue().expiry());
  }

  @Test
  public void testPutTimeToIdle() throws Exception {

    when(bucket.upsert(Matchers.any(RawJsonDocument.class)))
    .thenReturn(Observable.just(RawJsonDocument.create("test-id", JSON_DOC)));

    CachePersistencyOptions options = CachePersistencyOptions.createPersistentAndTimeToIdle(500, 1000);
    adapter.put(CACHE_KEY, JSON_DOC, options);

    // make sure the storage time of the CachePersistenceOptions is used for the expiry value
    ArgumentCaptor<Document> doc = ArgumentCaptor.forClass(Document.class);
    verify(bucket).upsert(doc.capture());
    assertEquals(options.getStorageTime(), doc.getValue().expiry());
  }

  public void testPutNoOptions() throws Exception {
    adapter.put(CACHE_KEY, JSON_DOC, null);

    // Checks that the put method does not invoke next steps after input parameters validation.
    // Put operations with absent options should be ignored.
    verify(couchbaseClientProvider, times(0)).getCacheBucket();
  }

  @Test
  public void testPutNonPersistentOptions() throws Exception {
    adapter.put(CACHE_KEY, JSON_DOC, CachePersistencyOptions.createTransient(100));

    // Checks that the put method does not invoke next steps after input parameters validation.
    // Put operations with transient input data should be ignored.
    verify(couchbaseClientProvider, times(0)).getCacheBucket();
  }

  @Test
  public void testPutIntoNonWritableCache() throws Exception {
    adapter = context.registerInjectActivateService(new CouchbaseCacheAdapter(), ImmutableMap.<String, Object>builder()
        .put(CouchbaseCacheAdapter.CACHE_KEY_PREFIX_PROPERTY, "prefix:")
        .put(CouchbaseCacheAdapter.CACHE_TIMEOUT_PROPERTY, 100)
        .put(CouchbaseCacheAdapter.CACHE_WRITABLE_PROPERTY, false)
        .build());
    adapter.put(CACHE_KEY, JSON_DOC, cachePersistencyOptions);

    // Checks that the put method does not invoke next steps after config parameters validation.
    // Put operations should be ignored for non writable caches (cacheWritable = false).
    verify(couchbaseClientProvider, times(0)).getCacheBucket();
  }

}
