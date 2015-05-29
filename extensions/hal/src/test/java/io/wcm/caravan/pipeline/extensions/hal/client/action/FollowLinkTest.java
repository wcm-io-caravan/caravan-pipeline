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
package io.wcm.caravan.pipeline.extensions.hal.client.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.extensions.hal.client.action.FollowLink;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

public class FollowLinkTest extends AbstractActionContext {

  @Test
  public void shouldHaveUniqueId() {

    FollowLink action = createAction("primary", 99);
    assertTrue(action.getId().contains("primary"));
    assertTrue(action.getId().contains("99"));

  }

  private FollowLink createAction(String relation, int index) {
    return new FollowLink("testService", relation, index, Collections.emptyMap());
  }

  @Test
  public void shouldLoadLinkAndReturnContent() {

    HalResource hal = createHalOutput("primary", 1);
    assertEquals("/resource2", hal.getLink().getHref());

  }

  private HalResource createHalOutput(String relation, int index) {

    FollowLink action = createAction(relation, index);
    JsonPipelineOutput output = action.execute(getInput(), context).toBlocking().single();
    HalResource hal = new HalResource((ObjectNode)output.getPayload());
    return hal;

  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionForunknownRelation() {

    HalResource hal = createHalOutput("unknown", 1);

  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionForInknownIndex() {

    HalResource hal = createHalOutput("primary", 99);

  }

  @Test
  public void shouldUseCacheStrategy() {

    FollowLink action = createAction("primary", 1).setCacheStrategy(cacheStrategy);
    action.execute(getInput(), context).toBlocking().single();
    Mockito.verify(cacheStrategy).getCachePersistencyOptions(Matchers.any());

  }

  @Test
  public void shouldUseCacheControlHeader() {

    FollowLink action = createAction("primary", 1);
    List<CaravanHttpRequest> requests = ImmutableList.of(new CaravanHttpRequestBuilder().header("Cache-Control", "max-age=10").build());
    JsonPipelineOutput input = new JsonPipelineOutputImpl(getPayload(), requests);
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    assertEquals("10", output.getRequests().get(0).getCacheControl().get("max-age"));

  }

  @Test
  public void shouldUseCorrelatonId() {

    FollowLink action = createAction("primary", 1);
    List<CaravanHttpRequest> requests = ImmutableList.of(new CaravanHttpRequestBuilder("test-service", "test-correlation-id").build());
    JsonPipelineOutput input = new JsonPipelineOutputImpl(getPayload(), requests);
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    assertEquals("test-correlation-id", output.getCorrelationId());

  }

}
