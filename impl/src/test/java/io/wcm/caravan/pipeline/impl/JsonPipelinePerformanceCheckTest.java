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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategies;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.Exceptions;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelinePerformanceCheckTest extends AbstractJsonPipelineTest {

  public JsonPipelinePerformanceCheckTest() {
    super();
  }


  @Override
  protected JsonPipelineContextImpl getJsonPipelineContext() {
    return new JsonPipelineContextImpl(jsonPipelineFactory, cacheAdapter, metricRegistry, getContextProperties(), true);
  }

  @Test
  public void testSingle() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody(getBooksString());
    assertTrue(pipeline.getJsonPipelineContext().isPerformanceMetricsEnabled());
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
    JsonPipeline collected = pipeline.collect("$..label", "extracted");
    assertFirstPipelineMetrics(pipeline, collected);
  }

  @Test
  public void testLastCollect() {
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
  public void testTransformer() {
    PerformanceMetrics custom = PerformanceMetrics.createNew("TEST", null, null);

    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a:123}");
    PerformanceMetrics first = pipeline.getPerformanceMetrics();
    PerformanceMetrics last = first.createNext("CUSTOM TRANSFORM", null);

    Observable<JsonPipelineOutput> testedOutput = pipeline.getOutput()
        .compose(new Transformer<JsonPipelineOutput, JsonPipelineOutput>() {

          @Override
          public Observable<JsonPipelineOutput> call(Observable<JsonPipelineOutput> t) {
            return Observable.create(new OnSubscribe<JsonPipelineOutput>() {

              @Override
              public void call(Subscriber<? super JsonPipelineOutput> subscriber) {

                JsonPipeline cached = newPipelineWithResponseBody("{b: 'cached'}");
                cached.getOutput().subscribe(new Observer<JsonPipelineOutput>() {

                  @Override
                  public void onCompleted() {
                    subscriber.onCompleted();
                  }

                  @Override
                  public void onError(Throwable e) {
                    subscriber.onError(e);
                  }

                  @Override
                  public void onNext(JsonPipelineOutput output) {
                    try {
                      Thread.sleep(2);
                      custom.setStartTimestamp();
                      Thread.sleep(2);
                      custom.setEndTimestamp();
                      Thread.sleep(2);
                    }
                    catch (InterruptedException ex) {
                      custom.setEndTimestamp();
                    }
                    subscriber.onNext(output);
                  }
                });
              }
            });
          }
        }
            ).doOnSubscribe(last.getStartAction()).doOnTerminate(last.getEndAction());

    assertStartMetrics(first);
    assertStartMetrics(last);
    assertStartMetrics(custom);

    testedOutput.toBlocking().single();

    assertNull(first.getStartTime());
    assertNull(first.getEndTime());

    assertEndMetrics(last);
    assertNull(last.getTakenTimeByStepEnd());
    assertNull(last.getTakenTimeByStepStart());

    //    FastDateFormat format = FastDateFormat.getInstance("HH:mm:ss S");
    //    System.out.println(format.format(last.getStartTime()) + " - first subscription");
    //    System.out.println(format.format(custom.getStartTime()) + " - custom subscription");
    //    System.out.println(format.format(custom.getEndTime()) + " - custom termination");
    //    System.out.println(format.format(last.getEndTime()) + " - first termination");

    assertTrue(custom.getStartTime() - last.getStartTime() >= 2);
    assertTrue(last.getEndTime() - custom.getEndTime() >= 2);
    assertTrue(custom.getEndTime() - custom.getStartTime() >= 2);
  }

  @Test
  public void testOperator() {
    PerformanceMetrics custom = PerformanceMetrics.createNew("TEST", null, null);

    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("{a:123}");
    PerformanceMetrics first = pipeline.getPerformanceMetrics();
    PerformanceMetrics last = first.createNext("CUSTOM OPERATION", null);
    Observable<JsonPipelineOutput> output = pipeline.getOutput()
        .lift(new Operator<JsonPipelineOutput, JsonPipelineOutput>() {
          @Override
          public Subscriber<? super JsonPipelineOutput> call(Subscriber<? super JsonPipelineOutput> s) {
            return new Subscriber<JsonPipelineOutput>() {

              @Override
              public void onCompleted() {
                s.onCompleted();
              }

              @Override
              public void onError(Throwable e) {
                Exceptions.throwIfFatal(e);
                s.onError(e);
              }

              @Override
              public void onNext(JsonPipelineOutput t) {
                try {
                  Thread.sleep(2);
                  custom.setStartTimestamp();
                  Thread.sleep(2);
                  custom.setEndTimestamp();
                  Thread.sleep(2);
                }
                catch (InterruptedException ex) {
                  custom.setEndTimestamp();
                }
                s.onNext(t);
              }
            };
          }

        }).doOnSubscribe(last.getStartAction()).doOnTerminate(last.getEndAction());

    assertStartMetrics(first);
    assertStartMetrics(last);
    assertStartMetrics(custom);

    output.toBlocking().single();

    assertFirstMetrics(first);
    assertNextMetrics(last);

    //    FastDateFormat format = FastDateFormat.getInstance("HH:mm:ss S");
    //    System.out.println(format.format(last.getStartTime()) + " - first subscription");
    //    System.out.println(format.format(first.getStartTime()) + " - second subscription");
    //    System.out.println(format.format(custom.getStartTime()) + " - custom subscription");
    //    System.out.println(format.format(custom.getEndTime()) + " - custom termination");
    //    System.out.println(format.format(first.getEndTime()) + " - second termination");
    //    System.out.println(format.format(last.getEndTime()) + " - first termination");


    assertTrue(custom.getStartTime() - last.getStartTime() >= 2);
    assertTrue(last.getEndTime() - custom.getEndTime() >= 2);
    assertTrue(custom.getEndTime() - custom.getStartTime() >= 2);
  }

  @Test
  public void testThreeSteps() {
    JsonPipelineImpl first = (JsonPipelineImpl)newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipelineImpl second = (JsonPipelineImpl)first.extract("$.a");
    JsonPipelineImpl third = (JsonPipelineImpl)second.extract("$.label");

    assertTrue(first.getJsonPipelineContext().isPerformanceMetricsEnabled());
    assertTrue(second.getJsonPipelineContext().isPerformanceMetricsEnabled());
    assertTrue(third.getJsonPipelineContext().isPerformanceMetricsEnabled());

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
    assertTrue(pipeline.getJsonPipelineContext().isPerformanceMetricsEnabled());
    PerformanceMetrics performanceMetrics = pipeline.getPerformanceMetrics();
    assertStartMetrics(performanceMetrics);

    toSubscribe.getJsonOutput().toBlocking().single();

    assertFirstMetrics(performanceMetrics);
  }

  private void assertNextPipelineMetrics(JsonPipelineImpl pipeline) {
    assertTrue(pipeline.getJsonPipelineContext().isPerformanceMetricsEnabled());
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
