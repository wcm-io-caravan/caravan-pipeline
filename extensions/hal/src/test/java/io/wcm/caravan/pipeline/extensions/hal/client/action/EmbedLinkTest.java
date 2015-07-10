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
import io.wcm.caravan.hal.commons.resource.HalResource;
import io.wcm.caravan.hal.commons.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;


public class EmbedLinkTest extends AbstractActionContext {

  @Test
  public void shouldHaveAnUniqueId() {

    EmbedLink action = createAction("primary", 99);
    assertTrue(action.getId().contains("primary"));
    assertTrue(action.getId().contains("99"));

  }

  private EmbedLink createAction(String relation, int index) {
    return new EmbedLink("testService", relation, index, Collections.emptyMap());
  }

  @Test
  public void shouldLoadOneLinkForRelationAndIndexAndEmbedAsResource() {

    HalResource hal = createHalOutput("primary", 1);

    List<HalResource> resources = hal.getEmbedded("primary");
    assertEquals(1, resources.size());
    assertEquals("/resource2", resources.get(0).getLink().getHref());

  }

  private HalResource createHalOutput(String relation, int index) {

    EmbedLink action = createAction(relation, index);
    JsonPipelineOutput output = action.execute(getInput(), context).toBlocking().single();
    HalResource hal = new HalResource((ObjectNode)output.getPayload());
    return hal;

  }

  @Test
  public void shouldDeleteOneLinkForRelationAndIndex() {

    HalResource hal = createHalOutput("primary", 1);

    List<Link> links = hal.getLinks("primary");
    assertEquals(1, links.size());
    assertEquals("/resource1", links.get(0).getHref());

  }

  @Test
  public void shouldDoNothingForMissingRelations() {

    HalResource hal = createHalOutput("unknown", 0);

    assertTrue(hal.hasLink("secondary"));
    assertTrue(hal.hasLink("primary"));
    assertFalse(hal.hasEmbedded("primary"));

  }

  @Test
  public void shouldDoNothingForWrongIndex() {

    HalResource hal = createHalOutput("primary", 55);

    assertTrue(hal.hasLink("secondary"));
    assertTrue(hal.hasLink("primary"));
    assertFalse(hal.hasEmbedded("primary"));

  }


}
