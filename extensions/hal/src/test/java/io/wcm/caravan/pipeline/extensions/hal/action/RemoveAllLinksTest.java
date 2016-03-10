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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

@RunWith(MockitoJUnitRunner.class)
public class RemoveAllLinksTest {

  @Mock
  private JsonPipelineContext context;

  private ObjectNode embedded = new HalResource("/embeddedResource1")
      .addLinks("links4", new Link("/resource5"), new Link("/resource6"))
      .addLinks("links5", new Link("/resource7"), new Link("/resource8"))
      .getModel();

  private ObjectNode payload = new HalResource("/resource")
      .addLinks("links1", new Link("/resource1"), new Link("/resource2"))
      .setLink("links2", new Link("/resource3"))
      .addLinks("links3", new Link("/resource4"))
      .addEmbedded("embedded1", new HalResource(embedded))
      .getModel();

  @Test
  public void getId_shouldAnswerWithoutRelations() {

    assertNotNull(new RemoveAllLinks().getId());

  }

  @Test
  public void getId_shouldBeUniqueForRelations() {

    String id = new RemoveAllLinks().except("links1").except("links2").getId();
    assertTrue(id.contains("links1"));
    assertTrue(id.contains("links2"));

  }

  @Test
  public void shouldRemoveLinksNotMatchingGivenRelations() {

    HalResource hal = getHalOutput("links3", "links4");
    assertFalse(hal.hasLink("links1"));
    assertFalse(hal.hasLink("links2"));
    assertFalse(hal.getEmbedded("embedded1").get(0).hasLink("links5"));

  }

  private HalResource getHalOutput(String... relations) {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    RemoveAllLinks action = new RemoveAllLinks().except(relations);
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    return new HalResource(output.getPayload());

  }

  @Test
  public void shouldRemainLinksMatchingGivenRelation() {

    HalResource hal = getHalOutput("links3", "links4");
    assertTrue(hal.hasLink("links3"));
    assertTrue(hal.getEmbedded("embedded1").get(0).hasLink("links4"));

  }

}
