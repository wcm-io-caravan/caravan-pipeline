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

import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class CachePointTransformerTest {

  @Mock
  private CacheAdapter cacheAdapter;

  @Mock
  private CacheStrategy cacheStrategy;

  @Before
  public void setUp() {
    Mockito.when(cacheAdapter.getCacheKey(Matchers.anyString(), Matchers.anyString())).thenReturn("test-cache-key");
    Mockito.when(cacheAdapter.get(Matchers.anyString(), Matchers.anyBoolean(), Matchers.anyInt())).thenReturn(Observable.just("{}"));
  }

  @Test
  public void test_ignoreCache() {
    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").headers(ImmutableListMultimap.of("Cache-Control", "no-cache")).build();
    CachePointTransformer transformer = new CachePointTransformer(cacheAdapter, Lists.newArrayList(request), "test-descriptor", null, cacheStrategy);
    Observable<JsonPipelineOutput> outputObservable = Observable.just(new JsonPipelineOutputImpl(new ObjectMapper().createObjectNode()));
    transformer.call(outputObservable).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.never()).get(Matchers.anyString(), Matchers.anyBoolean(), Matchers.anyInt());
  }

  @Test
  public void test_useCache() {
    CaravanHttpRequest request = new CaravanHttpRequestBuilder("test-service").headers(ImmutableListMultimap.of("Cache-Control", "max-age: 100")).build();
    CachePointTransformer transformer = new CachePointTransformer(cacheAdapter, Lists.newArrayList(request), "test-descriptor", null, cacheStrategy);
    Observable<JsonPipelineOutput> outputObservable = Observable.just(new JsonPipelineOutputImpl(new ObjectMapper().createObjectNode()));
    transformer.call(outputObservable).toBlocking().first();

    Mockito.verify(cacheAdapter, Mockito.atLeastOnce()).get(Matchers.anyString(), Matchers.anyBoolean(), Matchers.anyInt());
  }
}
