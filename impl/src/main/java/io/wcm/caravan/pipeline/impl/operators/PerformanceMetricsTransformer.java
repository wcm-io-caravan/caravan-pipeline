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

import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

/**
 * Prototype for a more rx-java like way to measure how much time a Transformer needs for subscription and handling of
 * emissions
 */
final class PerformanceMetricsTransformer implements Transformer<JsonPipelineOutput, JsonPipelineOutput> {

  private final Transformer<JsonPipelineOutput, JsonPipelineOutput> toMeasure;

  private final Stopwatch subscriptionStopwatch;
  private final Stopwatch emissionStopwatch;

  public PerformanceMetricsTransformer(Transformer<JsonPipelineOutput, JsonPipelineOutput> toMeasure, Ticker ticker) {
    super();
    this.toMeasure = toMeasure;

    subscriptionStopwatch = Stopwatch.createUnstarted(ticker);
    emissionStopwatch = Stopwatch.createUnstarted(ticker);
  }

  @Override
  public Observable<JsonPipelineOutput> call(Observable<JsonPipelineOutput> dataSource) {

    Observable<JsonPipelineOutput> wrappedInputObservable = Observable.create(new OnSubscribe<JsonPipelineOutput>() {

      @Override
      public void call(Subscriber<? super JsonPipelineOutput> subscriberToMeasure) {

        subscriptionStopwatch.stop();

        dataSource.subscribe(new Observer<JsonPipelineOutput>() {

          @Override
          public void onCompleted() {
            subscriberToMeasure.onCompleted();
          }

          @Override
          public void onError(Throwable e) {
            subscriberToMeasure.onError(e);
          }

          @Override
          public void onNext(JsonPipelineOutput t) {
            emissionStopwatch.start();
            subscriberToMeasure.onNext(t);
          }
        });

      }
    });

    Observable<JsonPipelineOutput> wrappedOutputObservable = Observable.create(new OnSubscribe<JsonPipelineOutput>() {

      @Override
      public void call(Subscriber<? super JsonPipelineOutput> actualOutputSubscriber) {

        Observable<JsonPipelineOutput> observableToMeasure = toMeasure.call(wrappedInputObservable);

        subscriptionStopwatch.start();
        observableToMeasure.subscribe(new Observer<JsonPipelineOutput>() {

          @Override
          public void onCompleted() {
            actualOutputSubscriber.onCompleted();
          }

          @Override
          public void onError(Throwable e) {
            actualOutputSubscriber.onError(e);
          }

          @Override
          public void onNext(JsonPipelineOutput t) {
            emissionStopwatch.elapsed(TimeUnit.MILLISECONDS);
            actualOutputSubscriber.onNext(t);
          }
        });
      }
    });

    return wrappedOutputObservable;
  }
}