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
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import rx.Observable;
import rx.functions.Func1;

@RunWith(MockitoJUnitRunner.class)
public class CacheControlUtilsTest {

  @Mock
  private Func1<List<JsonPipelineOutput>, JsonPipelineOutput> mockFunction;

  @Mock
  private JsonPipelineOutput functionResult;

  @Mock
  private JsonPipelineOutput lowestMAOutput;

  @Mock
  private JsonPipelineOutput nextMAOutput;

  @Mock
  private JsonPipelineOutput highestMAOutput;

  @Mock
  private JsonPipeline lowestMAPipeline;

  @Mock
  private JsonPipeline nextMAPipeline;

  @Mock
  private JsonPipeline highestMAPipeline;

  @Before
  public void setup() {
    when(lowestMAOutput.getMaxAge()).thenReturn(10);
    when(nextMAOutput.getMaxAge()).thenReturn(20);
    when(highestMAOutput.getMaxAge()).thenReturn(30);

    when(lowestMAPipeline.getOutput()).thenReturn(Observable.just(lowestMAOutput));
    when(nextMAPipeline.getOutput()).thenReturn(Observable.just(nextMAOutput));
    when(highestMAPipeline.getOutput()).thenReturn(Observable.just(highestMAOutput));

    when(mockFunction.call(any())).thenReturn(functionResult);
    when(functionResult.withMaxAge(10)).thenReturn(functionResult);
  }


  @Test
  public void testGetLowestMaxAgeFirstOfCollection() {
    // achieves the lowest maxAge value, found in the first list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(lowestMAOutput, nextMAOutput, highestMAOutput)));

  }

  @Test
  public void testGetLowestMaxAgeSecondOfCollection() {
    // achieves the lowest maxAge value, found in the second list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(nextMAOutput, lowestMAOutput, highestMAOutput)));
  }

  @Test
  public void testGetLowestMaxAgeThirdOfCollection() {
    // achieves the lowest maxAge value, found in the third list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(ImmutableList.of(highestMAOutput, nextMAOutput, lowestMAOutput)));
  }

  @Test
  public void testGetLowestMaxAgeWithNullElements() {
    List<JsonPipelineOutput> list = new ArrayList<JsonPipelineOutput>();
    list.add(null);
    list.add(null);
    list.add(highestMAOutput);

    // achieves the lowest maxAge value, null elements should be filtered
    assertEquals(30, CacheControlUtils.getLowestMaxAge(list));
  }

  @Test
  public void testGetLowestMaxAgeFirstofArray() {
    // achieves the lowest maxAge value, found in the first list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(lowestMAOutput, nextMAOutput, highestMAOutput));

  }

  @Test
  public void testGetLowestMaxAgeSecondofArray() {
    // achieves the lowest maxAge value, found in the second list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(nextMAOutput, lowestMAOutput, highestMAOutput));
  }

  @Test
  public void testGetLowestMaxAgeThirdofArray() {
    // achieves the lowest maxAge value, found in the third list element
    assertEquals(10, CacheControlUtils.getLowestMaxAge(highestMAOutput, nextMAOutput, lowestMAOutput));
  }

  @Test
  public void testGetLowestMaxAgeWithNullElementsofArray() {
    // achieves the lowest maxAge value, null elements should be filtered
    assertEquals(30, CacheControlUtils.getLowestMaxAge(null, null, highestMAOutput));
  }

