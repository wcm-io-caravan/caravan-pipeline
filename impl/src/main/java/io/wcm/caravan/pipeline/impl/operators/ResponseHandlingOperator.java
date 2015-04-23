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

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

/**
 * Operator that converts {@link Response} emissions from the {@link CaravanHttpClient} layer into
 * {@link JsonPipelineOutput} objects. All recoverable exceptions are wrapped in a {@link JsonPipelineInputException}
 * before they are forwarded to the subscriber's onNext method
 */
public class ResponseHandlingOperator implements Operator<JsonPipelineOutput, CaravanHttpResponse> {

  private static final Logger log = LoggerFactory.getLogger(ResponseHandlingOperator.class);

  private final CaravanHttpRequest request;

  /**
   * @param request the URL of the outgoing request
   */
  public ResponseHandlingOperator(CaravanHttpRequest request) {
    this.request = request;
  }

  @Override
  public Subscriber<? super CaravanHttpResponse> call(Subscriber<? super JsonPipelineOutput> subscriber) {
    return new Subscriber<CaravanHttpResponse>() {

      @Override
      public void onCompleted() {
        subscriber.onCompleted();
      }

      @Override
      public void onError(Throwable e) {
        Exceptions.throwIfFatal(e);

        int statusCode = 500;
        if (e instanceof IllegalResponseRuntimeException) {
          statusCode = ((IllegalResponseRuntimeException)e).getResponseStatusCode();
        }

        subscriber.onError(new JsonPipelineInputException(statusCode, "Failed to GET " + request.getUrl(), e));
      }

      @Override
      public void onNext(CaravanHttpResponse response) {
        try {
          final int statusCode = response.status();
          log.debug("received " + statusCode + " response (" + response.reason() + ") with from " + request.getUrl());
          if (statusCode == HttpServletResponse.SC_OK) {

            JsonNode payload = JacksonFunctions.stringToNode(response.body().asString());
            JsonPipelineOutput model = new JsonPipelineOutputImpl(payload, ImmutableList.of(request));
            int maxAge = NumberUtils.toInt((String)response.getHeaderAsMap("Cache-Control").get("max-age"));
            if (maxAge > 0) {
              model = model.withMaxAge(maxAge);
            }
            subscriber.onNext(model);
          }
          else {

            String msg = "Request for " + request.getUrl() + " failed with HTTP status code: " + statusCode + " (" + response.reason() + ")";
            log.warn(msg);

            subscriber.onError(new JsonPipelineInputException(statusCode, msg));
          }
        }
        catch (IOException ex) {
          subscriber.onError(new JsonPipelineInputException(500, "Failed to read JSON response from " + request.getUrl(), ex));
        }
      }
    };
  }
}
