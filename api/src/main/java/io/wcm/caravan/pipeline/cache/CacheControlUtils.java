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

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.observers.SafeSubscriber;

/**
 * Cache control utilities aid to manage pipeline output cache control meta data.
 */
public final class CacheControlUtils {

  private CacheControlUtils() {

  }

  /**
   * @param pipelineOutputs Pipeline outputs with {@code max-age} values
   * @return the lowest max-age value of all the given pipeline outputs
   */
  public static int getLowestMaxAge(Iterable<JsonPipelineOutput> pipelineOutputs) {

    return Observable
        .from(pipelineOutputs)
        .filter(output -> output != null)
        .map(output -> output.getMaxAge())
        .reduce(Math::min)
        .toBlocking().single();
  }

  /**
   * @param pipelineOutputs  Pipeline outputs with {@code max-age} values
   * @return the lowest max-age value of all the given pipeline outputs
   */
  public static int getLowestMaxAge(JsonPipelineOutput... pipelineOutputs) {
    return getLowestMaxAge(Arrays.asList(pipelineOutputs));
  }

  /**
   * Aggregates multiple resources fetched with different {@link JsonPipeline} instances, into a single
   * {@link JsonPipelineOutput} and ensures sure that the max-age Cache-Control-Header is set to the minimum value of
   * all aggregated responses.
   * @param pipelines an observable that emits MULTIPLE {@link JsonPipeline}s
   * @param zipFunc a lambda that is given the list of all {@link JsonPipelineOutput}s when they have been retrieved
   * @return a new observable that emits the aggregated JsonPipelineOutput with the correct max-age
   */
  public static Observable<JsonPipelineOutput> zipWithLowestMaxAge(
      Observable<JsonPipeline> pipelines,
      Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc) {

    Observable<Observable<JsonPipelineOutput>> outputs = pipelines.map(pipeline -> pipeline.getOutput());

    return zipWithLowestMaxAgeInternal(outputs, zipFunc);
  }

  /**
   * Aggregates multiple resources fetched with different {@link JsonPipeline} instances, into a single
   * {@link JsonPipelineOutput} and ensures sure that the max-age Cache-Control-Header is set to the minimum value of
   * all aggregated responses.
   * @param pipelines the {@link JsonPipeline}s to zip
   * @param zipFunc a lambda that is given the list of all {@link JsonPipelineOutput}s when they have been retrieved
   * @return a new observable that emits the aggregated JsonPipelineOutput with the correct max-age
   */
  public static Observable<JsonPipelineOutput> zipWithLowestMaxAge(
      Iterable<JsonPipeline> pipelines,
      Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc) {

    Observable<Observable<JsonPipelineOutput>> outputs = Observable.from(pipelines).map(pipeline -> pipeline.getOutput());

    return zipWithLowestMaxAgeInternal(outputs, zipFunc);
  }


  private static Observable<JsonPipelineOutput> zipWithLowestMaxAgeInternal(
      Observable<Observable<JsonPipelineOutput>> multipleOutputs,
      Func1<List<JsonPipelineOutput>, JsonPipelineOutput> zipFunc) {

    // this method could just return the zippingObservable created below, but this lead to an odd bug when this
    // method was called with an empty "multipleOutputs" observable. In that case, the OperatorZip called
    // #onCompleted twice (is this an implementation bug in rx.java?), which was causing weird behaviour
    // if #defaultIfEmpty was used on the zipped result (the default value was emitted twice)

    // that behaviour can be avoided by adding a SafeSubscriber layer

    return Observable.create(subscriber -> {

      Observable<JsonPipelineOutput> zippingObservable = Observable.zip(multipleOutputs, (arrayOfOutputs) -> {

        // collect the JsonPipelineOutput instances in a generic list (casting is required since FuncN only gives as a Object[])
        List<JsonPipelineOutput> listOfOutputs = new ArrayList<>(arrayOfOutputs.length);
        for (Object o : arrayOfOutputs) {
          listOfOutputs.add((JsonPipelineOutput)o);
        }

        // calculate the zipping function to produce the aggregated resposne from all JsonPipelineOutputs
        JsonPipelineOutput zippedOutput = zipFunc.call(listOfOutputs);

        // then update the max age of the overall output with the lowest max-age values of all outputs in the list
        int lowestMaxAge = getLowestMaxAge(listOfOutputs);
        return zippedOutput.withMaxAge(lowestMaxAge);

      });

      zippingObservable.subscribe(new SafeSubscriber<JsonPipelineOutput>(subscriber));
    });
  }
}
