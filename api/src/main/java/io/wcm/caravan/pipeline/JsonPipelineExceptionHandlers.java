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

import static rx.Observable.just;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contains some common exception handling strategies to be used with
 * {@link JsonPipeline#handleException(JsonPipelineExceptionHandler)}
 */
@ProviderType
public final class JsonPipelineExceptionHandlers {

  private JsonPipelineExceptionHandlers() {
    // no reason to instantiate
  }

  /**
   * Creates an exception handling function that will throw a new {@link JsonPipelineInputException} with the given
   * message if the exception being caught has a status code of 404
   * @param msg the error message
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is 404
   */
  public static JsonPipelineExceptionHandler rethrow404(String msg) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() == 404) {
        throw new JsonPipelineInputException(404, msg, e);
      }
      throw e;
    };
  }

  /**
   * Creates an exception handling function that will throw the given exception if the exception being caught has a
   * status code of 404 (and otherwise rethrow the original exception )
   * @param exceptionToThrow the error message
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is 404
   */
  public static JsonPipelineExceptionHandler rethrow404(RuntimeException exceptionToThrow) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() == 404) {
        throw exceptionToThrow;
      }
      throw e;
    };
  }

  /**
   * Creates an exception handling function that will throw throw a new {@link JsonPipelineInputException} with the
   * given message if the exception being caught has a status code >= 500
   * @param msg the new error message
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is in the 50x range
   */
  public static JsonPipelineExceptionHandler rethrow50x(String msg) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() >= 500) {
        throw new JsonPipelineInputException(defaultFallbackOutput.getStatusCode(), msg, e);
      }
      throw e;
    };
  }

  /**
   * Creates an exception handling function that will throw the given exception if the exception being caught has a
   * status code &gt;= 500 (and otherwise rethrow the original exception )
   * @param exceptionToThrow the error message
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is 404
   */
  public static JsonPipelineExceptionHandler rethrow50x(RuntimeException exceptionToThrow) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() >= 500) {
        throw exceptionToThrow;
      }
      throw e;
    };
  }


  /**
   * Creates an exception handling function that will provide static fallback content if the exception being caught has
   * a status code of 404 (and otherwise rethrow the original exception )
   * @param fallbackContent the fallback content to return
   * @param timeToLive the duration in seconds for which the fallback response is allowed to be cached
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is 404
   */
  public static JsonPipelineExceptionHandler fallbackFor404(JsonNode fallbackContent, int timeToLive) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() != 404) {
        throw e;
      }
      return just(defaultFallbackOutput
          .withStatusCode(200)
          .withPayload(fallbackContent)
          .withMaxAge(timeToLive));
    };
  }

  /**
   * Creates an exception handling function that will provide static fallback content if the exception being caught has
   * a status code &gt; 500 (and otherwise rethrow the original exception )
   * @param fallbackContent the fallback content to return
   * @param timeToLive the duration in seconds for which the fallback response is allowed to be cached
   * @return a function that will throw a new {@link JsonPipelineInputException} if status code is 404
   */
  public static JsonPipelineExceptionHandler fallbackFor50x(JsonNode fallbackContent, int timeToLive) {
    return (defaultFallbackOutput, e) -> {
      if (defaultFallbackOutput.getStatusCode() < 500) {
        throw e;
      }
      return just(defaultFallbackOutput
          .withStatusCode(200)
          .withPayload(fallbackContent)
          .withMaxAge(timeToLive));
    };
  }
}
