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

import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPathSelector;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * An operator that will check if the pipeline's output contains data at a given JSONPath, and fail with a
 * {@link JsonPipelineInputException} if this assumptions is not fulfilled.
 */
public class AssertExistsOperator implements Operator<JsonPipelineOutput, JsonPipelineOutput> {

  private final String jsonPath;
  private final int statusCode;
  private final String msg;

  /**
   * @param jsonPath the JSONPath to evaluate on the pipeline's output
   * @param statusCode the appropriate HTTP status code in case data is missing
   * @param msg an informative error message
   */
  public AssertExistsOperator(String jsonPath, int statusCode, String msg) {

    // fail fast if the given json path is invalid
    JsonPath.compile(jsonPath);

    this.jsonPath = jsonPath;
    this.statusCode = statusCode;
    this.msg = msg;
  }

  @Override
  public Subscriber<? super JsonPipelineOutput> call(Subscriber<? super JsonPipelineOutput> subscriber) {
    return new Subscriber<JsonPipelineOutput>() {

      private boolean assertionFailed;

      @Override
      public void onCompleted() {
        // take care not to call #onCompleted if #onError has been called because the assertion failed
        if (!assertionFailed) {
          subscriber.onCompleted();
        }
      }

      @Override
      public void onError(Throwable e) {
        Exceptions.throwIfFatal(e);
        subscriber.onError(e);
      }

      @Override
      public void onNext(JsonPipelineOutput output) {

        JsonNode jsonResponse = output.getPayload();

        // if this #assertExist is chained after an #extract call, the responseNode or its child node can be a MissingNode and we should bail out early

        if (jsonResponse == null || jsonResponse.isMissingNode() || (jsonResponse.isObject() && jsonResponse.iterator().next().isMissingNode())) {
          onAssertionFailed();
          return;
        }

        try {
          // evalute the JSONPath on the pipeline's response
          ArrayNode jsonPathResult = new JsonPathSelector(jsonPath).call(jsonResponse);

          if (jsonPathResult.size() == 0) {
            onAssertionFailed();
          }
          else {
            // the responseNode has content at the given JsonPath, so we can continue processing the response
            subscriber.onNext(output);
          }
        }
        catch (PathNotFoundException p) {
          onAssertionFailed();
        }
      }

      public void onAssertionFailed() {
        assertionFailed = true;
        this.onError(new JsonPipelineInputException(statusCode, msg));
      }
    };
  }
}
