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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.concurrent.TimeUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public abstract class JsonPipelineAddCachePointTest extends AbstractJsonPipelineTest {

  public JsonPipelineAddCachePointTest() {
    super();
  }


  @Test
  public void cacheHit() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(10, TimeUnit.SECONDS);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(caching.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(caching.get(eq(cacheKey), anyBoolean(), anyInt()))
    .thenReturn(cachedContent("{b: 456}}", 5));

    String output = cached.getStringOutput().toBlocking().single();

    // only getCacheKey and get should have been called to check if it is available in the cache
    verify(caching).getCacheKey(SERVICE_NAME, a.getDescriptor());
    verify(caching).get(eq(cacheKey), eq(false), eq(10));
    verifyNoMoreInteractions(caching);

    // make sure that the version from the cache is emitted in the response
    JSONAssert.assertEquals("{b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void cacheMissAndStore() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(1, TimeUnit.SECONDS);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(caching.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(caching.get(eq(cacheKey), anyBoolean(), anyInt()))
    .thenReturn(Observable.empty());

    String output = cached.getStringOutput().toBlocking().single();

    // get must have been called to check if the document is available in the cache
    verify(caching).get(eq(cacheKey), eq(false), eq(1));

    // put must have been called with an cache envelope version of the JSON, that contains an additional _cacheInfo
    verify(caching).put(eq(cacheKey), Matchers.argThat(new BaseMatcher<String>() {

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

    }), eq(1));

    // the _cacheInfo however should not be contained in the output
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

}
