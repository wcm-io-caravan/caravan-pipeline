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
package io.wcm.caravan.pipeline.extensions.hal.client.action;

import java.util.Collections;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.impl.JsonPipelineContextImpl;
import io.wcm.caravan.pipeline.impl.JsonPipelineOutputImpl;
import io.wcm.caravan.testing.pipeline.JsonPipelineContext;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractActionContext {

  public OsgiContext osgiCtx = new OsgiContext();
  public JsonPipelineContext pipelineCtx = new JsonPipelineContext(osgiCtx);

  @Rule
  public RuleChain chain = RuleChain
  .outerRule(osgiCtx)
  .around(pipelineCtx);

  @Mock
  protected CacheStrategy cacheStrategy;

  protected JsonPipelineContextImpl context;

  @Before
  public void setUp() {

    context = new JsonPipelineContextImpl(pipelineCtx.getJsonPipelineFactory(), pipelineCtx.getCacheAdapter(), pipelineCtx.getMetricRegistry(),
        Collections.emptyMap());
    pipelineCtx.getCaravanHttpClient().mockRequest().urlStartsWith("/resource1")
    .response(new HalResource("/resource1").getModel().toString());
    pipelineCtx.getCaravanHttpClient().mockRequest().urlStartsWith("/resource2")
    .response(new HalResource("/resource2").getModel().toString());

    Mockito.when(cacheStrategy.getCachePersistencyOptions(Matchers.any())).thenReturn(CachePersistencyOptions.createTransient(0));

  }

  protected ObjectNode getPayload() {
    return new HalResource("/resource")
        .addLinks("primary", new Link("/resource1"), new Link("/resource2"))
        .addLinks("secondary", new Link("/resource3"))
        .getModel();
  }

  protected JsonPipelineOutput getInput() {
    return new JsonPipelineOutputImpl(getPayload(), Collections.emptyList());
  }

}
