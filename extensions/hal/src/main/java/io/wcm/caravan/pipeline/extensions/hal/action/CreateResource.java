/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2016 wcm.io
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

import org.apache.commons.lang3.StringUtils;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;
import rx.Observable;

/**
 * an abstract base class for actions that create a new {@link HalResource} based on the Hal output of the previous
 * pipeline step
 */
public abstract class CreateResource implements JsonPipelineAction {

  private final String selfHref;

  /**
   * @param selfHref the path and query parameters to use in the output's self link (also used for the cache-key
   *          generation)
   */
  public CreateResource(String selfHref) {
    this.selfHref = selfHref;
  }

  @Override
  public String getId() {
    return "CREATE-RESOURCE(" + selfHref + ")";
  }

  @Override
  public final Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    if (!previousStepOutput.getPayload().isObject()) {
      throw new JsonPipelineOutputException(CreateResource.class.getName()
          + " expects the output of the previous step to be a JSON *object* output, but got "
          + previousStepOutput.getPayload().toString());
    }

    HalResource input = new HalResource(previousStepOutput.getPayload());

    HalResource output = createOutput(input);

    if (output.getLink() == null) {
      output.setLink(new Link(selfHref));
    }
    else if (StringUtils.isBlank(output.getLink().getHref())) {
      output.getLink().setHref(selfHref);
    }

    return Observable.just(previousStepOutput.withPayload(output.getModel()));
  }

  /**
   * @param input the original {@link HalResource} from the previous step's output
   * @return the output hal resource (a self link will be automatically set)
   */
  public abstract HalResource createOutput(HalResource input);

}
