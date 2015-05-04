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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.cache.CacheStrategies;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelinePerformanceRegressionTest extends AbstractJsonPipelineTest {

  public JsonPipelinePerformanceRegressionTest() {
    super();
  }

  @Test
  public void testSingle() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody(getBooksString());
    assertFirstPipelineMetrics(pipeline, pipeline);
  }

  @Test
  public void testFirstAssertExists() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}");
    JsonPipeline asserted = pipeline.assertExists("$.a", 500, "a not found");
    assertFirstPipelineMetrics(pipeline, asserted);
  }

  @Test
  public void testLastAssertExists() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}").assertExists("$.a", 500, "a not found");
    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstHandleException() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline exceptionHandler = pipeline
        .handleException((fallbackContent, ex) -> {
          fail("this should not be called");
          return Observable.just(fallbackContent);
        });

    assertFirstPipelineMetrics(pipeline, exceptionHandler);

  }

  @Test
  public void testLastHandleException() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}").handleException(
        (fallbackContent, ex) -> {
          fail("this should not be called");
          return Observable.just(fallbackContent);
        });

    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstCollect() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline collected = pipeline.collect("$..label");
    assertFirstPipelineMetrics(pipeline, collected);
  }

  @Test
  public void testLastCollect() {
    JsonPipelineImpl collected = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}").collect("$..label");
    assertNextPipelineMetrics(collected);
  }

  @Test
  public void testFirstCollectInto() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline collected = pipeline.collect("$..label", "extracted");
    assertFirstPipelineMetrics(pipeline, collected);
  }

  @Test
  public void testLastCollectInto() {
    JsonPipelineImpl collected = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}").collect("$..label", "extracted");
    assertNextPipelineMetrics(collected);
  }

  @Test
  public void testFirstExtract() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a");
    assertFirstPipelineMetrics(pipeline, extracted);
  }

  @Test
  public void testLastExtract() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}").extract("$.a");
    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstExtractInto() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a", "extracted");
    assertFirstPipelineMetrics(pipeline, extracted);
  }

  @Test
  public void testLastExtractInto() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}").extract("$.a", "extracted");
    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstMerge() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}");
    JsonPipeline merged = pipeline.merge(newPipelineWithResponseBody("{b: 456}"));
    assertFirstPipelineMetrics(pipeline, merged);
  }

  @Test
  public void testLastMerge() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}").merge(newPipelineWithResponseBody("{b: 456}"));
    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstMergeInto() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}");
    JsonPipeline merged = pipeline.merge(newPipelineWithResponseBody("{b: 456}"), "extracted");
    assertFirstPipelineMetrics(pipeline, merged);
  }

  @Test
  public void testLastMergeInto() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}").merge(newPipelineWithResponseBody("{b: 456}"), "extracted");
    assertNextPipelineMetrics(pipeline);
  }

  @Test
  public void testFirstAction() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a: 123}");

    JsonPipelineAction action = mock(JsonPipelineAction.class);
    when(action.execute(any(), any())).thenReturn(pipeline.getOutput());
    JsonPipeline afterAction = pipeline.applyAction(action);

    assertFirstPipelineMetrics(pipeline, afterAction);
  }

  @Test
  public void testLastAction() {
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}");

    JsonPipelineAction action = mock(JsonPipelineAction.class);
    when(action.execute(any(), any())).thenReturn(pipeline.getOutput());
    JsonPipelineImpl afterAction = (JsonPipelineImpl)pipeline.applyAction(action);

    assertNextPipelineMetrics(afterAction);
  }

  @Test
  public void testFirstCachePoint() {
    int timeToLiveSeconds = 60;
    int cacheContentAge = 20;

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
    .thenReturn(cachedContent("{b: 'cached'}}", cacheContentAge));

    JsonPipelineImpl firstPipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a:123}");
    JsonPipeline lastPipeline = firstPipeline.addCachePoint(CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    PerformanceMetrics first = firstPipeline.getPerformanceMetrics();
    assertStartMetrics(first);

    lastPipeline.getOutput().toBlocking().single();

    assertNull(first.getStartTime());
    assertNull(first.getEndTime());
  }

  @Test
  public void testLastCachePoint() {
    int timeToLiveSeconds = 60;
    int cacheContentAge = 20;

    Mockito.when(cacheAdapter.get(anyString(), anyObject()))
    .thenReturn(cachedContent("{b: 'cached'}", cacheContentAge));

    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a:123}").addCachePoint(
        CacheStrategies.timeToLive(timeToLiveSeconds, TimeUnit.SECONDS));

    PerformanceMetrics last = pipeline.getPerformanceMetrics();
    assertStartMetrics(last);

    pipeline.getOutput().toBlocking().single();

    assertEndMetrics(last);
    assertNull(last.getTakenTimeByStepEnd());
    assertNull(last.getTakenTimeByStepStart());
  }

  @Test
  public void testThreeSteps() {
    JsonPipelineImpl first = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipelineImpl second = (JsonPipelineImpl)first.extract("$.a");
    JsonPipelineImpl third = (JsonPipelineImpl)second.extract("$.label");

    PerformanceMetrics firstPerformanceMetrics = first.getPerformanceMetrics();
    PerformanceMetrics secondPerformanceMetrics = second.getPerformanceMetrics();
    PerformanceMetrics thirdPerformanceMetrics = third.getPerformanceMetrics();

    assertStartMetrics(firstPerformanceMetrics);
    assertStartMetrics(secondPerformanceMetrics);
    assertStartMetrics(thirdPerformanceMetrics);

    third.getJsonOutput().toBlocking().single();

    assertFirstMetrics(firstPerformanceMetrics);
    assertNextMetrics(secondPerformanceMetrics);
    assertNextMetrics(thirdPerformanceMetrics);
  }

  private void assertFirstPipelineMetrics(JsonPipelineImpl pipeline, JsonPipeline toSubscribe) {
    PerformanceMetrics performanceMetrics = pipeline.getPerformanceMetrics();
    assertStartMetrics(performanceMetrics);

    toSubscribe.getJsonOutput().toBlocking().single();

    assertFirstMetrics(performanceMetrics);
  }

  private void assertNextPipelineMetrics(JsonPipelineImpl pipeline) {
    PerformanceMetrics performanceMetrics = pipeline.getPerformanceMetrics();
    assertStartMetrics(performanceMetrics);

    pipeline.getJsonOutput().toBlocking().single();

    assertNextMetrics(performanceMetrics);
  }


  private void assertStartMetrics(PerformanceMetrics performanceMetrics) {
    assertNull(performanceMetrics.getStartTime());
    assertNull(performanceMetrics.getEndTime());
  }

  private void assertEndMetrics(PerformanceMetrics performanceMetrics) {
    assertTrue(performanceMetrics.getStartTime() > 0);
    assertTrue(performanceMetrics.getEndTime() > 0);
    assertTrue(performanceMetrics.getTakenTimeByStep() >= 0);
    System.out.println(performanceMetrics.toString());
  }

  private void assertNextMetrics(PerformanceMetrics performanceMetrics) {
    assertEndMetrics(performanceMetrics);
    assertTrue(performanceMetrics.getTakenTimeByStepEnd() >= 0);
    assertTrue(performanceMetrics.getTakenTimeByStepStart() >= 0);
  }

  private void assertFirstMetrics(PerformanceMetrics performanceMetrics) {
    assertEndMetrics(performanceMetrics);
    assertNull(performanceMetrics.getTakenTimeByStepEnd());
    assertNull(performanceMetrics.getTakenTimeByStepStart());
  }

}
