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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategies;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineCacheControlTests extends AbstractJsonPipelineTest {


  @Test
  public void defaultMaxAgeIsZero() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}");

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    assertEquals(0, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAge() {

    int timeToLiveSeconds = 60;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the max-age must match the expiry time from the cache strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAgeFromResponseIsHigher() {

    int responseMaxAge = 600;
    int timeToLiveSeconds = 60;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{a:123}", responseMaxAge)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the overall max-age from the response should override that of our cache-point (because it is higher)
    assertEquals(responseMaxAge, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAgeFromResponseIsLower() {

    int responseMaxAge = 6;
    int timeToLiveSeconds = 60;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{a:123}", responseMaxAge)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the overall max-age from the response should *not* override that of our cache-point (because it is lower)
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cacheHitFreshContent() {

    int timeToLiveSeconds = 60;
    int cacheContentAge = 20;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the max-age must match the remaning time the content will be served from cache, before it is considered stale
    assertEquals(timeToLiveSeconds - cacheContentAge, output.getMaxAge());
  }

  @Test
  public void cacheHitIgnoreStaleContent() throws JSONException {

    int timeToLiveSeconds = 30;
    int cacheContentAge = 50;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(caching).get(anyString(), anyBoolean(), anyInt());

    // ...but the content from cache was not used, because it's stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{a:123}", json, JSONCompareMode.STRICT);

    // and the new content must have been put into the cache
    Mockito.verify(caching).put(anyString(), anyString(), eq(timeToLiveSeconds));

    // the max-age must be taken from the caching strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());

  }

  @Test
  public void cacheHitFallbackToStaleContent() throws JSONException {

    int timeToLiveSeconds = 30;
    int cacheContentAge = 50;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Simulated Exception in transport layer"))
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(caching).get(anyString(), anyBoolean(), anyInt());

    // ...and the content from cache was used as a fallback, even though it is stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{b:'cached'}", json, JSONCompareMode.STRICT);

    // the max-age must be taken from the caching strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cache404Responses() {

    int timeToLiveSeconds = 30;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    try {
      pipeline.getOutput().toBlocking().single();
      fail("expected JsonPipelineInputException with 404 response code to be thrown");
    }
    catch (JsonPipelineInputException e) {
      assertEquals(404, e.getStatusCode());
    }

    // make sure that an entry representing the 404 has been put into the cache
    Mockito.verify(caching).put(anyString(), anyString(), eq(timeToLiveSeconds));
  }

  @Test
  public void useCached404Response() {

    int timeToLiveSeconds = 30;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
    .thenReturn(Observable.just("{metadata: {generated:'" + CacheDateUtils.formatRelativeTime(-15) + "', statusCode: 404}, content: {reason: 'cached!'}}"));

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    try {
      pipeline.getOutput().toBlocking().single();
      fail("expected JsonPipelineInputException with 404 response code to be thrown");
    }
    catch (JsonPipelineInputException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("cached!", e.getMessage());
    }
  }
}
