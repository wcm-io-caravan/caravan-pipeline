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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategies;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
  public void cacheMissMaxAgeTakenFromStrategy() {

    int timeToLiveSeconds = 60;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
        .thenReturn(Observable.empty());

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    assertEquals(timeToLiveSeconds, output.getMaxAge());
  }

  @Test
  public void cacheHitMaxAgeIsBasedOnAgeAndStale() {

    int timeToLiveSeconds = 60;
    int cacheContentAge = 20;

    Mockito.when(caching.get(anyString(), anyBoolean(), anyInt()))
        .thenReturn(cachedContent("{b: 456}}", cacheContentAge));

    JsonPipeline pipeline = newPipelineWithResponseBody("{a:123}")
        .addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();

    assertEquals(timeToLiveSeconds - cacheContentAge, output.getMaxAge());
  }
}
