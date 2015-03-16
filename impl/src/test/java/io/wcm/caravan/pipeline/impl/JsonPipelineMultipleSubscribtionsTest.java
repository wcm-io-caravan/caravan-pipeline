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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineMultipleSubscribtionsTest extends AbstractJsonPipelineTest {

  private JsonPipeline firstStep;

  private JsonPipeline secondStep;

  private JsonPipeline thirdStep;

  @Mock
  protected JsonPipelineAction action;


  public JsonPipelineMultipleSubscribtionsTest() {
    super();
  }

  @Test
  public void twoSubscriptions() {
    firstStep = newPipelineWithResponseBody("{id:123}");
    assertNotNull(firstStep);

    // if you call #getOutput twice, you get a different observable on each call
    Observable<JsonPipelineOutput> firstOutput = firstStep.getOutput();
    Observable<JsonPipelineOutput> secondOutput = firstStep.getOutput();
    assertFalse(firstOutput.equals(secondOutput));

    // but both observables should emit the same JsonPipelineOutput instance
    assertTrue(firstOutput.toBlocking().first().equals(secondOutput.toBlocking().first()));
  }

  @Test
  public void concurrentSubscriptions() throws InterruptedException, JSONException {
    firstStep = newPipelineWithResponseBody("{id:123}");

    // use a synchronized set to collect the pipeline output from multiple threads
    Set<JsonPipelineOutput> distinctOutputs = Collections.synchronizedSet(new HashSet<JsonPipelineOutput>());

    ExecutorService executorService = Executors.newCachedThreadPool();
    CountDownLatch countDown = new CountDownLatch(100);

    while (countDown.getCount() > 0) {

      executorService.submit(() -> {
        // wait until all executor threads have been started before accessing the pipeline output
        countDown.await();
        distinctOutputs.add(firstStep.getOutput().toBlocking().single());

        return null; // this is required for the lambda to be considered a Callable<Void> and therefore be allowed to throw exceptions
      });

      countDown.countDown();
    }

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    // ensure all threads received the same JsonPipelineOutput instance with the expected JSON output
    assertEquals(1, distinctOutputs.size());
    JSONAssert.assertEquals("{id: 123}", firstStep.getStringOutput().toBlocking().first(), JSONCompareMode.STRICT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTwoPipelineActionCalls() {
    firstStep = newPipelineWithResponseBody("{id:123}");
    secondStep = firstStep.applyAction(action);
    when(action.execute(any())).thenReturn(firstStep.getOutput());
    Observer<JsonPipelineOutput> firstObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> secondObserver = Mockito.mock(Observer.class);
    secondStep.getOutput().subscribe(firstObserver);
    secondStep.getOutput().subscribe(secondObserver);
    verify(action, times(1)).execute(any());
  }

  @Test
  public void testConcurrentPipelineActionCalls() {
    firstStep = newPipelineWithResponseBody("{id:123}");
    secondStep = firstStep.applyAction(action);
    when(action.execute(any())).thenReturn(firstStep.getOutput());
    CountDownLatch countDown = new CountDownLatch(100);
    ExecutorService executorService = Executors.newCachedThreadPool();
    for (int i = 0; i < 100; i++) {
      executorService.execute(new Runnable() {

        @Override
        public void run() {
          try {
            countDown.await();
          }
          catch (InterruptedException ex) {
            ex.printStackTrace();
          }
          @SuppressWarnings("unchecked")
          Observer<JsonPipelineOutput> observer = Mockito.mock(Observer.class);
          secondStep.getOutput().subscribe(observer);
        }
      });
      countDown.countDown();
    }
    executorService.shutdown();
    try {
      while (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
        // wait until all thread are terminated
      }
    }
    catch (InterruptedException ex) {
      ex.printStackTrace();
    }
    verify(action, times(1)).execute(any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test3StepPipelineActionCalls() {

    final AtomicInteger subscribeCount = new AtomicInteger();
    initPipelines(subscribeCount);

    Observer<JsonPipelineOutput> firstObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> secondObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> thirdObserver = Mockito.mock(Observer.class);

    firstStep.getOutput().subscribe(firstObserver);
    secondStep.getOutput().subscribe(secondObserver);
    thirdStep.getOutput().subscribe(thirdObserver);

    assertEquals(1, subscribeCount.get());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test3StepPipelineActionCallsReversedOrder() {

    final AtomicInteger subscribeCount = new AtomicInteger();
    initPipelines(subscribeCount);

    Observer<JsonPipelineOutput> firstObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> secondObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> thirdObserver = Mockito.mock(Observer.class);

    thirdStep.getOutput().subscribe(thirdObserver);
    secondStep.getOutput().subscribe(secondObserver);
    firstStep.getOutput().subscribe(firstObserver);

    assertEquals(1, subscribeCount.get());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test3StepPipelineActionCallsMixedOrder() {

    final AtomicInteger subscribeCount = new AtomicInteger();
    initPipelines(subscribeCount);

    Observer<JsonPipelineOutput> firstObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> secondObserver = Mockito.mock(Observer.class);
    Observer<JsonPipelineOutput> thirdObserver = Mockito.mock(Observer.class);

    secondStep.getOutput().subscribe(secondObserver);
    firstStep.getOutput().subscribe(firstObserver);
    thirdStep.getOutput().subscribe(thirdObserver);

    assertEquals(1, subscribeCount.get());
  }

  private void initPipelines(AtomicInteger subscribeCount) {
    Observable<CaravanHttpResponse> sourceObservable = Observable.create(new OnSubscribe<CaravanHttpResponse>() {

      @Override
      public void call(Subscriber<? super CaravanHttpResponse> t1) {
        subscribeCount.incrementAndGet();
        t1.onNext(getJsonResponse(200, "{}", 0));
        t1.onCompleted();
      }
    });

    firstStep = new JsonPipelineImpl(new CaravanHttpRequestBuilder().build(), sourceObservable, caching, metricRegistry);
    secondStep = firstStep.applyAction(action);
    thirdStep = secondStep.merge(newPipelineWithResponseBody("{name:'abc'}"));
    when(action.execute(any())).thenReturn(firstStep.getOutput());
  }
}

