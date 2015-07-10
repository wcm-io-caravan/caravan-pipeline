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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.hal.commons.resource.HalResource;
import io.wcm.caravan.hal.commons.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineContextImpl;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;
import io.wcm.caravan.testing.pipeline.JsonPipelineContext;

import java.util.Collections;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class InlineEmbeddedCollectionTest {

  public OsgiContext osgiCtx = new OsgiContext();
  public JsonPipelineContext pipelineCtx = new JsonPipelineContext(osgiCtx);

  @Rule
  public RuleChain chain = RuleChain
      .outerRule(osgiCtx)
      .around(pipelineCtx);

  private ObjectNode resource1 = HalResourceFactory.createResource("/resource1").getModel().put("key", "val1");
  private ObjectNode resource2 = HalResourceFactory.createResource("/resource2").getModel().put("key", "val2");
  private HalResource embedded = new HalResource(resource1).addEmbedded("item", new HalResource(resource1), new HalResource(resource2));
  private ObjectNode payload = HalResourceFactory.createResource("/resource")
      .setEmbedded("embedded", embedded)
      .getModel();

  @Mock
  protected JsonPipelineContextImpl context;

  @Test
  public void shouldHaveUniqueId() {

    String id = new InlineEmbedded("embedded").getId();
    assertTrue(id.contains("embedded"));

  }

  private HalResource createHalOutput(String... relations) {
    InlineEmbeddedCollection action = new InlineEmbeddedCollection(relations);
    JsonPipelineOutputImpl input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    return new HalResource((ObjectNode)output.getPayload());
  }

  @Test
  public void shouldStoreEmbeddedCollectionInArray() {

    HalResource hal = createHalOutput("embedded");

    JsonNode container = hal.getModel().get("embedded");
    assertTrue(container.isArray());
    assertEquals(2, container.size());
    assertEquals("val1", container.get(0).get("key").asText());

  }

  @Test
  public void shouldRemoveSelfLink() {

    HalResource hal = createHalOutput("embedded");
    assertFalse(hal.hasLink("self"));

  }

  @Test
  public void shouldRemoveEmbeddedResource() {

    HalResource hal = createHalOutput("embedded");
    assertFalse(hal.hasEmbedded("embedded"));

  }

}
