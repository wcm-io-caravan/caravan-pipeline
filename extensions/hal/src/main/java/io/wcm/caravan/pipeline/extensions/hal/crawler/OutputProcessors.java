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
package io.wcm.caravan.pipeline.extensions.hal.crawler;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.Iterables;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

/**
 * Default output processors
 */
@ProviderType
public final class OutputProcessors {

  private OutputProcessors() {
    // nothing to do
  }

  /**
   * Creates a flat Collection of HAL links for each processed request.
   * @return Output processor creating flat links collection
   */
  public static OutputProcessor report() {
    return new OutputProcessor() {

      @Override
      public JsonPipelineOutput process(JsonPipelineOutput previousStepOutput, List<JsonPipelineOutput> loadedLinkOutputs) {

        // create output HAL
        String url = getUrl(previousStepOutput);
        HalResource hal = new HalResource(url);

        // add URL with relation to links
        CaravanHttpRequest request = getRequest(previousStepOutput);
        if (request != null && request.getHeaders().containsKey(HalCrawler.HEADER_CRAWLER_RELATION)) {
          String relation = Iterables.getFirst(request.getHeaders().get(HalCrawler.HEADER_CRAWLER_RELATION), null);
          hal.addLinks(relation, new Link(url));
        }

        // add child links
        loadedLinkOutputs.stream()
            .map(loadedLinkOutput -> new HalResource(loadedLinkOutput.getPayload()))
            .flatMap(loadedLinkHal -> loadedLinkHal.getLinks().entries().stream())
            .filter(entry -> !"self".equals(entry.getKey()))
            .forEach(entry -> hal.addLinks(entry.getKey(), entry.getValue()));

        // output
        return previousStepOutput.withPayload(hal.getModel());
      }

      @Override
      public String getId() {
        return "REPORT";
      }

    };
  }

  private static String getUrl(JsonPipelineOutput previousStepOutput) {
    CaravanHttpRequest request = getRequest(previousStepOutput);
    return request == null ? null : request.getUrl();
  }

  private static CaravanHttpRequest getRequest(JsonPipelineOutput previousStepOutput) {

    List<CaravanHttpRequest> requests = previousStepOutput.getRequests();
    if (requests.isEmpty()) {
      return null;
    }
    return requests.get(requests.size() - 1);

  }

}
