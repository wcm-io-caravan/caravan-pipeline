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
package io.wcm.caravan.pipeline.extensions.hal.filter;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

@RunWith(MockitoJUnitRunner.class)
public class FilterEmbeddedHalResourceTest {

  @Mock
  private HalResourcePredicate matcher;
  @Mock
  private HalResourcePredicate predicate;
  @Mock
  private JsonPipelineContext context;

  private FilterEmbeddedHalResource action;

  private ObjectNode payload = HalResourceFactory.createResource("/resource")
      .addEmbedded("layer1-1",
          HalResourceFactory.createResource("/resource1-1-1")
          .addEmbedded("layer2-1",
              HalResourceFactory.createResource("/resource2-1-1"),
              HalResourceFactory.createResource("/resource2-1-2"),
              HalResourceFactory.createResource("/resource2-1-3")))
      .getModel();

  @Before
  public void setUp() {

    action = new FilterEmbeddedHalResource(matcher, predicate);

    Mockito.when(matcher.apply(Matchers.any(), Matchers.any())).thenReturn(true);
    Mockito.when(predicate.apply(Matchers.any(), Matchers.any())).thenReturn(true);

  }

  @Test
  public void shouldBuildValidHalPath() {

    ArgumentCaptor<HalPath> captor = ArgumentCaptor.forClass(HalPath.class);
    Mockito.when(matcher.apply(captor.capture(), Matchers.any())).thenReturn(true);

    getHalOutput();

    List<HalPath> arguments = captor.getAllValues();
    assertEquals("/layer1-1", arguments.get(0).toString());
    assertEquals("/layer1-1/layer2-1", arguments.get(1).toString());

  }

  @Test
  public void shouldRemoveFilteredResource() {

    Mockito.when(predicate.apply(Matchers.any(), Matchers.argThat(new ArgumentMatcher<HalResource>() {

      @Override
      public boolean matches(Object argument) {
        return "/resource2-1-2".equals(((HalResource)argument).getLink().getHref());
      }
    }))).thenReturn(false);

    HalResource hal = getHalOutput();

    List<HalResource> resources = hal.getEmbedded("layer1-1").get(0).getEmbedded("layer2-1");
    assertEquals(2, resources.size());
    assertEquals("/resource2-1-1", resources.get(0).getLink().getHref());
    assertEquals("/resource2-1-3", resources.get(1).getLink().getHref());

  }

  private HalResource getHalOutput() {

    JsonPipelineOutputImpl input = new JsonPipelineOutputImpl(payload, Collections.emptyList());
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    return new HalResource(output.getPayload());

  }

}
