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

import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.pipeline.cache.CacheStrategy;

import java.util.SortedSet;

import rx.Observable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A pipeline that aids consuming/orchestrating services to transform, merge and cache JSON responses from a
 * {@link ResilientHttp} and allows to
 * <ul>
 * <li>select only specific parts of the JSON tree with a JsonPath expression (using {@link #collect(String, String)})</li>
 * <li>merge all content of another pipeline into a new property of its own result document (using
 * {@link #merge(JsonPipeline, String)})</li>
 * <li>cache the original result, or the result of any transformation/aggregation step (using
 * {@link #addCachePoint(CacheStrategy)}</li>
 * <li>access the overall output either as a Jackson {@link JsonNode}, a JSON string, or as a type-mapped Java object</li>
 * </ul>
 * Note that {@link JsonPipeline}'s are immutable. None of the methods will alter the current instance, but instead will
 * return a new instance with the desired behavior (and an updated descriptor that will be used to generate unique
 * cache keys)
 */
public interface JsonPipeline {

  /**
   * Provides a string representation of all the actions executed by this pipeline. This is supposed to help create
   * understandable logging messages, and can also be used to generate cache keys.
   * @return e.g. "GET(serviceName/path)+SELECT($..someProperty into targetPeropty)"
   */
  String getDescriptor();

  /**
   * @return all logical service names that were used in generating the pipeline's result
   */
  SortedSet<String> getSourceServices();

  /**
   * Assert that the Response contains content at the given JsonPath
   * @param jsonPath the expression to look for
   * @param ex the exception to fail with
   * @return a new pipeline with the same response and descriptor
   */
  JsonPipeline assertExists(String jsonPath, RuntimeException ex);

  /**
   * Select a single property from the pipeline's response by specifying a JSON path.
   * If targetProperty is null or blank, the pipeline's response will return the (first) result of the JSONPath
   * expression. If a targetProperty is specified, the output of will be an object with a single property containing the
   * result.
   * @param jsonPath a jsonPath expression that matches a single leaf in the source JSON
   * @param targetProperty the name of the single property in new response JSON object (can be null or empty)
   * @return a new pipeline that will only emit a single JSON object with a single Object property containing the
   *         (first) result of the JSON path expression
   */
  JsonPipeline extract(String jsonPath, String targetProperty);

  /**
   * Select multiple properties (or individual entries from a property array!) from the pipeline's response by
   * specifying a JSON path. If targetProperty is null or blank, the pipeline's response will return an array with the
   * results of the JSONPath expression. If a targetProperty is specified, the output of will be an object with a single
   * property containing the result array.
   * @param jsonPath a jsonPath expression that can match multiple items in the source JSON
   * @param targetProperty the name of the single property in new response JSON object (can be null or empty)
   * @return a new pipeline that will only emit a single JSON object with a single array property containing the results
   *         of the JSON path expression
   */
  JsonPipeline collect(String jsonPath, String targetProperty);

  /**
   * Merges/Zips the JSON response object from another Pipeline into the response object from this pipeline.
   * @param secondarySource another pipeline that returns a single JSON object
   * @param targetProperty the property to add to the primary source, which will contain all content of the secondary
   *          source
   * @return a new pipeline with the merged response
   */
  JsonPipeline merge(JsonPipeline secondarySource, String targetProperty);

  /**
   * Make the result of the current pipeline cacheable
   * @param strategy - specifies details for the caching behavior
   * @return a new cache-aware pipeline with the same response as the current pipeline
   */
  JsonPipeline addCachePoint(CacheStrategy strategy);

  /**
   * @return an Observable that will emit a single JSON node (of type {@link ObjectNode} or {@link ArrayNode}) when the
   *         response has been fetched (and processed by all stages of the pipeline)
   */
  Observable<JsonNode> getOutput();

  /**
   * @return an Observable that will emit a single JSON String when the response has been fetched (and processed by all
   *         stages of the pipeline)
   */
  Observable<String> getStringOutput();

  /**
   * Can be used to convert the JSON response from this pipeline into a Java object (if a simple mapping is possible)
   * @param clazz a POJO class that is suitable for automatic Json-to-Object Mapping
   * @return an Observable that will emit a single object when the response has been retrieved
   * @param <T> Mapping type
   */
  <T> Observable<T> getTypedOutput(Class<T> clazz);

}
