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
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.extensions.hal.action.StripHal;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class StripHalTest {

  @Mock
  private JsonPipelineContext context;

  private final ObjectNode payload = HalResourceFactory.createResource("/resource")
      .addEmbedded("item", HalResourceFactory.createResource("/embedded1"))
      .getModel().put("attribute", "value");

  private final StripHal action = new StripHal();

  private JsonNode json;

  @Before
  public void setup() {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    json = output.getPayload();

  }

  @Test
  public void shouldHaveAnId() {

    assertEquals("STRIP-HAL", action.getId());

  }

  @Test
  public void shouldRemoveAllHalData() {

    assertFalse(json.has("_links"));
    assertFalse(json.has("_embedded"));

  }

  @Test
  public void shouldRemainState() {

    assertTrue(json.has("attribute"));

  }

}
