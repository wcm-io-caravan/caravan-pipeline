package io.wcm.caravan.pipeline.extensions.halclient.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RunWith(MockitoJUnitRunner.class)
public class RemoveAllLinksTest {

  @Mock
  private JsonPipelineContext context;

  private ObjectNode embedded = HalResourceFactory.createResource("/embeddedResource1")
      .addLinks("links4", HalResourceFactory.createLink("/resource5"), HalResourceFactory.createLink("/resource6"))
      .addLinks("links5", HalResourceFactory.createLink("/resource7"), HalResourceFactory.createLink("/resource8"))
      .getModel();

  private ObjectNode payload = HalResourceFactory.createResource("/resource")
      .addLinks("links1", HalResourceFactory.createLink("/resource1"), HalResourceFactory.createLink("/resource2"))
      .setLink("links2", HalResourceFactory.createLink("/resource3"))
      .addLinks("links3", HalResourceFactory.createLink("/resource4"))
      .addEmbedded("embedded1", new HalResource(embedded))
      .getModel();

  @Test
  public void shouldHaveUniqueId() {

    String id = new RemoveAllLinks("links1", "link2").getId();
    assertTrue(id.contains("links1"));
    assertTrue(id.contains("link2"));

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
    RemoveAllLinks action = new RemoveAllLinks(relations);
    JsonPipelineOutput output = action.execute(input, context).toBlocking().single();
    return new HalResource((ObjectNode)output.getPayload());

  }

  @Test
  public void shouldRemainLinksMatchingGivenRelation() {

    HalResource hal = getHalOutput("links3", "links4");
    assertTrue(hal.hasLink("links3"));
    assertTrue(hal.getEmbedded("embedded1").get(0).hasLink("links4"));

  }

}
