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
package io.wcm.caravan.pipeline.extensions.hal.client;

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.extensions.hal.client.action.DeepEmbedLinks;
import io.wcm.caravan.pipeline.extensions.hal.client.action.EmbedLink;
import io.wcm.caravan.pipeline.extensions.hal.client.action.EmbedLinks;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;


public class HalClientTest {

  private HalClient client;

  private JsonPipelineExceptionHandler handler = JsonPipelineExceptionHandlers.rethrow50x("BAD");

  @Before
  public void setUp() {
    client = new HalClient("test-service", CacheStrategies.noCache(), Collections.emptyMap())
        .addExceptionHandler(handler);
  }

  @Test
  public void shouldSetExceptionHandlerForEmbedLinks() {

    EmbedLinks action = client.embed("item");
    assertEquals(handler, action.getExceptionHandlers().get(0));

  }

  @Test
  public void shouldSetExceptionHandlerForDeepEmbedLinks() {

    DeepEmbedLinks action = client.deepEmbed("item");
    assertEquals(handler, action.getExceptionHandlers().get(0));

  }

  @Test
  public void shouldSetExceptionHandlerForEmbedLink() {

    EmbedLink action = client.embed("item", 1);
    assertEquals(handler, action.getExceptionHandlers().get(0));

  }

}
