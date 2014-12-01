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
package io.wcm.dromas.pipeline.cache.couchbase.impl.provider;

import com.couchbase.client.java.AsyncBucket;

/**
 * Provides access to a preconfigured couchbase client.
 */
public interface CouchbaseClientProvider {

  /**
   * @return true if Couchbase caching is enabled and configured properly.
   */
  boolean isEnabled();

  /**
   * @return Default bucket as configured for the couchbase client provider.
   */
  AsyncBucket getCacheBucket();

  /**
   * @return Prefix for all caching keys.
   */
  String getKeyPrefix();

}
