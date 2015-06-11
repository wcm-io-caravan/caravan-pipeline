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

import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

abstract class AbstractHalClientAction implements HalClientAction {

  private CacheStrategy cacheStrategy;
  private List<JsonPipelineExceptionHandler> exceptionHandlers = Collections.emptyList();
  private Logger logger;
  private Multimap<String, String> httpHeaders = ImmutableMultimap.of();

  @Override
  public HalClientAction setCacheStrategy(CacheStrategy value) {
    this.cacheStrategy = value;
    return this;
  }

  @Override
  public HalClientAction setExceptionHandlers(List<JsonPipelineExceptionHandler> value) {
    this.exceptionHandlers = value;
    return this;
  }

  @Override
  public HalClientAction setLogger(Logger value) {
    this.logger = value;
    return this;
  }

  @Override
  public HalClientAction setHttpHeaders(Multimap<String, String> value) {
    this.httpHeaders = value;
    return this;
  }

  /**
   * @return Returns the cacheStrategy.
   */
  public CacheStrategy getCacheStrategy() {
    return this.cacheStrategy;
  }

  /**
   * @return Returns the exceptionHandlers.
   */
  public List<JsonPipelineExceptionHandler> getExceptionHandlers() {
    return ImmutableList.copyOf(this.exceptionHandlers);
  }

  /**
   * @return Current Logger
   */
  public Logger getLogger() {
    return logger;
  }

  /**
   * @return True if has a logger
   */
  public boolean hasLogger() {
    return logger != null;
  }

  /**
   * @return Returns the httpHeaders.
   */
  public Multimap<String, String> getHttpHeaders() {
    return this.httpHeaders;
  }

}
