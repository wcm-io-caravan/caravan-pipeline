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
package io.wcm.dromas.pipeline.cache.couchbase.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import io.wcm.dromas.commons.couchbase.CouchbaseClientProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.couchbase.client.java.AsyncBucket;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class CouchbaseCacheAdapterTest {

  @Rule
  public OsgiContext context = new OsgiContext();

  @Mock
  private AsyncBucket bucket;
  @Mock
  private CouchbaseClientProvider couchbaseClientProvider;

  private CouchbaseCacheAdapter adapter;

  @Before
  public void setUp() {
    when(couchbaseClientProvider.getCacheBucket()).thenReturn(bucket);
    context.registerService(CouchbaseClientProvider.class, couchbaseClientProvider);
    adapter = context.registerInjectActivateService(new CouchbaseCacheAdapter(), ImmutableMap.<String, Object>builder()
        .put(CouchbaseCacheAdapter.CACHE_KEY_PREFIX_PROPERTY, "prefix:")
        .build());
  }

  @Test
  public void getShortCacheKey() {
    String cacheKey = adapter.getCacheKey("example", "/a/b/c");
    assertEquals("prefix:example:/a/b/c", cacheKey);
  }

  @Test
  public void getLongCacheKey() {
    String cacheKey = adapter.getCacheKey("example", StringUtils.repeat("/a/b/c", 500));
    assertEquals(250, cacheKey.length());
    assertTrue("beginning of cache key is left untouched?", cacheKey.startsWith("prefix:example:/a/b/c"));
  }

}
