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

import java.util.List;
import java.util.SortedSet;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.common.performance.PerformanceMetrics;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import rx.Observable;

/**
 * A pipeline aids consuming/orchestrating services to transform, merge and cache JSON responses from a
 * {@link CaravanHttpClient} and allows to
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
@ProviderType
public interface JsonPipeline {

  /**
   * Provides a string representation of all the actions executed by this pipeline. Description is supposed to help
   * in creation of understandable logging messages. It could be also used to generate cache keys.
   * See sample of action description: "GET(serviceId/path)+SELECT($..someProperty into targetPeropty)"
   * @return description of pipeline actions
   */
  String getDescriptor();

  /**
   * @return all logical service IDs that were used in generating the pipeline's result
   */
  SortedSet<String> getSourceServices();

  /**
   * @return all resilient HTTP requests involved in producing the pipeline's output
   */
  List<CaravanHttpRequest> getRequests();

  /**
   * @return pipeline performance metrics
   */
  PerformanceMetrics getPerformanceMetrics();

  /**
   * Raises an exception in case, if expected content is not present in the actual JSON node of this pipeline.
   * @param jsonPath a JSONPath expression
   * @param statusCode the appropriate status code to send to the client if the assumption fails
   * @param msg the expression to look for
   * @return a pipeline that fails with a {@link JsonPipelineInputException} if no content is found at the given path
   */
  JsonPipeline assertExists(String jsonPath, int statusCode, String msg);

  /**
   * Extracts a single (first) property from the pipeline's response by specifying a JSONPath expression. An extraction
   * result is saved as a single JSON property in the root node of the result JSON object.
   * @param jsonPath a JSONPath expression that matches a single leaf in the source JSON object
   * @return a new pipeline that will only emit a single JSON object with the extraction result
   */
  JsonPipeline extract(String jsonPath);

  /**
   * Extracts a single (first) property from the pipeline's response by specifying a JSONPath expression. An extraction
   * result is saved as a single JSON property in a new node of the result JSON object and named by the targetProperty
   * parameter.
   * @param jsonPath a JSONPath expression that matches a single leaf in the source JSON object
   * @param targetProperty the name of the single property to save extraction results in a response JSON object
   * @return a new pipeline that will only emit a single JSON object with the extraction result
   */
  JsonPipeline extract(String jsonPath, String targetProperty);

  /**
   * Extracts multiple properties (or individual entries from a property array!) from the pipeline's response by
   * specifying a JSONPath expression. An extraction result is saved as a JSON array in the root node of the result JSON
   * object.
   * @param jsonPath a JSONPath expression that can match multiple items in the source JSON object
   * @return a new pipeline that will only emit a single JSON object with the extraction result
   */
  JsonPipeline collect(String jsonPath);

  /**
   * Extracts multiple properties (or individual entries from a property array!) from the pipeline's response by
   * specifying a JSONPath expression. An extraction result is saved as a JSON array in a new node of the result JSON
   * object and named by the targetProperty parameter.
   * @param jsonPath a jsonPath expression that can match multiple items in the source JSON object
   * @param targetProperty the name of the single node to save extraction results in a response JSON object
   * @return a new pipeline that will only emit a single JSON object with the extraction result
   */
  JsonPipeline collect(String jsonPath, String targetProperty);

  /**
   * Merges/Zips the JSON response objects of this pipeline and another pipeline, specifying by secondarySource
   * parameter. A merge result is a new JSON object, where the JSON object of this pipeline and the JSON object of the
   * secondary source are saved on the root node level.
   * @param secondarySource another pipeline that returns a single JSON object to be merged
   * @return a new pipeline that will emit a single JSON object with the merge result
   */
  JsonPipeline merge(JsonPipeline secondarySource);

  /**
   * Merges/Zips the JSON response objects of this pipeline and another pipeline, specifying by secondarySource
   * parameter. A merge result is a new JSON object, where the JSON object of this pipeline is saved on the root node
   * level. The JSON object of the secondary source is saved as a new node in the result JSON object and named by the
   * targetProperty parameter.
   * @param secondarySource another pipeline that returns a single JSON object to be merged
   * @param targetProperty the name of the single node to save the content of the secondary source in merged JSON
   *          object
   * @return a new pipeline that will emit a single JSON object with the merge result
   */
  JsonPipeline merge(JsonPipeline secondarySource, String targetProperty);

  /**
   * Applies an action on this pipeline, specifying an algorithm by the implementation of {@link JsonPipelineAction}.
   * @param action a JSON pipeline action, that provides the actual algorithm
   * @return a new pipeline that emits the result of the action execution
   */
  JsonPipeline applyAction(JsonPipelineAction action);

  /**
   * Makes the result of the current pipeline cacheable.
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
   * Subscribes asynchronous client to the full pipeline output that consists of a {@link JsonNode} payload and some
   * additional metadata. Any first subscription calls a pipeline to execute listed in it operations.
   * @return an Observable that will emit a single JsonPipelineOutput
   */
  Observable<JsonPipelineOutput> getOutput();

  /**
   * Subscribes asynchronous client to the JSON payload of the pipeline. Use it for the subscription, if you're not
   * interested in metadata. Any first subscription calls a pipeline to execute listed in it operations.
   * @return an Observable that will emit a single JSON node (of type {@link ObjectNode} or {@link ArrayNode}) when the
   *         response has been fetched (and processed by all stages of the pipeline)
   */
  Observable<JsonNode> getJsonOutput();

  /**
   * Subscribes asynchronous client to the serialized payload of the pipeline. Use it for the subscription, if you're
   * not interested in metadata. Any first subscription calls a pipeline to execute listed in it operations.
   * @return an Observable that will emit a single JSON String when the response has been fetched (and processed by all
   *         stages of the pipeline)
   */
  Observable<String> getStringOutput();

}