  @Test
  public void testZipWithLowestMaxAgePipelineFirst() {
    // tests #zipWithLowestMaxAge(Iterable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the first element
    assertZipWithLowestMaxAgeList(lowestMAPipeline, nextMAPipeline, highestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAgePipelineSecond() {
    // tests #zipWithLowestMaxAge(Iterable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the second element
    assertZipWithLowestMaxAgeList(nextMAPipeline, lowestMAPipeline, highestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAgePipelineThird() {
    // tests #zipWithLowestMaxAge(Iterable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the third element
    assertZipWithLowestMaxAgeList(nextMAPipeline, highestMAPipeline, lowestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableFirst() {
    // tests #zipWithLowestMaxAge(Observable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the first element
    assertZipWithLowestMaxAgeObservable(lowestMAPipeline, nextMAPipeline, highestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableSecond() {
    // tests #zipWithLowestMaxAge(Observable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the second element
    assertZipWithLowestMaxAgeObservable(nextMAPipeline, lowestMAPipeline, highestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAgeObservableThird() {
    // tests #zipWithLowestMaxAge(Observable<JsonPipeline> pipelines, Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc)
    // achieves the pipelineOutput with the lowest maxAge value should be found in the third element
    assertZipWithLowestMaxAgeObservable(nextMAPipeline, highestMAPipeline, lowestMAPipeline);
  }

  @Test
  public void testZipWithLowestMaxAge_emptyList() {

    List<JsonPipeline> emptyListOfPipelines = ImmutableList.of();
    Observable<JsonPipelineOutput> zipped = CacheControlUtils.zipWithLowestMaxAge(emptyListOfPipelines, mockFunction);

    // zipping an empty list of pipelines should return an empty Observable (i.e. one that does not emit any JsonPipelineOutput)
    Iterator<JsonPipelineOutput> outputIterable = zipped.toBlocking().next().iterator();
    assertFalse(outputIterable.hasNext());

    // and the zip function should have never been called
    verifyZeroInteractions(mockFunction);
  }

  @Test
  public void testZipWithLowestMaxAge_emptyObservable() {

    Observable<JsonPipeline> emptyObservable = Observable.from(ImmutableList.of());
    Observable<JsonPipelineOutput> zipped = CacheControlUtils.zipWithLowestMaxAge(emptyObservable, mockFunction);

    // zipping an empty observable of pipelines should return an empty Observable (i.e. one that does not emit any JsonPipelineOutput)
    Iterator<JsonPipelineOutput> outputIterable = zipped.toBlocking().next().iterator();
    assertFalse(outputIterable.hasNext());

    // and the zip function should have never been called
    verifyZeroInteractions(mockFunction);
  }

  @Test
  public void testZipWithLowestMaxAge_emptyObservableWithDefault() {

    Observable<JsonPipeline> emptyObservable = Observable.from(ImmutableList.of());
    Observable<JsonPipelineOutput> zipped = CacheControlUtils.zipWithLowestMaxAge(emptyObservable, mockFunction);

    // if a default output is provided by using Obseravble#defaultIfEmpty, then a .toBlicking().single() should
    // succeed and return the specified fallback output
    JsonPipelineOutput fallbackOutput = Mockito.mock(JsonPipelineOutput.class);
    JsonPipelineOutput output = zipped.defaultIfEmpty(fallbackOutput).toBlocking().single();

    assertEquals(fallbackOutput, output);
  }

  private void assertZipWithLowestMaxAgeList(JsonPipeline first, JsonPipeline second, JsonPipeline third) {
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(ImmutableList.of(first, second, third),
        mockFunction);
    assertZippedObservable(outputObservable);
  }

  private void assertZipWithLowestMaxAgeObservable(JsonPipeline first, JsonPipeline second, JsonPipeline third) {
    Observable<JsonPipelineOutput> outputObservable = CacheControlUtils.zipWithLowestMaxAge(Observable.just(first, second, third),
        mockFunction);
    assertZippedObservable(outputObservable);
  }

  private void assertZippedObservable(Observable<JsonPipelineOutput> outputObservable) {
    JsonPipelineOutput output = outputObservable.toBlocking().single();

    // achieves mock pipelineOutput "functionResult" called with the lowest maxAge value
    verify(functionResult, times(1)).withMaxAge(10);

    // achieves mock pipelineOutput "functionResult" is returned from Observable
    assertEquals(functionResult, output);
  }

}
