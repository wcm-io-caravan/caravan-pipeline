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
import rx.functions.Func1;

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
   * a simple way to raise an exception in case that expected content is not present in the pipeline's JSON
   * @param jsonPath a JSONPath expression
   * @param statusCode the appropriate status code to send to the client if the assumption fails
   * @param msg the expression to look for
   * @return a pipeline that fails with a {@link JsonPipelineInputException} if no content is found at the given path
   */
  JsonPipeline assertExists(String jsonPath, int statusCode, String msg);

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
   * Applies a custom transformation on this pipeline's JSON content (e.g. a HAL representation)
   * @param transformationId an id that is unique for the given transformation.
   * @param mapping a function that is given the root node of the Json output, and will return the new root
   * @return a new pipeline that will emit the result of the transformation
   */
  JsonPipeline applyTransformation(String transformationId, Func1<JsonNode, JsonNode> mapping);

  /**
   * Make the result of the current pipeline cacheable
   * @param strategy - specifies details for the caching behavior
   * @return a new cache-aware pipeline with the same response as the current pipeline
   */
  JsonPipeline addCachePoint(CacheStrategy strategy);

  /**
   * Catches all exceptions from any of the previous pipeline steps, and passes them to the given exception handler
   * function, together with a fallback content object that is initialized with the appropriate status code for the
   * given exception, and the max-age value set to 0.
   * Based on the type of the exception and the status code, the exception handler function can decide to return
   * fallback content, rethrow the exception as it is, or wrap it in an exception with a more informative error message.
   * {@link JsonPipelineExceptionHandlers} contains some common exception handling strategies for providing fallback
   * content or rethrowing 404 and 50x errors.
   * @param exceptionHandler the function to call when an exception is caught
   * @return a new pipeline with the same descriptor but additional exception handling behaviour from the given function
   * @see JsonPipelineExceptionHandlers
   */
  JsonPipeline handleException(JsonPipelineExceptionHandler exceptionHandler);

  /**
   * allows to subscribe to the full pipeline output that consists of a {@link JsonNode} payload and some additional
   * metadata.
   * @return an Observable that will emit a single JsonPipelineOutput
   */
  Observable<JsonPipelineOutput> getOutput();

  /**
   * Allows to subscribe only to the payload of the pipeline (if you're not interested in metadata)
   * @return an Observable that will emit a single JSON node (of type {@link ObjectNode} or {@link ArrayNode}) when the
   *         response has been fetched (and processed by all stages of the pipeline)
   */
  Observable<JsonNode> getJsonOutput();

  /**
   * Allows to subscribe only to the serialized payload of the pipeline (if you're not interested in metadata)
   * @return an Observable that will emit a single JSON String when the response has been fetched (and processed by all
   *         stages of the pipeline)
   */
  Observable<String> getStringOutput();

  /**
   * Can be used to convert the JSON response from this pipeline into a Java object (if a simple mapping is possible)
   * TODO: remove this method to clean-up the interface? default mapping to java object with jackson is trivial to do
   * anyway
   * @param clazz a POJO class that is suitable for automatic Json-to-Object Mapping
   * @return an Observable that will emit a single object when the response has been retrieved
   * @param <T> Mapping type
   */
  <T> Observable<T> getTypedOutput(Class<T> clazz);


}
