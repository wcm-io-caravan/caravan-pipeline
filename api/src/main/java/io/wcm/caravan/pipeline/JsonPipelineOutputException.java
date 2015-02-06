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

/**
 * Used to indicate a problem while creating the output of a {@link JsonPipeline} (most likely due to an impossible
 * type-mapping).
 */
public final class JsonPipelineOutputException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * @param msg Message
   */
  public JsonPipelineOutputException(String msg) {
    super(msg);
  }

  /**
   * @param msg Message
   * @param cause Cause
   */
  public JsonPipelineOutputException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
