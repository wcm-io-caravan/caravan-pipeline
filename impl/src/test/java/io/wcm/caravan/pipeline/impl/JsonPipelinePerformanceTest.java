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

import static org.junit.Assert.assertTrue;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import rx.functions.Action0;
import rx.functions.Action1;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelinePerformanceTest extends AbstractJsonPipelineTest {

  public JsonPipelinePerformanceTest() {
    super();
  }


  @Override
  protected JsonPipelineContextImpl getJsonPipelineContext() {
    return new JsonPipelineContextImpl(jsonPipelineFactory, cacheAdapter, metricRegistry, getContextProperties(), true);
  }

  @Test
  public void testOnNext() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("");
    JsonPipelineImpl next1 = pipeline.cloneWith(pipeline.getOutput(), "suffix1", "action1");
    JsonPipelineImpl next2 = next1.cloneWith(next1.getOutput().doOnNext(new Action1<JsonPipelineOutput>() {

      @Override
      public void call(JsonPipelineOutput t) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }), "suffix2", "action2");

    next2.getOutput().toBlocking().first();
    assertTrue(next1.getPerformanceMetrics().getTakenTimeByStep() >= 100 && next1.getPerformanceMetrics().getTakenTimeByStep() <= 102);
    assertTrue(next2.getPerformanceMetrics().getTakenTimeByStep() >= 0 && next2.getPerformanceMetrics().getTakenTimeByStep() <= 2);
  }


  @Test
  public void testOnSubscribe() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("");
    JsonPipelineImpl next1 = pipeline.cloneWith(pipeline.getOutput(), "suffix1", "action1");
    JsonPipelineImpl next2 = next1.cloneWith(next1.getOutput().doOnSubscribe(new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }), "suffix2", "action2");

    next2.getOutput().toBlocking().first();
    assertTrue(next1.getPerformanceMetrics().getTakenTimeByStep() >= 0 && next1.getPerformanceMetrics().getTakenTimeByStep() <= 2);
    assertTrue(next2.getPerformanceMetrics().getTakenTimeByStep() >= 100 && next2.getPerformanceMetrics().getTakenTimeByStep() <= 102);
  }

  @Test
  public void testOnTerminate() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("");
    JsonPipelineImpl next1 = pipeline.cloneWith(pipeline.getOutput(), "suffix1", "action1");
    JsonPipelineImpl next2 = next1.cloneWith(next1.getOutput().doOnTerminate(new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }), "suffix2", "action2");
    next2.getOutput().toBlocking().first();
    assertTrue(next1.getPerformanceMetrics().getTakenTimeByStep() >= 0 && next1.getPerformanceMetrics().getTakenTimeByStep() <= 2);
    assertTrue(next2.getPerformanceMetrics().getTakenTimeByStep() >= 100 && next2.getPerformanceMetrics().getTakenTimeByStep() <= 102);
  }

  @Test
  public void testOnCompleted() {
    JsonPipelineImpl pipeline = (JsonPipelineImpl)newPipelineWithResponseBody("");
    JsonPipelineImpl next1 = pipeline.cloneWith(pipeline.getOutput(), "suffix1", "action1");
    JsonPipelineImpl next2 = next1.cloneWith(next1.getOutput().doOnCompleted(new Action0() {

      @Override
      public void call() {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex) {
          ex.printStackTrace();
        }

      }

    }), "suffix2", "action2");

    next2.getOutput().toBlocking().first();
    assertTrue(next1.getPerformanceMetrics().getTakenTimeByStep() >= 0 && next1.getPerformanceMetrics().getTakenTimeByStep() <= 2);
    assertTrue(next2.getPerformanceMetrics().getTakenTimeByStep() >= 100 && next2.getPerformanceMetrics().getTakenTimeByStep() <= 102);
  }


}
