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
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.hal.resource.util.HalBuilder;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class BuildResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private JsonPipelineContext context;

  private final ObjectNode payload = HalResourceFactory.createResource("/old").getModel().put("oldAttribute", "value");

  private final BuildResource action = new BuildResource("/new") {

    @Override
    public HalResource build(HalResource input, HalBuilder outputBuilder) {
      HalResource outputHal = outputBuilder.build();
      outputHal.getModel().put("newAttribute", "value");
      return outputHal;
    }

  };

  private HalResource hal;

  @Before
  public void setup() {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    hal = new HalResource((ObjectNode)output.getPayload());

  }

  @Test
  public void shouldHaveUniqueId() {

    assertEquals("BUILD-RESOURCE(/new)", action.getId());

  }

  @Test(expected = JsonPipelineOutputException.class)
  public void shouldThrowExceptionIfInputIsNoJsonObject() {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(OBJECT_MAPPER.createArrayNode(), Collections.emptyList());
    action.execute(input, context).toBlocking().single();

  }

  @Test
  public void shouldAddSelfLinkIfInputIsNoHalResource() {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(OBJECT_MAPPER.createObjectNode(), Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();

    assertEquals("/new", new HalResource((ObjectNode)output.getPayload()).getLink().getHref());

  }

  @Test
  public void shouldAddSelfLinkIfInputHalResourceHasNoLink() {

    ObjectNode errorInput = OBJECT_MAPPER.createObjectNode();
    errorInput.putObject("_links").putObject("self");
    JsonPipelineOutput input = new JsonPipelineOutputImpl(errorInput, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();

    assertEquals("/new", new HalResource((ObjectNode)output.getPayload()).getLink().getHref());
  }

  @Test
  public void shouldHaveSelfHref() {

    assertEquals("/new", hal.getLink().getHref());

  }

  @Test
  public void shouldHaveNewState() {

    assertEquals("value", hal.getModel().get("newAttribute").asText());
    assertFalse(hal.getModel().has("oldAttribute"));

  }

}
