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
package io.wcm.caravan.pipeline.extensions.halclient.action;

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
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
public class ModifyResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock
  private JsonPipelineContext context;

  private final ObjectNode payload = HalResourceFactory.createResource("/old").getModel().put("oldAttribute", "value");

  private final ModifyResource action = new ModifyResource("/new") {

    @Override
    public void modify(HalResource output) {
      output.getModel().put("newAttribute", "value");
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

    assertEquals("MODIFY-RESOURCE(/new)", action.getId());

  }

  @Test(expected = JsonPipelineOutputException.class)
  public void shouldThrowExceptionIfInputIsNoJsonObject() {

    JsonPipelineOutput input = new JsonPipelineOutputImpl(OBJECT_MAPPER.createArrayNode(), Collections.emptyList());
    action.execute(input, context).toBlocking().single();

  }

  @Test
  public void shouldHavenewSelfHref() {

    assertEquals("/new", hal.getLink().getHref());

  }

  @Test
  public void shouldHaveCombinedState() {

    assertEquals("value", hal.getModel().get("oldAttribute").asText());
    assertEquals("value", hal.getModel().get("newAttribute").asText());

  }

}
