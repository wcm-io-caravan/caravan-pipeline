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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer.CacheEnvelope;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineCacheTest extends AbstractJsonPipelineTest {


  @Test
  public void defaultMaxAgeIsZero() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}");

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    assertEquals(0, output.getMaxAge());
  }

  @Test
  public void cacheMissMaxAge() {

    int timeToLiveSeconds = 60;

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
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

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
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

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
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

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
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

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(cacheAdapter).get(anyString(), anyObject());

    // ...but the content from cache was not used, because it's stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{a:123}", json, JSONCompareMode.STRICT);

    // and the new content must have been put into the cache
    Mockito.verify(cacheAdapter).put(anyString(), anyString(), anyObject());

    // the max-age must be taken from the caching strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());

  }

  @Test
  public void cacheHitFallbackToStaleContent() throws JSONException {

    int timeToLiveSeconds = 30;
    int cacheContentAge = 50;

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Simulated Exception in transport layer"))
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    // make sure the cache was accessed...
    Mockito.verify(cacheAdapter).get(anyString(), anyObject());

    // ...and the content from cache was used as a fallback, even though it is stale
    String json = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals("{b:'cached'}", json, JSONCompareMode.STRICT);

    // the max-age must be taken from the caching strategy
    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cache404Responses() {

    int timeToLiveSeconds = 30;

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
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
    Mockito.verify(cacheAdapter).put(anyString(), anyString(), anyObject());
  }

  @Test
  public void useCached404Response() {

    int timeToLiveSeconds = 30;

    CacheEnvelope cached404 = CacheEnvelope.from404Response("original reason", new LinkedList<CaravanHttpRequest>(), null, null, getContextProperties());
    cached404.setGeneratedDate(CacheDateUtils.formatRelativeTime(-15));

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
    .thenReturn(Observable.just(cached404.getEnvelopeString()));

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    try {
      pipeline.getOutput().toBlocking().single();
      fail("expected JsonPipelineInputException with 404 response code to be thrown");
    }
    catch (JsonPipelineInputException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("original reason" + CachePointTransformer.CacheResponseObserver.SUFFIX_FOR_CACHED_404_REASON_STRING, e.getMessage());
    }
  }

  @Test
  public void cacheHit() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(10, TimeUnit.SECONDS);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(cacheAdapter.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(cacheAdapter.get(eq(cacheKey), anyObject()))
    .thenReturn(cachedContent("{b: 456}}", 5));

    String output = cached.getStringOutput().toBlocking().single();

    // only getCacheKey and get should have been called to check if it is available in the cache
    verify(cacheAdapter).getCacheKey(SERVICE_NAME, a.getDescriptor());
    verify(cacheAdapter).get(eq(cacheKey), anyObject());
    verifyNoMoreInteractions(cacheAdapter);

    // make sure that the version from the cache is emitted in the response
    JSONAssert.assertEquals("{b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void cacheMissAndStore() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(1, TimeUnit.SECONDS);
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(cacheAdapter.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(cacheAdapter.get(eq(cacheKey), anyObject()))
    .thenReturn(Observable.empty());

    String output = cached.getStringOutput().toBlocking().single();

    // get must have been called to check if the document is available in the cache
    verify(cacheAdapter).get(eq(cacheKey), anyObject());

    // put must have been called with an cache envelope version of the JSON, that contains an additional _cacheInfo
    verify(cacheAdapter).put(eq(cacheKey), Matchers.argThat(new BaseMatcher<String>() {

      @Override
      public boolean matches(Object item) {
        ObjectNode storedNode = JacksonFunctions.stringToObjectNode(item.toString());

        return storedNode.has("metadata")
            && storedNode.has("content")
            && storedNode.get("content").get("a").asInt() == 123;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected storedObject to contain original value & _cacheInfo");
      }

    }), anyObject());

    // the _cacheInfo however should not be contained in the output
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

}
