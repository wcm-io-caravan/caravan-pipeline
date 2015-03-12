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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineMultipleSubscribtionsTest extends AbstractJsonPipelineTest {

  @Mock
  protected JsonPipelineAction action;

  public JsonPipelineMultipleSubscribtionsTest() {
    super();
  }

  @Test
  public void twoSubscriptions() {
    JsonPipeline firstStep = newPipelineWithResponseBody("{id:123}");
    assertNotNull(firstStep);
    Observable<JsonPipelineOutput> fristSubscriber = firstStep.getOutput();
    Observable<JsonPipelineOutput> secondSubscriber = firstStep.getOutput();
    assertFalse(fristSubscriber.equals(secondSubscriber));
    assertFalse(fristSubscriber.toBlocking().equals(secondSubscriber.toBlocking()));
    assertFalse(fristSubscriber.toBlocking().first().equals(secondSubscriber.toBlocking().first()));
  }

  @Test
  public void concurrentSubscriptions() {
    JsonPipeline pipeline = newPipelineWithResponseBody("{id:123}");
    ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
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

          try {
            JSONAssert.assertEquals("{id: 123}", pipeline.getStringOutput().toBlocking().first(), JSONCompareMode.STRICT);
            queue.add(pipeline.getStringOutput().toBlocking().first());
          }
          catch (JSONException ex) {
            ex.printStackTrace();
          }
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
    assertEquals(queue.size(), 100);
  }

  @Test
  public void testTwoPipelineActionCalls() {
    JsonPipeline firstStep = newPipelineWithResponseBody("{id:123}");
    JsonPipeline nextStep = firstStep.applyAction(action);
    when(action.execute(any())).thenReturn(firstStep.getOutput());
    Observable<JsonPipelineOutput> fristSubscriber = nextStep.getOutput();
    Observable<JsonPipelineOutput> secondSubscriber = nextStep.getOutput();
    fristSubscriber.toBlocking().single();
    secondSubscriber.toBlocking().single();
    verify(action, times(2)).execute(any());
  }

  @Test
  public void testConcurrentPipelineActionCalls() {
    JsonPipeline firstStep = newPipelineWithResponseBody("{id:123}");
    JsonPipeline nextStep = firstStep.applyAction(action);
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
          nextStep.getOutput().toBlocking().single();
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
    verify(action, times(100)).execute(any());
  }
}

