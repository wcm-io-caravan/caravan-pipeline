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
package io.wcm.caravan.pipeline.cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;
import rx.functions.Func1;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class CacheControlUtilsTest {

  @Mock
  private Func1<List<JsonPipelineOutput>, JsonPipelineOutput> mockFunction;

  @Mock
  private JsonPipelineOutput functionResult;

  @Mock
  private JsonPipelineOutput firstOutput;

  @Mock
  private JsonPipelineOutput secondOutput;

  @Mock
  private JsonPipelineOutput thirdOutput;

  @Mock
  private JsonPipeline firstPipeline;

  @Mock
  private JsonPipeline secondPipeline;

  @Mock
  private JsonPipeline thirdPipeline;


  @Test
  public void testGetLowestMaxAgeFirst() {
    when(firstOutput.getMaxAge()).thenReturn(10);
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdOutput.getMaxAge()).thenReturn(30);

    // achieves the lowest maxAge value
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(firstOutput, secondOutput, thirdOutput)));

  }

  @Test
  public void testGetLowestMaxAgeSecond() {
    when(firstOutput.getMaxAge()).thenReturn(20);
    when(secondOutput.getMaxAge()).thenReturn(10);
    when(thirdOutput.getMaxAge()).thenReturn(30);

    // achieves the lowest maxAge value
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(firstOutput, secondOutput, thirdOutput)));
  }

  @Test
  public void testGetLowestMaxAgeThird() {
    when(firstOutput.getMaxAge()).thenReturn(30);
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdOutput.getMaxAge()).thenReturn(10);

    // achieves the lowest maxAge value
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(firstOutput, secondOutput, thirdOutput)));
  }

  @Test
  public void testZipWithLowestMaxAgePipelineFirst() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(10);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(30);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(ImmutableList.of(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

  @Test
  public void testZipWithLowestMaxAgePipelineSecond() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(20);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(10);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(30);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(ImmutableList.of(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

  @Test
  public void testZipWithLowestMaxAgePipelineThird() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(30);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(10);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(ImmutableList.of(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableFirst() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(10);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(30);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(Observable.just(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableSecond() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(20);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(10);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(30);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(Observable.just(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableThird() {
    when(firstPipeline.getOutput()).thenReturn(Observable.just(firstOutput));
    when(firstOutput.getMaxAge()).thenReturn(30);
    when(secondPipeline.getOutput()).thenReturn(Observable.just(secondOutput));
    when(secondOutput.getMaxAge()).thenReturn(20);
    when(thirdPipeline.getOutput()).thenReturn(Observable.just(thirdOutput));
    when(thirdOutput.getMaxAge()).thenReturn(10);
    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(Observable.just(firstPipeline, secondPipeline, thirdPipeline),
        mockFunction);
    JsonPipelineOutput output = outputObservable.toBlocking().single();
    assertEquals(functionResult, output);
  }

}
