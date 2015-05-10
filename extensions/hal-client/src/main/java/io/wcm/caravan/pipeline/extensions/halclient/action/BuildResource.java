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
package io.wcm.caravan.pipeline.extensions.halclient.action;

import io.wcm.caravan.commons.hal.HalBuilder;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A pipeline action you can use if you want to build a new {@link HalResource} based on another {@link HalResource}
 * from the previous step's output.
 */
public abstract class BuildResource implements JsonPipelineAction {

  private final String selfHref;

  /**
   * @param selfHref the path and query parameters to use in the output's self link
   */
  public BuildResource(String selfHref) {
    this.selfHref = selfHref;
  }

  @Override
  public String getId() {
    return "BUILD-RESOURCE(" + selfHref + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    if (!previousStepOutput.getPayload().isObject()) {
      throw new JsonPipelineOutputException(BuildResource.class.getName()
          + " expects the output of the previous step to be a JSON *object* output, but got "
          + previousStepOutput.getPayload().toString());
    }

    HalResource input = new HalResource(((ObjectNode)previousStepOutput.getPayload()));

    if (input.getLink() == null || StringUtils.isBlank(input.getLink().getHref())) {
      throw new JsonPipelineOutputException(BuildResource.class.getName() +
          " expects the output of the previous step to already be a HAL resource, but got "
          + previousStepOutput.getPayload().toString());
    }

    HalBuilder outputBuilder = new HalBuilder(selfHref);

    HalResource output = build(input, outputBuilder);

    return Observable.just(previousStepOutput.withPayload(output.getModel()));
  }

  /**
   * @param input the original {@link HalResource} from the previous step's output
   * @param outputBuilder a builder that already contains a self-link, but no state
   * @return the new HalResource that will be returned as the result of the pipeline action
   */
  public abstract HalResource build(HalResource input, HalBuilder outputBuilder);
}
