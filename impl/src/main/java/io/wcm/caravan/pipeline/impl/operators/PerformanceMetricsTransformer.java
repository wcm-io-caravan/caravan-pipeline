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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;

/**
 * A class that measures the time spent by a specific {@link Transformer}, {@link Operator} or {@link Observable}.
 * Usage of this class is limited to observables with a single emission
 */
public final class PerformanceMetricsTransformer<T> implements Transformer<T, T> {

  private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsTransformer.class);

  private final Transformer<T, T> toMeasure;

  private final Stopwatch subscriptionStopwatch;
  private final Stopwatch observationStopwatch;
  private final Stopwatch emissionStopwatch;

  /**
   * constructor for unit-tests that allows you to specify a custom ticker. For general usage use
   * {@link #withSystemTicker(Transformer)}
   * @param toMeasure the transformer that should be measured
   * @param ticker the time source
   */
  PerformanceMetricsTransformer(Transformer<T, T> toMeasure, Ticker ticker) {
    super();
    this.toMeasure = toMeasure;

    subscriptionStopwatch = Stopwatch.createUnstarted(ticker);
    observationStopwatch = Stopwatch.createUnstarted(ticker);
    emissionStopwatch = Stopwatch.createUnstarted(ticker);
  }

  /**
   * constructor for unit-tests that allows you to specify a custom ticker. For general usage use
   * {@link #withSystemTicker(Operator)}
   * @param toMeasure the operator that should be measured
   * @param ticker the time source
   */
  PerformanceMetricsTransformer(Operator<T, T> toMeasure, Ticker ticker) {

    // if the subject to be measured is an operator we wrap it in a trivial transformer
    this(new Transformer<T, T>() {

      @Override
      public Observable<T> call(Observable<T> sourceObservable) {
        return Observable.create(subscriber -> {
          // apply the operator to measure to the source observable before subscribing
          sourceObservable.lift(toMeasure).subscribe(subscriber);
        });
      }

    }, ticker);
  }

  /**
   * constructor for unit-tests that allows you to specify a custom ticker. For general usage use
   * {@link #withSystemTicker(Observable)}
   * @param toMeasure the operator that should be measured
   * @param ticker the time source
   */
  PerformanceMetricsTransformer(Observable<T> toMeasure, Ticker ticker) {

    // if the subject to be measured is an operator we wrap it in a trivial transformer
    this(new Transformer<T, T>() {

      @Override
      public Observable<T> call(Observable<T> sourceObservable) {
        return sourceObservable;
      }
    }, ticker);

  }

  /**
   * @param toMeasure the transformer that should be measured
   * @param <T> type
   * @return a PerformanceMetricsTransformer that uses the {@link Ticker#systemTicker()}
   */
  public static <T> PerformanceMetricsTransformer<T> withSystemTicker(Transformer<T, T> toMeasure) {
    return new PerformanceMetricsTransformer<T>(toMeasure, Ticker.systemTicker());
  }

  /**
   * @param toMeasure the operator that should be measured
   * @param <T> type
   * @return a PerformanceMetricsTransformer that uses the {@link Ticker#systemTicker()}
   */
  public static <T> PerformanceMetricsTransformer<T> withSystemTicker(Operator<T, T> toMeasure) {
    return new PerformanceMetricsTransformer<T>(toMeasure, Ticker.systemTicker());
  }

  /**
   * @param toMeasure the observable that should be measured
   * @param <T> type
   * @return a PerformanceMetricsTransformer that uses the {@link Ticker#systemTicker()}
   */
  public static <T> PerformanceMetricsTransformer<T> withSystemTicker(Observable<T> toMeasure) {
    return new PerformanceMetricsTransformer<T>(toMeasure, Ticker.systemTicker());
  }

  @Override
  public Observable<T> call(Observable<T> dataSource) {

    Observable<T> wrappedInputObservable = Observable.create(subscriberToMeasure -> {

      // 3. this is called after the subscriber to measure has finished its job and subscribes to this input observable
      if (subscriptionStopwatch.isRunning()) {
        subscriptionStopwatch.stop();
      }

      // 4. we now start the stop watch that measures how long the actual data source takes between subscription and emission
      observationStopwatch.start();
      dataSource.subscribe(new Observer<T>() {

        @Override
        public void onCompleted() {
          subscriberToMeasure.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          subscriberToMeasure.onError(e);
        }

        @Override
        public void onNext(T emission) {

          // 5. the source data is available, we now stop the observation stop watch
          if (observationStopwatch.isRunning()) {
            observationStopwatch.stop();
          }

          // 6. now start the stop watch to measure the time it takes for the transfomer to handle the data
          emissionStopwatch.start();
          subscriberToMeasure.onNext(emission);
        }
      });
    });

    Observable<T> wrappedOutputObservable = Observable.create(actualOutputSubscriber -> {

      Observable<T> observableToMeasure = toMeasure.call(wrappedInputObservable);

      // 1. start the subscription stop watch
      subscriptionStopwatch.start();

      // 2. subscribing here will call onSubscribe function of the transformer that we want to measure
      observableToMeasure.subscribe(new Observer<T>() {

        @Override
        public void onCompleted() {
          actualOutputSubscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          actualOutputSubscriber.onError(e);
        }

        @Override
        public void onNext(T emission) {
          // 7. the transformer to measure has finished processing the data, so we stop the emission stop watch
          if (emissionStopwatch.isRunning()) {
            emissionStopwatch.stop();
          }

          log.info("subscription=" + getSubscriptionMillis() + "ms, observation=" + getObservationMillis() + "ms, emission: " + getEmissionMillis() + "ms");

          actualOutputSubscriber.onNext(emission);
        }
      });
    });

    return wrappedOutputObservable.cache();
  }


  /**
   * Calculate the time spent during the {@link OnSubscribe} function of the {@link Transformer} to measure. Will be 0
   * if you are using this class to measure a {@link Operator} or {@link Observable}
   * @return the time in milliseconds spent subscribing onto the source observable
   */
  public long getSubscriptionMillis() {
    return this.subscriptionStopwatch.elapsed(TimeUnit.MILLISECONDS);
  }

  /**
   * Calculate the time spent waiting for the source observable to emit the result
   * @return the time in milliseconds spent waiting for the source observable
   */
  public long getObservationMillis() {
    return this.observationStopwatch.elapsed(TimeUnit.MILLISECONDS);
  }

  /**
   * Calculate the time spent processing the emission of the source observable
   * @return the time in milliseconds spent processing the emission
   */
  public long getEmissionMillis() {
    return this.emissionStopwatch.elapsed(TimeUnit.MILLISECONDS);
  }
}
