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

import org.osgi.annotation.versioning.ProviderType;

/**
 * Used to indicate that the JSON input data of a {@link JsonPipeline} could not be retrieved, was invalid JSON, or
 * didn't match the expected data structure.
 */
@ProviderType
public final class JsonPipelineInputException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;

  private String reason;

  /**
   * @param statusCode the status code to use in the HTTP response
   * @param msg the error message (that will also be used as reason string in the HTTP response)
   */
  public JsonPipelineInputException(int statusCode, String msg) {
    super(msg);
    this.statusCode = statusCode;
  }

  /**
   * @param statusCode the status code to use in the HTTP response
   * @param msg the error message (that will also be used as reason string in the HTTP response)
   * @param cause Cause
   */
  public JsonPipelineInputException(int statusCode, String msg, Throwable cause) {
    super(msg, cause);
    this.statusCode = statusCode;
  }

  /**
   * Provides the appropriate status code for this exception. If the underlying HTTP request fails, the status code will
   * be taken from the HTTP response. If JSON parsing or object mapping failed, the status code will be 500.
   * @return the HTTP status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * @return exception reason.
   */
  public String getReason() {
    return reason;
  }

  /**
   * @param reasonPhrase the reason line from the HTTP response headers
   * @return this
   */
  public JsonPipelineInputException setReason(String reasonPhrase) {
    this.reason = reasonPhrase;
    return this;
  }
}
