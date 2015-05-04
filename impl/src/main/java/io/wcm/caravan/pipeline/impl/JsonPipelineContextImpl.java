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
package io.wcm.caravan.pipeline.impl;

import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.util.Map;

import com.codahale.metrics.MetricRegistry;


/**
 * Keeps non modifiable references to instances of objects provided by factory {@link JsonPipelineFactoryImpl} for each
 * new implementation of {@link JsonPipelineImpl}. Each entry of JSON pipeline context must exist during the pipeline
 * life cycle. Temporary objects or primitives should not be a part of context.
 */
public class JsonPipelineContextImpl implements JsonPipelineContext {

  private final JsonPipelineFactory factory;

  private final CacheAdapter cacheAdapter;

  private final MetricRegistry metricRegistry;

  private final Map<String, String> cacheMetadataProperties;

  /**
   * @param cacheAdapter a caching layer / cache adapter to use
   * @param metricRegistry metrics registry
   * @param contextProperties additional metadata in string property form
   */
  public JsonPipelineContextImpl(JsonPipelineFactory factory, CacheAdapter cacheAdapter, MetricRegistry metricRegistry, Map<String, String> contextProperties) {
    this.factory = factory;
    this.cacheAdapter = cacheAdapter;
    this.metricRegistry = metricRegistry;
    this.cacheMetadataProperties = contextProperties;
  }

  @Override
  public JsonPipelineFactory getFactory() {
    return factory;
  }

  @Override
  public CacheAdapter getCacheAdapter() {
    return this.cacheAdapter;
  }

  public MetricRegistry getMetricRegistry() {
    return this.metricRegistry;
  }

  @Override
  public Map<String, String> getProperties() {
    return this.cacheMetadataProperties;
  }

}
