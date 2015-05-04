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
package io.wcm.caravan.pipeline.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.cache.MultiLayerCacheAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;

import rx.Observable;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineFactoryImplTest {

  /**
   * The OSGI context.
   */
  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private CacheAdapter secondLevelCacheAdapter;

  @Mock
  private CacheAdapter firstLevelCacheAdapter;

  @Mock
  private CaravanHttpClient caravanHttpClient;

  private JsonPipelineFactoryImpl factory;

  private CaravanHttpRequest request;

  @Before
  public void setup() {
    context.registerService(MetricRegistry.class, new MetricRegistry());
    context.registerService(CaravanHttpClient.class, caravanHttpClient);

    secondLevelCacheAdapter = context.registerService(CacheAdapter.class, secondLevelCacheAdapter,
        ImmutableMap.of(Constants.SERVICE_RANKING, 5000));
    firstLevelCacheAdapter = context.registerService(CacheAdapter.class, firstLevelCacheAdapter,
        ImmutableMap.of(Constants.SERVICE_RANKING, 2000));

    factory = context.registerInjectActivateService(new JsonPipelineFactoryImpl());
  }

  @Test
  public void testCreateEmpty() throws Exception {
    JsonPipeline pipeline = factory.createEmpty();
    JsonPipelineOutput output = pipeline.getOutput().toBlocking().first();
    assertEquals(31536000L, output.getMaxAge());
  }

  @Test
  public void testCreateSpecifiedRequest() throws Exception {
    request = new CaravanHttpRequestBuilder("service").append("/path").build();
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.of("Cache-Control", "max-age=" + Long.toString(TimeUnit.DAYS.toSeconds(1)));
    when(caravanHttpClient.execute(request)).thenReturn(
        Observable.just(new CaravanHttpResponseBuilder()
        .status(HttpStatus.SC_OK)
        .reason("Content")
        .headers(headers)
        .body(new byte[0])
        .build()));

    JsonPipeline pipeline = factory.create(request);
    JsonPipelineOutput output = pipeline.getOutput().toBlocking().first();
    assertEquals(86400, output.getMaxAge());
  }

  @Test
  public void testCreateMultiLayerCacheAdapter() throws Exception {
    MultiLayerCacheAdapter cacheAdapter = factory.createMultiLayerCacheAdapter();
    assertNotNull(cacheAdapter);

    // expected two levels of cache adapters via OSGI injection
    assertEquals(2, cacheAdapter.cachingLevels());

    List<CacheAdapter> cacheAdapters = cacheAdapter.getCacheAdapters();
    // expected firstLevelCacheAdapter at place 0 and secondLevelCacheAdapter at place 1
    // according to the ranking order 5000 of firstLevelCacheAdapter (higher priority)
    // and 2000 ranking order of secondLevelCacheAdapter (lower priority)
    assertEquals(firstLevelCacheAdapter, cacheAdapters.get(0));
    assertEquals(secondLevelCacheAdapter, cacheAdapters.get(1));
  }

  @Test
  public void testRemoveOneCacheAtMultiLayerCacheAdapter() throws Exception {
    MultiLayerCacheAdapter cacheAdapter = factory.createMultiLayerCacheAdapter();
    assertNotNull(cacheAdapter);

    // expected two levels of cache adapters via OSGI injection
    assertEquals(2, cacheAdapter.cachingLevels());
  }



}
