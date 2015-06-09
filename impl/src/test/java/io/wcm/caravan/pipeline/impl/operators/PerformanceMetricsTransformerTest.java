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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import com.google.common.base.Ticker;


public class PerformanceMetricsTransformerTest {

  private static final String EMISSION_RESULT = "Hello world!";

  @Test
  public void test_withDelayingTransformer() throws Exception {

    int subscriptionDelayMillis = 1000;
    long observationDelayMillis = 750;
    int emissionDelayMillis = 500;

    // use a custom ticker that can simulate waiting time
    SimulationTicker ticker = new SimulationTicker();

    // create a transformer that simulates two delay: one during subscription and one during emission of events
    DelayingTransformer transformer = new DelayingTransformer(ticker, subscriptionDelayMillis, emissionDelayMillis);

    // wrap this transformer with a PerformanceMetricsTransformer that measures the time spend during subscription and emission
    PerformanceMetricsTransformer<String> performanceMetricsTransformer = new PerformanceMetricsTransformer<>(transformer, ticker);

    // use a simple source observable that emits a single string after a delay of 100ms
    Observable<String> sourceObservable = createDelayingObservable(ticker, observationDelayMillis);

    // transform it to an observable that applies the delay and performance metrics transformations
    Observable<String> performanceMetricsObservable = performanceMetricsTransformer.call(sourceObservable);

    // subscribe to the wrapped observable and wait for the result emission
    String result = performanceMetricsObservable.toBlocking().single();

    // make sure that the emission matches that of the source observable
    assertEquals(EMISSION_RESULT, result);

    // check that the delays have been correctly captured by the stopwatch
    assertEquals(transformer.subscriptionDelayMillis, performanceMetricsTransformer.getSubscriptionMillis());
    assertEquals(observationDelayMillis, performanceMetricsTransformer.getObservationMillis());
    assertEquals(transformer.emissionDelayMillis, performanceMetricsTransformer.getEmissionMillis());
  }

  @Test
  public void test_withDelayingOperator() throws Exception {

    long observationDelayMillis = 750;
    int emissionDelayMillis = 500;

    // use a custom ticker that can simulate waiting time
    SimulationTicker ticker = new SimulationTicker();

    // create a operator that simulates a delay during emission of events
    DelayingOperator operator = new DelayingOperator(ticker, emissionDelayMillis);

    // wrap this operator with a PerformanceMetricsTransformer that measures the time spend during subscription and emission
    PerformanceMetricsTransformer<String> performanceMetricsTransformer = new PerformanceMetricsTransformer<>(operator, ticker);

    // use a simple source observable that emits a single string after the specified delay
    Observable<String> sourceObservable = createDelayingObservable(ticker, observationDelayMillis);

    // transform it to an observable that applies the delay and performance metrics transformations
    Observable<String> performanceMetricsObservable = performanceMetricsTransformer.call(sourceObservable);

    // subscribe to the wrapped observable and wait for the result emission
    String result = performanceMetricsObservable.toBlocking().single();

    // make sure that the emission matches that of the source observable
    assertEquals(EMISSION_RESULT, result);

    // check that the delays have been correctly captured by the stopwatch
    assertEquals(0, performanceMetricsTransformer.getSubscriptionMillis());
    assertEquals(observationDelayMillis, performanceMetricsTransformer.getObservationMillis());
    assertEquals(operator.emissionDelayMillis, performanceMetricsTransformer.getEmissionMillis());
  }


