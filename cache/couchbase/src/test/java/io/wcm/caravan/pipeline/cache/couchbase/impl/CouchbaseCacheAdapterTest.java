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
import static org.mockito.Mockito.when;
import io.wcm.caravan.commons.couchbase.CouchbaseClientProvider;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rx.Observable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.RawJsonDocument;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class CouchbaseCacheAdapterTest {

  private static final String JSON_DOC = "{\"key\":\"value\"}";

  private static final String CACHE_KEY = "prefix:example:/a/b/c";

  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private AsyncBucket bucket;
  @Mock
  private CouchbaseClientProvider couchbaseClientProvider;

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
        .build());
  }

  @Test
  public void getShortCacheKey() {
    String cacheKey = adapter.getCacheKey("example", "/a/b/c");
    assertEquals(CACHE_KEY, cacheKey);
  }

  @Test
  public void getLongCacheKey() {
    String cacheKey = adapter.getCacheKey("example", StringUtils.repeat("/a/b/c", 500));
    assertEquals(250, cacheKey.length());
    assertTrue("beginning of cache key is left untouched?", cacheKey.startsWith(CACHE_KEY));
  }

  @Test
  public void testGet_hit() throws Exception {
    Mockito.when(bucket.get(CACHE_KEY, RawJsonDocument.class)).then(new Answer<Observable<RawJsonDocument>>() {

      @Override
      public Observable<RawJsonDocument> answer(final InvocationOnMock invocation) throws InterruptedException {
        return Observable.just(RawJsonDocument.create("1", JSON_DOC)).delay(50, TimeUnit.MILLISECONDS);
      }
    });
    Observable<String> observable = adapter.get(CACHE_KEY, false, 0);

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
    Mockito.when(bucket.get(CACHE_KEY, RawJsonDocument.class)).then(new Answer<Observable<RawJsonDocument>>() {

      @Override
      public Observable<RawJsonDocument> answer(final InvocationOnMock invocation) throws InterruptedException {
        return Observable.<RawJsonDocument>empty().delay(50, TimeUnit.MILLISECONDS);
      }
    });
    Observable<String> observable = adapter.get(CACHE_KEY, false, 0);

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
  public void testPut() throws Exception {
    Mockito.when(bucket.upsert(Matchers.any(RawJsonDocument.class))).then(new Answer<Observable<RawJsonDocument>>() {

      @Override
      public Observable<RawJsonDocument> answer(final InvocationOnMock invocation) throws InterruptedException {
        return Observable.just(RawJsonDocument.create("test-id", JSON_DOC)).delay(50, TimeUnit.MILLISECONDS);
      }
    });
    adapter.put(CACHE_KEY, JSON_DOC, 100);
    Thread.sleep(60);
    assertEquals(1, getPutLatencyTimer().getCount());
    assertTrue(getPutLatencyTimer().getSnapshot().getMean() / 1000000 >= 50);
  }

  private Timer getPutLatencyTimer() {
    return metricRegistry.getTimers().get(MetricRegistry.name(CouchbaseCacheAdapter.class, "latency", "put"));
  }


}
