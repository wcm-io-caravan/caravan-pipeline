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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;


public class OutputProcessorsTest {

  @Test
  public void report_shouldListAllCrawledUrlsInFlatLinkList() {

    OutputProcessor processor = OutputProcessors.report();

    HalResource resource = new HalResource("/resource")
        .addLinks("section", new Link("/resource-1"), new Link("/resource-2"));
    HalResource resource1 = new HalResource("/resource-1")
        .addLinks("item", new Link("/resource-1-1"));
    HalResource resource1_1 = new HalResource("/resource-1-1");
    HalResource resource2 = new HalResource("/resource-2");

    JsonPipelineOutput result2 = processor.process(createJsonPipelineOutput("section", resource2), Collections.emptyList());
    JsonPipelineOutput result1_1 = processor.process(createJsonPipelineOutput("item", resource1_1), Collections.emptyList());
    JsonPipelineOutput result1 = processor.process(createJsonPipelineOutput("section", resource1), ImmutableList.of(result1_1));
    JsonPipelineOutput output = processor.process(createJsonPipelineOutput(null, resource), ImmutableList.of(result1, result2));
    HalResource hal = new HalResource(output.getPayload());

    assertEquals("/resource", hal.getLink().getHref());
    assertEquals(2, hal.getLinks("section").size());
    assertEquals("/resource-1", hal.getLinks("section").get(0).getHref());
    assertEquals(1, hal.getLinks("item").size());
    assertEquals("/resource-1-1", hal.getLinks("item").get(0).getHref());

  }

  private JsonPipelineOutput createJsonPipelineOutput(String relation, HalResource hal) {
    return createJsonPipelineOutput(relation, hal, 200);
  }

  private JsonPipelineOutput createJsonPipelineOutput(String relation, HalResource hal, int status) {
    String url = hal.getLink().getHref();
    return createJsonPipelineOutput(relation, url, hal.getModel(), status);
  }

  private JsonPipelineOutput createJsonPipelineOutput(String relation, String url, JsonNode json, int status) {
    CaravanHttpRequestBuilder builder = new CaravanHttpRequestBuilder().append(url);
    if (relation != null) {
      builder.header(HalCrawler.HEADER_CRAWLER_RELATION, relation);
    }
    CaravanHttpRequest request = builder.build();
    List<CaravanHttpRequest> requests = Lists.newArrayList(request);
    return new JsonPipelineOutputImpl(json, requests).withStatusCode(status);
  }

}