  @Test
  public void test_withDelayingObservable() throws Exception {

    long observationDelayMillis = 750;

    // use a custom ticker that can simulate waiting time
    SimulationTicker ticker = new SimulationTicker();

    // use a simple source observable that emits a single string after the specified delay
    Observable<String> sourceObservable = createDelayingObservable(ticker, observationDelayMillis);

    // wrap this observable with a PerformanceMetricsTransformer that measures the time spend during subscription and emission
    PerformanceMetricsTransformer<String> performanceMetricsTransformer = new PerformanceMetricsTransformer<>(sourceObservable, ticker);
    Observable<String> performanceMetricsObservable = performanceMetricsTransformer.call(sourceObservable);

    // subscribe to the wrapped observable and wait for the result emission
    String result = performanceMetricsObservable.toBlocking().single();

    // make sure that the emission matches that of the source observable
    assertEquals(EMISSION_RESULT, result);

    // check that the subscription and emission delays have been correctly captured by the stopwatch
    assertEquals(0, performanceMetricsTransformer.getSubscriptionMillis());
    assertEquals(observationDelayMillis, performanceMetricsTransformer.getObservationMillis());
    assertEquals(0, performanceMetricsTransformer.getEmissionMillis());
  }

  public Observable<String> createDelayingObservable(SimulationTicker ticker, long observationDelayMillis) {
    // create an observable that simulates a delay before it emits events
    Observable<String> sourceObservable = Observable.create(subscriber -> {

      ticker.simulateWait(observationDelayMillis);

      // we use a scheduler here, so that the delay is not added during subscription
      Schedulers.computation().createWorker().schedule(() -> {

        subscriber.onNext(EMISSION_RESULT);
        subscriber.onCompleted();
      });

    });
    return sourceObservable;
  }

  /**
   * A ticker that only increases the current time when {@link #simulateWait(long)} is called
   */
  static class SimulationTicker extends Ticker {

    private long nanos;

    void simulateWait(long millis) {
      nanos += TimeUnit.MILLISECONDS.toNanos(millis);
    }

    @Override
    public long read() {
      return nanos;
    }

  }

  /**
   * a transformer that increases the time of the given ticker by the specified amounts during subscription and emission
   */
  static class DelayingTransformer implements Transformer<String, String> {

    private final SimulationTicker ticker;

    private final long subscriptionDelayMillis;
    private final long emissionDelayMillis;

    DelayingTransformer(SimulationTicker ticker, long subscriptionDelayMillis, long emissionDelayMillis) {
      this.ticker = ticker;
      this.subscriptionDelayMillis = subscriptionDelayMillis;
      this.emissionDelayMillis = emissionDelayMillis;
    }

    @Override
    public Observable<String> call(Observable<String> sourceObservable) {

      // define the output observable by implementing a OnSubscribe function
      return Observable.create(subscriber -> {

        // when a subscriber subscribes, we simulate a delay by advancing the clock
        ticker.simulateWait(subscriptionDelayMillis);

        // then add a new subscriber to the source observable that allows us to advancethe clock before forwarding the emission
        sourceObservable.subscribe(new Observer<String>() {

          @Override
          public void onCompleted() {
            subscriber.onCompleted();
          }

          @Override
          public void onError(Throwable e) {
            subscriber.onError(e);
          }

          @Override
          public void onNext(String emission) {
            // simulate a delay before the emission is forwarded to the actual subscriber
            ticker.simulateWait(emissionDelayMillis);
            subscriber.onNext(emission);
          }
        });

      });
    }
  }

  /**
   * an operator that increases the time of the given ticker by the specified amounts before it forward the emissions
   */
  static class DelayingOperator implements Operator<String, String> {

    private final SimulationTicker ticker;

    private final long emissionDelayMillis;

    DelayingOperator(SimulationTicker ticker, long emissionDelayMillis) {
      this.ticker = ticker;
      this.emissionDelayMillis = emissionDelayMillis;
    }

    @Override
    public Subscriber<? super String> call(Subscriber<? super String> subscriber) {
      return new Subscriber<String>() {

        @Override
        public void onCompleted() {
          subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
          subscriber.onError(e);
        }

        @Override
        public void onNext(String emission) {
          // simulate a delay before the emission is forwarded to the actual subscriber
          ticker.simulateWait(emissionDelayMillis);
          subscriber.onNext(emission);
        }

      };
    }
  }
}
