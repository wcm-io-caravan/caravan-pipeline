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
package io.wcm.caravan.pipeline;

import rx.Observable;
import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Default implementations of {@link JsonPipelineAction}
 */
public final class JsonPipelineActions {

  private JsonPipelineActions() {
    // static methods only
  }

  /**
   * Applies a custom transformation on the JSON node (e.g. a HAL representation) of this pipeline, specifying a
   * function as transformation mechanism. Function receives JSON node as parameter, which should be
   * applied on the actual JSON node, and returns a new JSON node with the transformation result.
   * @param transformationId an unique id of the actual transformation
   * @param transformation a function that provides transformation algorithm
   * @return a new pipeline that will emit the result of the transformation
   */
  public static JsonPipelineAction simpleTransformation(String transformationId, Func1<JsonNode, JsonNode> transformation) {
    return new JsonPipelineAction() {

      @Override
      public String getId() {
        return transformationId;
      }

      @Override
      public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput) {
        JsonNode transformedPayload = transformation.call(previousStepOutput.getPayload());
        JsonPipelineOutput transformedOutput = previousStepOutput.withPayload(transformedPayload);
        return Observable.just(transformedOutput);
      }

    };
  }
}
