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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * work in progress! functional interface to allow users of the JsonPipeline to specify custom exception handling with
 * {@link JsonPipeline#handleException(JsonPipelineExceptionHandler)}
 */
public interface JsonPipelineExceptionHandler {

  /**
   * in this method, you can decide depending on the response code and exception type if you want to provide fallback
   * content, wrap the exception in another class or rethrow it as it is
   * @param responseCode if the exception originated from a failed HTTP request, this will contain the status code
   * @param ex the exception thrown
   * @return an observable that emits a single JsonNode with the fallback content to use instead
   */
  Observable<JsonNode> rethrowOrReturnFallback(int responseCode, RuntimeException ex);
}
