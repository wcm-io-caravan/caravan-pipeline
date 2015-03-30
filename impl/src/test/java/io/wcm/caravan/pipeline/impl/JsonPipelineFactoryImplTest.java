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
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.cache.CacheAdapterMock;
import io.wcm.caravan.pipeline.impl.cache.MultiLayerCacheAdapter;

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

  private CacheAdapterMock firstLevelCacheAdapter;

  private CacheAdapterMock secondLevelCacheAdapter;

  @Mock
  CaravanHttpClient caravanHttpClient;

  private JsonPipelineFactoryImpl factory;

  private CaravanHttpRequest request;

  @Before
  public void setup() {
    context.registerService(MetricRegistry.class, new MetricRegistry());
    //context.registerService(HealthCheckRegistry.class, new HealthCheckRegistry());
    context.registerService(CaravanHttpClient.class, caravanHttpClient);

    factory = new JsonPipelineFactoryImpl();
    firstLevelCacheAdapter = new CacheAdapterMock("level 1");
    secondLevelCacheAdapter = new CacheAdapterMock("level 2");
    secondLevelCacheAdapter = context.registerInjectActivateService(secondLevelCacheAdapter, ImmutableMap.of(Constants.SERVICE_RANKING, Integer.MIN_VALUE));
    firstLevelCacheAdapter = context.registerInjectActivateService(firstLevelCacheAdapter, ImmutableMap.of(Constants.SERVICE_RANKING, Integer.MAX_VALUE));
    factory = context.registerInjectActivateService(factory);
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
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.of("Cache-Control", "max-age: " + Long.toString(TimeUnit.DAYS.toSeconds(1)));
    when(caravanHttpClient.execute(request)).thenReturn(
        Observable.just(CaravanHttpResponse.create(HttpStatus.SC_OK, "Content", headers, new byte[0])));

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
  }

  @Test
  public void testRemoveOneCacheAtMultiLayerCacheAdapter() throws Exception {
    MultiLayerCacheAdapter cacheAdapter = factory.createMultiLayerCacheAdapter();
    assertNotNull(cacheAdapter);

    // expected two levels of cache adapters via OSGI injection
    assertEquals(2, cacheAdapter.cachingLevels());
  }

}
