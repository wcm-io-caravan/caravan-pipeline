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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer.CacheEnvelope;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineCacheTest extends AbstractJsonPipelineTest {


  @Test
  public void defaultMaxAgeIsUndefined() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}");

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    assertEquals(-1, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAge() {

    int timeToLiveSeconds = 60;

    Mockito.when(cacheAdapter.get(anyString(), any()))
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

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{a:123}", responseMaxAge)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the lower time-to-live of the cache-strategy should override the max-age from the response
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAgeFromResponseIsLower() {

    int responseMaxAge = 6;
    int timeToLiveSeconds = 60;

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{a:123}", responseMaxAge)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // the lower max-age from the response should override the time-to-live of the cache-strategy
    assertEquals(responseMaxAge, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAgeFromResponseIsMissing() {

    int timeToLiveSeconds = 60;

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // if no max-age is specified in the response the time-to-live from the cache-strategy should become effective
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cacheHitFreshContent() {

    int timeToLiveSeconds = 60;
    int cacheContentAge = 20;

    Mockito.when(cacheAdapter.get(anyString(), any()))
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
    int maxAgeFomResponse = 20;

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{a:123}", maxAgeFomResponse)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(cacheAdapter).get(anyString(), any());

    // ...but the content from cache was not used, because it's stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{a:123}", json, JSONCompareMode.STRICT);

    // and the new content must have been put into the cache
    Mockito.verify(cacheAdapter).put(anyString(), anyString(), any());

    //  the max-age must be taken from the response, since it's smaller than the caching strategy
    assertEquals(maxAgeFomResponse, output.getMaxAge());

  }

  @Test
  public void cacheHitIgnoreStaleContent_NoMaxAgeInResponse() throws JSONException {

    int timeToLiveSeconds = 30;
    int cacheContentAge = 50;

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(cacheAdapter).get(anyString(), any());

    // ...but the content from cache was not used, because it's stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{a:123}", json, JSONCompareMode.STRICT);

    // and the new content must have been put into the cache
    Mockito.verify(cacheAdapter).put(anyString(), anyString(), any());

    //  the max-age must be taken from the caching strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());

  }

  @Test
  public void cacheHitFallbackToStaleContent() throws JSONException {

    int timeToLiveSeconds = 30;
    int cacheContentAge = 50;

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Simulated Exception in transport layer"))
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(cacheAdapter).get(anyString(), any());

    // ...and the content from cache was used as a fallback, even though it is stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{b:'cached'}", json, JSONCompareMode.STRICT);

    // the max-age must be set to 0
    assertEquals(0, output.getMaxAge());
  }

  @Test
  public void cache404Responses() {

    int timeToLiveSeconds = 30;

    Mockito.when(cacheAdapter.get(anyString(), any()))
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
    Mockito.verify(cacheAdapter).put(anyString(), anyString(), any());
  }

  @Test
  public void useCached404Response() {

    int timeToLiveSeconds = 30;
    int maxAgeFor404 = 60;
    int timeSince404Response = 15;

    String originalReason = "original reason";

    CacheEnvelope cached404 = CacheEnvelope.from404Response(originalReason, maxAgeFor404, new LinkedList<CaravanHttpRequest>(), null, null,
        getContextProperties());
    cached404.setGeneratedDate(CacheDateUtils.formatRelativeTime(-timeSince404Response));

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(Observable.just(cached404.getEnvelopeString()));

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    try {
      pipeline.getOutput().toBlocking().single();
      fail("expected JsonPipelineInputException with 404 response code to be thrown");
    }
    catch (JsonPipelineInputException e) {
      assertEquals(404, e.getStatusCode());

      assertTrue("the message should contain reason phrase from the original response", e.getMessage().startsWith(originalReason));
      assertTrue("but something should have been appended to indicate it's coming from cache", e.getMessage().length() > originalReason.length());
    }
  }

  @Test
  public void ignoreStale404Response() {

    // make sure that 404 errors are ignored even if the cache strategy has a long time to live
    int timeToLiveSeconds = 300;
    int maxAgeFor404 = 60;
    int timeSince404Response = 75;

    String originalReason = "original reason";

    CacheEnvelope cached404 = CacheEnvelope.from404Response(originalReason, maxAgeFor404, new LinkedList<CaravanHttpRequest>(), null, null,
        getContextProperties());
    cached404.setGeneratedDate(CacheDateUtils.formatRelativeTime(-timeSince404Response));
    cached404.setExpiresDate(CacheDateUtils.formatRelativeTime(maxAgeFor404 - timeSince404Response));

    Mockito.when(cacheAdapter.get(anyString(), any()))
    .thenReturn(Observable.just(cached404.getEnvelopeString()));

    JsonPipeline pipeline = newPipelineWithResponseCode(200)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();
    assertEquals("expect status code from response", 200, output.getStatusCode());

  }

  @Test
  public void cacheHit() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(10, TimeUnit.SECONDS);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "testService:GET(//testService/path)";

    when(cacheAdapter.get(eq(cacheKey), any()))
    .thenReturn(cachedContent("{b: 456}}", 5));

    String output = cached.getStringOutput().toBlocking().single();

    // only get should have been called to check if it is available in the cache
    verify(cacheAdapter).get(eq(cacheKey), any());
    verifyNoMoreInteractions(cacheAdapter);

    // make sure that the version from the cache is emitted in the response
    JSONAssert.assertEquals("{b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void cacheMissAndStore() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(1, TimeUnit.SECONDS);
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "testService:GET(//testService/path)";

    when(cacheAdapter.get(eq(cacheKey), any()))
    .thenReturn(Observable.empty());

    String output = cached.getStringOutput().toBlocking().single();

    // get must have been called to check if the document is available in the cache
    verify(cacheAdapter).get(eq(cacheKey), any());

    // put must have been called with an cache envelope version of the JSON, that contains an additional _cacheInfo
    verify(cacheAdapter).put(eq(cacheKey), ArgumentMatchers.argThat(new ArgumentMatcher<String>() {

      @Override
      public boolean matches(String item) {
        ObjectNode storedNode = JacksonFunctions.stringToObjectNode(item);

        return storedNode.has("metadata")
            && storedNode.has("content")
            && storedNode.get("content").get("a").asInt() == 123;
      }

    }), any());

    // the _cacheInfo however should not be contained in the output
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void checkNotCacheableStrategy() {

    CacheStrategy strategy = CacheStrategies.noCache();
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    // the same JsonPipeline instance should be returned because no caching is required
    assertEquals(a, cached);
  }

  @Test
  public void cacheCacheableStrategy() {
    CacheStrategy strategy = CacheStrategies.timeToLive(10, TimeUnit.SECONDS);
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    // a new JsonPipeline instance should be returned because a caching strategy was included
    assertNotEquals(a, cached);
  }

}
