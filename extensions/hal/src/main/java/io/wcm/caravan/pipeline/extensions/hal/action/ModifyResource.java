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
package io.wcm.caravan.pipeline.extensions.hal.action;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.util.HalBuilder;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import org.osgi.annotation.versioning.ConsumerType;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An action that can take any JSON object as input (already HAL or not), sets the specified self-link and allows
 * arbitrary manipulation of the output HalResource by implementing the build function.
 */
//CHECKSTYLE:OFF
@ConsumerType
public abstract class ModifyResource implements JsonPipelineAction {
  //CHECKSTYLE:ON

  private final String selfHref;

  /**
   * @param selfHref New self HREF
   */
  public ModifyResource(String selfHref) {
    this.selfHref = selfHref;
  }

  @Override
  public String getId() {
    return "MODIFY-RESOURCE(" + selfHref + ")";
  }

  @Override
  public final Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    if (!previousStepOutput.getPayload().isObject()) {
      throw new JsonPipelineOutputException(ModifyResource.class.getName()
          + " expects the output of the previous step to be a JSON *object* output, but got " + previousStepOutput.getPayload().toString());
    }

    ObjectNode state = previousStepOutput.getPayload().deepCopy();

    HalResource resource = new HalBuilder(state, selfHref).build();

    modify(resource);

    return Observable.just(previousStepOutput.withPayload(resource.getModel()));
  }

  /**
   * @param output a HalResource with the state of the previous pipeline output and a
   *          self link, that you can modify to fine-tune your output
   */
  public abstract void modify(HalResource output);

}
