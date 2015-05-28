package io.wcm.caravan.pipeline.extensions.halclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.extensions.halclient.action.DeepEmbedLinks;
import io.wcm.caravan.pipeline.extensions.halclient.action.EmbedLink;
import io.wcm.caravan.pipeline.extensions.halclient.action.EmbedLinks;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;


public class HalClientTest {

  private HalClient client;

  private JsonPipelineExceptionHandler handler = JsonPipelineExceptionHandlers.rethrow50x("BAD");

  @Before
  public void setUp() {
    client = new HalClient("test-service", CacheStrategies.noCache(), Collections.emptyMap())
        .setExceptionHandler(handler);
  }

  @Test
  public void shouldSetExceptionHandlerForEmbedLinks() {

    EmbedLinks action = client.embed("item");
    assertEquals(handler, action.getExceptionHandler());

  }

  @Test
  public void shouldSetExceptionHandlerForDeepEmbedLinks() {

    DeepEmbedLinks action = client.deepEmbed("item");
    assertEquals(handler, action.getExceptionHandler());

  }

  @Test
  public void shouldNotSetExceptionHandlerForEmbedLink() {

    EmbedLink action = client.embed("item", 1);
    assertNull(action.getExceptionHandler());

  }

}
