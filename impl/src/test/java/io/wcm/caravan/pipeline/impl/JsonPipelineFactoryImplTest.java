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

import rx.Observable;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableListMultimap;

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

    // Sample #1
    // The working sample of initialization. Specifies default oder of list entries
    // last entry of the list should be initialized first
    secondLevelCacheAdapter = context.registerService(CacheAdapter.class, secondLevelCacheAdapter);
    // first entry of the list should be initialized last
    firstLevelCacheAdapter = context.registerService(CacheAdapter.class, firstLevelCacheAdapter);


    //    Sample #2
    //    Expected that Constants.SERVICE_RANKING property should modify ranking level of service, which is 0 defaults.
    //    1st Level cache with ranking 1000 should act earlier than 2nd level cache with ranking 3000
    //    Practically no ranking update is happened. Default order of initialization is kept as in Sample #1
    //    #testCreateMultiLayerCacheAdapter fails, but should pass.
    //    firstLevelCacheAdapter = context.registerService(CacheAdapter.class, firstLevelCacheAdapter,
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 1000L));
    //    secondLevelCacheAdapter = context.registerService(CacheAdapter.class, secondLevelCacheAdapter,
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 3000L));

    //    Sample #3
    //    Expected that Constants.SERVICE_RANKING property should modify ranking level of service, which is 0 defaults.
    //    1st Level cache with ranking 3000 should act later than 2nd level cache with ranking 1000
    //    Practically no ranking update is happened. Default order of initialization is kept as in Sample #1
    //    #testCreateMultiLayerCacheAdapter passes as in Sample #1, but should fail.
    //    secondLevelCacheAdapter = context.registerService(CacheAdapter.class, secondLevelCacheAdapter,
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 1000L));
    //    firstLevelCacheAdapter = context.registerService(CacheAdapter.class, firstLevelCacheAdapter,
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 3000L));

    //     Sample #4
    //     It just does not inject anything
    //     CacheAdapter adapter = new io.wcm.caravan.pipeline.impl.cache.CacheAdapterMock("name");
    //     MockOsgi.injectServices(adapter, context.bundleContext());
    //     MockOsgi.activate(adapter, context.bundleContext(),
    //         ImmutableMap.of(Constants.SERVICE_RANKING, 1000L, Constants.SERVICE_PID, "thirdLevelCacheAdapter"));


    //    Sample #5
    //    Fails with "NoScrMetadata No OSGi SCR metadata found".
    //    MockOsgi.activate(secondLevelCacheAdapter, context.bundleContext(),
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 1000L, Constants.SERVICE_PID, "secondLevelCacheAdapter"));
    //    MockOsgi.activate(firstLevelCacheAdapter, context.bundleContext(),
    //        ImmutableMap.of(Constants.SERVICE_RANKING, 3000L, Constants.SERVICE_PID, "firstLevelCacheAdapter"));

    //    Sample #6
    //    Fails with "NoScrMetadata No OSGi SCR metadata found". It was a reason to replace mocks with custom annotated implementation
    //    firstLevelCacheAdapter = context.registerInjectActivateService(firstLevelCacheAdapter, ImmutableMap.of(Constants.SERVICE_RANKING, 1000L));
    //    secondLevelCacheAdapter = context.registerInjectActivateService(secondLevelCacheAdapter, ImmutableMap.of(Constants.SERVICE_RANKING, 3000L));

    factory = new JsonPipelineFactoryImpl();
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

    List<CacheAdapter> cacheAdapters = cacheAdapter.getCacheAdapters();

    // expected firstLevelCacheAdapter at place 0 and secondLevelCacheAdapter at place 1
    // according to the ranking order 1000 of CacheAdapterMock2 (higher priority)
    // and 3000 ranking order of CacheAdapterMock (lower priority)
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
