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
package io.wcm.caravan.pipeline.impl.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.AbstractCaravanTestCase;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer.CacheEnvelope;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class CachePointTransformerTest extends AbstractCaravanTestCase {

  @Mock
  private CacheStrategy cacheStrategy;

  private CachePersistencyOptions cachePersistencyOptions;

  private ObjectNode uncachedNode;

  private int mockEntryAge = 60;

  @Before
  public void setUp() {
    cachePersistencyOptions = CachePersistencyOptions.createTransient(100);
    Mockito.when(cacheStrategy.getCachePersistencyOptions(anyCollection())).thenReturn(cachePersistencyOptions);

    uncachedNode = JacksonFunctions.stringToObjectNode("{cached: false}");

    CacheEnvelope cacheEnvelope = CacheEnvelope.fromContentString("{cached: true}", mockEntryAge);
    Mockito.when(cacheAdapter.get(anyString(), any())).thenReturn(Observable.just(cacheEnvelope.getEnvelopeString()));
  }

  @Test
  public void test_useCache() {
    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").build();
    CachePointTransformer transformer = new CachePointTransformer(getJsonPipelineContext(), Lists.newArrayList(request), "test-descriptor", cacheStrategy);
    JsonPipelineOutputImpl uncachedOutput = new JsonPipelineOutputImpl(uncachedNode, ImmutableList.of(request));

    JsonPipelineOutput actualOutput = transformer.call(Observable.just(uncachedOutput)).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.atLeastOnce()).get(anyString(), any());
    assertTrue(actualOutput.getPayload().get("cached").asBoolean());
  }

  @Test
  public void test_ignoreNoCacheHeader() {

    // ignoring cached content via the Cache-Control: no-cache is no longer supported, use max-age instead!
    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").header("Cache-Control", "no-cache").build();
    CachePointTransformer transformer = new CachePointTransformer(getJsonPipelineContext(), Lists.newArrayList(request), "test-descriptor", cacheStrategy);
    JsonPipelineOutputImpl uncachedOutput = new JsonPipelineOutputImpl(uncachedNode, ImmutableList.of(request));

    JsonPipelineOutput actualOutput = transformer.call(Observable.just(uncachedOutput)).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.atLeastOnce()).get(anyString(), any());
    assertTrue(actualOutput.getPayload().get("cached").asBoolean());
  }

  @Test
  public void test_useCache_maxAge() {

    // check that cached content is used if max-age header is set, but content was cached more recently

    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").header("Cache-Control", "max-age=120").build();
    CachePointTransformer transformer = new CachePointTransformer(getJsonPipelineContext(), Lists.newArrayList(request), "test-descriptor", cacheStrategy);
    JsonPipelineOutputImpl uncachedOutput = new JsonPipelineOutputImpl(uncachedNode, ImmutableList.of(request));

    JsonPipelineOutput actualOutput = transformer.call(Observable.just(uncachedOutput)).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.atLeastOnce()).get(anyString(), any());
    assertTrue(actualOutput.getPayload().get("cached").asBoolean());
  }

  @Test
  public void test_ignoreCache_maxAge() {

    // check that cached content is ignored if max-age header is set, and content is older

    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").header("Cache-Control", "max-age=30").build();
    CachePointTransformer transformer = new CachePointTransformer(getJsonPipelineContext(), Lists.newArrayList(request), "test-descriptor", cacheStrategy);
    JsonPipelineOutputImpl uncachedOutput = new JsonPipelineOutputImpl(uncachedNode, ImmutableList.of(request));

    JsonPipelineOutput actualOutput = transformer.call(Observable.just(uncachedOutput)).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.atLeastOnce()).get(anyString(), any());
    assertFalse(actualOutput.getPayload().get("cached").asBoolean());
  }
  @Test
  public void testCacheEnvelopeAvailabilityAt404() {

    CacheEnvelope cached404 = CacheEnvelope.from404Response("original reason", 60, new LinkedList<CaravanHttpRequest>(), null, null,
        getContextProperties());
    JsonNode node404 = JacksonFunctions.stringToNode(cached404.getEnvelopeString());
    JsonNode properties = node404.path("metadata").path("contextProperties");
    assertNotNull(properties);
    assertEquals("123-id", properties.get("id").asText());
    assertEquals(404, node404.path("metadata").path("statusCode").asInt());
    assertEquals("original reason", node404.path("metadata").path("reason").asText());
    assertEquals("null", node404.path("metadata").path("cacheKey").asText());
    assertEquals("null", node404.path("metadata").path("pipeline").asText());

  }

  @Test
  public void testCacheEnvelopeAvailabilityAt200() {
    CacheEnvelope cached200 = CacheEnvelope.from200Response(JacksonFunctions.stringToNode("{}"), 0, new LinkedList<CaravanHttpRequest>(),
        "cacheKey", "descriptor", getContextProperties());

    JsonNode node200 = JacksonFunctions.stringToNode(cached200.getEnvelopeString());
    JsonNode properties = node200.path("metadata").path("contextProperties");
    assertNotNull(properties);
    assertEquals("123-id", properties.get("id").asText());
    assertEquals(200, node200.path("metadata").path("statusCode").asInt());
    assertEquals("cacheKey", node200.path("metadata").path("cacheKey").asText());
    assertEquals("descriptor", node200.path("metadata").path("pipeline").asText());

  }
}
