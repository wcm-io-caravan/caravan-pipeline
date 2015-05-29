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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.extensions.hal.client.action.DeepEmbedLinks;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class DeepEmbedLinksTest extends AbstractActionContext {

  @Test
  public void shouldHaveAnUniqueId() {

    DeepEmbedLinks action = createAction("multiple");
    assertTrue(action.getId().contains("multiple"));

  }

  private DeepEmbedLinks createAction(String relation) {
    return new DeepEmbedLinks("testService", relation, Collections.emptyMap());
  }

  @Test
  public void shouldLoadAllLinksForRelationAndEmbedAsResource() {

    HalResource hal = createHalOutput("primary");

    List<HalResource> resources = hal.getEmbedded("primary");
    assertEquals(2, resources.size());
    assertEquals("/resource1", resources.get(0).getLink().getHref());
    assertEquals("/resource2", resources.get(1).getLink().getHref());

    resources = hal.getEmbedded("stillEmbedded").get(0).getEmbedded("primary");
    assertEquals(2, resources.size());
    assertEquals("/resource1", resources.get(0).getLink().getHref());
    assertEquals("/resource2", resources.get(1).getLink().getHref());

  }

  private HalResource createHalOutput(String relation) {

    DeepEmbedLinks action = createAction(relation);
    ObjectNode payload = getPayload();
    new HalResource(payload).addEmbedded("stillEmbedded", new HalResource(getPayload()));
    JsonPipelineOutput input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    HalResource hal = new HalResource((ObjectNode)output.getPayload());
    return hal;

  }

  @Test
  public void shouldDeleteAllLinksForRelation() {

    HalResource hal = createHalOutput("primary");
    assertFalse(hal.hasLink("primary"));
    assertTrue(hal.hasLink("secondary"));

    hal = hal.getEmbedded("stillEmbedded").get(0);
    assertFalse(hal.hasLink("primary"));
    assertTrue(hal.hasLink("secondary"));

  }

  @Test
  public void shouldDoNothingForMissingRelations() {

    HalResource hal = createHalOutput("unknown");
    assertTrue(hal.hasLink("secondary"));
    assertTrue(hal.hasLink("primary"));
    assertFalse(hal.hasEmbedded("primary"));

    hal = hal.getEmbedded("stillEmbedded").get(0);
    assertTrue(hal.hasLink("secondary"));
    assertTrue(hal.hasLink("primary"));
    assertFalse(hal.hasEmbedded("primary"));

  }

}
