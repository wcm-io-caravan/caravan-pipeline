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
package io.wcm.caravan.pipeline.extensions.hal.crawler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.extensions.hal.client.HalClient;
import io.wcm.caravan.testing.http.RequestMatcher;
import io.wcm.caravan.testing.pipeline.JsonPipelineContext;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class HalCrawlerTest {

  public OsgiContext osgiCtx = new OsgiContext();
  public JsonPipelineContext pipelineCtx = new JsonPipelineContext(osgiCtx);

  @Rule
  public RuleChain chain = RuleChain
      .outerRule(osgiCtx)
      .around(pipelineCtx);

  private HalClient client;
  private HalCrawler crawler;
  private JsonPipeline pipeline;

  private HalResource entryPoint;
  private HalResource resourceLink1;
  private HalResource resourceLink1Section1;
  private HalResource resourceLink2;
  private HalResource resourceEmbedded1;


  @Before
  public void setUp() {

    entryPoint = HalResourceFactory.createResource("/resource")
        .addLinks("section", HalResourceFactory.createLink("/resource/link-1"), HalResourceFactory.createLink("/resource/link-2"))
        .addEmbedded("item", HalResourceFactory.createResource("/resource/embedded-1"));
    resourceLink1 = HalResourceFactory.createResource("/resource/link-1")
        .setLink("item", HalResourceFactory.createLink("/resource/link-1/section-1"));
    resourceLink1Section1 = HalResourceFactory.createResource("/resource/link-1/section-1");
    resourceLink2 = HalResourceFactory.createResource("/resource/link-2");
    resourceEmbedded1 = HalResourceFactory.createResource("/resource/embedded-1");

    pipelineCtx.getCaravanHttpClient().mockRequest().url(entryPoint.getLink().getHref()).response(entryPoint.getModel().toString());
    pipelineCtx.getCaravanHttpClient().mockRequest().url(resourceLink1.getLink().getHref()).response(resourceLink1.getModel().toString());
    pipelineCtx.getCaravanHttpClient().mockRequest().url(resourceLink1Section1.getLink().getHref()).response(resourceLink1Section1.getModel().toString());
    pipelineCtx.getCaravanHttpClient().mockRequest().url(resourceLink2.getLink().getHref()).response(resourceLink2.getModel().toString());
    pipelineCtx.getCaravanHttpClient().mockRequest().url(resourceEmbedded1.getLink().getHref()).response(resourceEmbedded1.getModel().toString());

    client = new HalClient("test-service", CacheStrategies.noCache());
    crawler = new HalCrawler(client, LinkExtractors.all(), UriParametersProviders.empty(), OutputProcessors.report(), StopCriteria.alwaysEnabled());
    pipeline = pipelineCtx.getJsonPipelineFactory().create(new CaravanHttpRequestBuilder().append(entryPoint.getLink().getHref()).build());

  }

  @Test
  public void shouldCrawlAllLinks() {

    JsonPipelineOutput output = pipeline.applyAction(crawler).getOutput().toBlocking().single();
    HalResource hal = new HalResource(output.getPayload());
    assertEquals(5, hal.getLinks().size());

    Map<RequestMatcher, AtomicInteger> counter = pipelineCtx.getCaravanHttpClient().getMatchingCounter();
    assertEquals(5, counter.size());
    counter.entrySet().stream()
        .filter(entry -> entry.getValue().get() != 1)
        .forEach(entry -> {
          fail(entry.getKey() + " was invoked " + entry.getValue() + " times");
        });

  }

  @Test(expected = JsonPipelineInputException.class)
  public void shouldCrashWithoutExceptionHandler() {

    // add Not Found Link to HAL Entry Point
    resourceLink1.addLinks("item", HalResourceFactory.createLink("/not-found"));
    pipelineCtx.getCaravanHttpClient().getRequestMatchers().get(1).response(resourceLink1.getModel().toString());
    // mock Not Found request
    pipelineCtx.getCaravanHttpClient().mockRequest().url("/not-found").response(new CaravanHttpResponseBuilder().status(404).reason("Not Found").build());

    pipeline.applyAction(crawler).getOutput().toBlocking().single();

  }

  @Test
  public void shouldNotCrashWithExceptionHandler() {

    crawler = new HalCrawler(client, LinkExtractors.all(), UriParametersProviders.empty(), OutputProcessors.report(), StopCriteria.alwaysEnabled());

    // add Not Found Link to HAL Entry Point
    resourceLink1.addLinks("item", HalResourceFactory.createLink("/not-found"));
    pipelineCtx.getCaravanHttpClient().getRequestMatchers().get(1).response(resourceLink1.getModel().toString());
    // mock Not Found request
    pipelineCtx.getCaravanHttpClient().mockRequest().url("/not-found").response(new CaravanHttpResponseBuilder().status(404).reason("Not Found").build());

    client.addExceptionHandler(new JsonPipelineExceptionHandler() {

      @Override
      public Observable<JsonPipelineOutput> call(JsonPipelineOutput defaultFallbackContent, RuntimeException caughtException) {
        return Observable.just(defaultFallbackContent.withPayload(JsonNodeFactory.instance.textNode(caughtException.getMessage())));
      }

    });

    JsonPipelineOutput output = pipeline.applyAction(crawler).getOutput().toBlocking().single();
    HalResource hal = new HalResource(output.getPayload());
    assertEquals(6, hal.getLinks().size());

  }

  @Test
  public void shouldUseClientCacheStrategy() {

    CacheStrategy cacheStrategy = createCacheStrategyMock();
    client = new HalClient("test-service", cacheStrategy);
    crawler = new HalCrawler(client, LinkExtractors.all(), UriParametersProviders.empty(), OutputProcessors.report(), StopCriteria.alwaysEnabled());

    pipeline.applyAction(crawler).getOutput().toBlocking().single();

    Mockito.verify(cacheStrategy, atLeast(1)).getCachePersistencyOptions(Matchers.any());

  }

  private CacheStrategy createCacheStrategyMock() {

    CacheStrategy cacheStrategy = Mockito.mock(CacheStrategy.class);
    Mockito.when(cacheStrategy.getCachePersistencyOptions(Matchers.any())).thenReturn(
        CacheStrategies.noCache().getCachePersistencyOptions(Collections.emptyList()));
    return cacheStrategy;

  }

  @Test
  public void shouldUseGivenCacheStrategyIfProvided() {

    CacheStrategy clientCacheStrategy = createCacheStrategyMock();
    client = new HalClient("test-service", clientCacheStrategy);
    crawler = new HalCrawler(client, LinkExtractors.all(), UriParametersProviders.empty(), OutputProcessors.report(), StopCriteria.alwaysEnabled());

    CacheStrategy extraCacheStrategy = createCacheStrategyMock();
    crawler.setCacheStrategy(extraCacheStrategy);

    pipeline.applyAction(crawler).getOutput().toBlocking().single();

    Mockito.verify(clientCacheStrategy, never()).getCachePersistencyOptions(Matchers.any());
    Mockito.verify(extraCacheStrategy, atLeast(1)).getCachePersistencyOptions(Matchers.any());

  }

}
