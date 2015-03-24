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
package io.wcm.caravan.pipeline.cache.guava.impl;

import io.wcm.caravan.pipeline.cache.CachePersistencyOptions;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;

import com.codahale.metrics.MetricRegistry;


public class AbstractGuavaTestCase {

  @Rule
  public OsgiContext context = new OsgiContext();
  protected GuavaCacheAdapter cacheAdapter;
  protected MetricRegistry metricRegistry;
  protected CachePersistencyOptions options;

  public AbstractGuavaTestCase() {
    super();
  }

  @SuppressWarnings("unchecked")
  @Before
  public void before() {
    metricRegistry = new MetricRegistry();
    context.registerService(MetricRegistry.class, metricRegistry);
    cacheAdapter = context.registerInjectActivateService(new GuavaCacheAdapter(), getCacheConfig());
    options = new CachePersistencyOptions(100, 10, true);
  }

  protected Map<String, Object> getCacheConfig() {
    return Collections.EMPTY_MAP;
  }

}
