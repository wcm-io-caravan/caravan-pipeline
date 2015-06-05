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

import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.List;

import org.slf4j.Logger;

import com.google.common.collect.Multimap;

/**
 * Specific HAL client pipeline action with convenience setter.
 */
public interface HalClientAction extends JsonPipelineAction {

  /**
   * @param cacheStrategy Caching strategy
   * @return This HAL client action
   */
  HalClientAction setCacheStrategy(CacheStrategy cacheStrategy);

  /**
   * @param exceptionHandlers Exception Handlers
   * @return This HAL client action
   */
  HalClientAction setExceptionHandlers(List<JsonPipelineExceptionHandler> exceptionHandlers);

  /**
   * @param logger Logger
   * @return This HAL client action
   */
  HalClientAction setLogger(Logger logger);

  /**
   * @param httpHeaders HTTP headers
   * @return This HAL client action
   */
  HalClientAction setHttpHeaders(Multimap<String, String> httpHeaders);

}
