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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JacksonFunctions;
import io.wcm.caravan.pipeline.impl.JsonPathSelector;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * An operator that evaluates a JSONPath expression on the pipeline's JSON output and collects *ALL* results.
 * Extraction results are always saved in Jackson {@link ArrayNode}, even if no results are found.
 */
public class CollectOperator implements Operator<JsonPipelineOutput, JsonPipelineOutput> {

  private final String jsonPath;
  private final String targetProperty;

  /**
   * @param jsonPath the JSONPath to evaluate
   * @param targetProperty the name of the property to store the first result of the JSONpath expression in
   */
  public CollectOperator(String jsonPath, String targetProperty) {
    this.jsonPath = jsonPath;
    this.targetProperty = targetProperty;
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
        subscriber.onError(e);
      }

      @Override
      public void onNext(JsonPipelineOutput output) {
        JsonNode extractedPayload = new JsonPathSelector(jsonPath).call(output.getPayload());

        if (isNotBlank(targetProperty)) {
          extractedPayload = JacksonFunctions.wrapInObject(targetProperty, extractedPayload);
        }

        JsonPipelineOutput extractedOutput = output.withPayload(extractedPayload);
        subscriber.onNext(extractedOutput);
      }
    };
  }
}
