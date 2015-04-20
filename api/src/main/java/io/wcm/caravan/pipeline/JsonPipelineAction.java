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

import org.osgi.annotation.versioning.ConsumerType;

import rx.Observable;
import rx.functions.Func1;

/**
 * A JSON pipeline action aids in specifying of custom operations, which could be applied on {@link JsonPipelineOutput}.
 * Each action must called asynchronously as an element of JSON pipeline chain and could be added to the chain via
 * {@link JsonPipeline#applyAction(JsonPipelineAction)}. Action will be executed only as soon as the previous step
 * result of the chain is available.
 * A JSON pipeline action could be implemented in multiple cases, when the necessary operation is not present directly
 * in the declared methods of {@link JsonPipeline}. The most usual cases of custom action implementation are
 * - a custom transformation of the pipeline output is required. See
 * {@link JsonPipelineActions#simpleTransformation(String, Func1)};
 * - modification of existing or creation of a new pipeline output uses data, which require an access to an
 * external data source. A call for such data could be implemented in the JSON pipeline action.
 * Each implementation of JSON pipeline action must guarantee an identifier, provided by {@link #getId()}. The
 * identifier must be constant (non modifiable) per each new class instance, even if the pipeline output is changed.
 * See {@link JsonPipelineActions} for existing implementations.
 */
@ConsumerType
public interface JsonPipelineAction {

  /**
   * Provides an unique identifier of this actions, which must be guaranteed by the implementation to be constant during
   * the whole lifetime of the action instance.
   * @return unique identifier of this action
   */
  String getId();

  /**
   * Provides a subscription to the JSON pipeline output with the the results of this action.
   * @param previousStepOutput a JSON pipeline output provided after pipeline has executed previous steps
   * @param pipelineContext the context of the {@link JsonPipeline} instance
   * @return a subscription the result JSON pipeline output
   */
  Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext);

}
