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

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineContextImpl;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;
import io.wcm.caravan.testing.pipeline.JsonPipelineContext;

@RunWith(MockitoJUnitRunner.class)
public class InlineEmbeddedTest {

  public OsgiContext osgiCtx = new OsgiContext();
  public JsonPipelineContext pipelineCtx = new JsonPipelineContext(osgiCtx);

  @Rule
  public RuleChain chain = RuleChain
  .outerRule(osgiCtx)
  .around(pipelineCtx);

  private ObjectNode resource1 = new HalResource("/resource1").getModel().put("key", "val1");
  private ObjectNode resource2 = new HalResource("/resource2").getModel().put("key", "val2");
  private ObjectNode resource3 = new HalResource("/resource3").getModel().put("key", "val3");
  private ObjectNode payload = new HalResource("/resource")
      .setEmbedded("singular", new HalResource(resource1))
      .addEmbedded("multiple", new HalResource(resource2), new HalResource(resource3))
      .getModel();

  @Mock
  protected JsonPipelineContextImpl context;

  @Test
  public void shouldHaveUniqueId() {

    String id = new InlineEmbedded("singular", "multiple").getId();
    assertTrue(id.contains("singular"));
    assertTrue(id.contains("multiple"));

  }

  private HalResource createHalOutput(String... relations) {
    InlineEmbedded action = new InlineEmbedded(relations);
    JsonPipelineOutputImpl input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    return new HalResource(output.getPayload());
  }

  @Test
  public void shouldStoreAsObjectForSingularResources() {

    HalResource hal = createHalOutput("singular");

    JsonNode container = hal.getModel().get("singular");
    assertTrue(container.isObject());
    assertEquals("val1", container.get("key").asText(null));

  }

  @Test
  public void shouldStoreInArrayForMultipleResources() {

    HalResource hal = createHalOutput("multiple");

    JsonNode container = hal.getModel().get("multiple");
    assertTrue(container.isArray());
    assertEquals(2, container.size());
    assertEquals("val2", container.get(0).get("key").asText(null));

  }

  @Test
  public void shouldRemoveSelfLink() {

    HalResource hal = createHalOutput("singular", "multiple");
    assertFalse(hal.hasLink("self"));

  }

  @Test
  public void shouldRemoveOldReferenceAsEmbeddedResource() {

    HalResource hal = createHalOutput("singular");
    assertFalse(hal.hasEmbedded("singular"));
    assertTrue(hal.hasEmbedded("multiple"));

  }

}
