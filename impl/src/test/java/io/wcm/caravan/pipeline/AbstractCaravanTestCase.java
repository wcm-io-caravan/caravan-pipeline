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
package io.wcm.caravan.pipeline;

import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.JsonPipelineContext;

import java.util.HashMap;
import java.util.Map;

import org.mockito.Answers;
import org.mockito.Mock;

import com.codahale.metrics.MetricRegistry;



public abstract class AbstractCaravanTestCase {

  @Mock
  protected CacheAdapter cacheAdapter;

  @Mock(answer = Answers.RETURNS_MOCKS)
  protected MetricRegistry metricRegistry;

  public AbstractCaravanTestCase() {
    super();
  }

  protected static Map<String, String> getContextProperties() {
    Map<String, String> cacheMetadataProperties = new HashMap<String, String>();
    cacheMetadataProperties.put("id", "123-id");
    return cacheMetadataProperties;
  }

  protected JsonPipelineContext getJsonPipelineContext() {
    return new JsonPipelineContext(cacheAdapter, metricRegistry, getContextProperties());
  }

}
