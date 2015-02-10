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

import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;
import rx.Observable;
import rx.Subscriber;
import rx.Observable.Operator;
import rx.exceptions.Exceptions;

/**
 * An operator that delegates all non-fatal exception-handling to the given function, allowing the user of the
 * pipeline to specify fallback content for certain expected exception scenarios.
 */
public class HandleExceptionOperator implements Operator<JsonPipelineOutput, JsonPipelineOutput> {

  private final JsonPipelineExceptionHandler handler;

  /**
   * @param handler the function to call when an exception is caught
   */
  public HandleExceptionOperator(JsonPipelineExceptionHandler handler) {
    super();
    this.handler = handler;
  }

  @Override
  public Subscriber<? super JsonPipelineOutput> call(Subscriber<? super JsonPipelineOutput> subscriber) {
    return new Subscriber<JsonPipelineOutput>() {

      @Override
      public void onCompleted() {
        subscriber.onCompleted();
      }

      @Override
      public void onError(Throwable e) {

        Exceptions.throwIfFatal(e);

        // extract the HTTP status code from the exceptions known to contain such information
        int statusCode = 500;
        if (e instanceof JsonPipelineInputException) {
          statusCode = ((JsonPipelineInputException)e).getStatusCode();
        }
        if (e instanceof IllegalResponseRuntimeException) {
          statusCode = ((IllegalResponseRuntimeException)e).getResponseStatusCode();
        }

        JsonPipelineOutput defaultFallbackContent = new JsonPipelineOutputImpl(JacksonFunctions.emptyObject()).withStatusCode(statusCode).withMaxAge(0);
        try {
          Observable<JsonPipelineOutput> fallbackResponse = handler.call(defaultFallbackContent, (RuntimeException)e);

          fallbackResponse.subscribe(subscriber);
        }
        catch (Throwable rethrown) {
          subscriber.onError(rethrown);
        }
      }

      @Override
      public void onNext(JsonPipelineOutput output) {
        subscriber.onNext(output);
      }
    };
  }
}