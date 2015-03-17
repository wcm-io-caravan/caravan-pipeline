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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.wcm.caravan.commons.jsonpath.impl.JsonPathDefaultConfig;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.AbstractCaravanTestCase;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer.CacheEnvelope;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.Configuration;


public class AbstractJsonPipelineTest extends AbstractCaravanTestCase {

  protected static final String SERVICE_NAME = "testService";

  @Mock
  protected Observer<String> stringObserver;
  @Mock
  protected Observer<BooksDocument> booksObserver;

  public AbstractJsonPipelineTest() {
    super();
  }

  @BeforeClass
  public static void initJsonPath() {
    Configuration.setDefaults(JsonPathDefaultConfig.INSTANCE);
  }

  protected static String getBooksString() {
    return getJsonString("/json/books.json");
  }

  /**
   * @param json the input content for pipeline
   * @return pipeline for the given input content
   */
  protected JsonPipeline newPipelineWithResponseBody(String json) {
    CaravanHttpResponse response = getJsonResponse(200, json, -1);
    return new JsonPipelineImpl(new CaravanHttpRequestBuilder(SERVICE_NAME).append("/path").build(), Observable.just(response), getJsonPipelineContext());
  }

  /**
   * @param json the input content for pipeline
   * @return pipeline for the given input content
   */
  protected JsonPipeline newPipelineWithResponseBodyAndMaxAge(String json, int maxAge) {
    CaravanHttpResponse response = getJsonResponse(200, json, maxAge);
    return new JsonPipelineImpl(new CaravanHttpRequestBuilder(SERVICE_NAME).append("/path").build(), Observable.just(response), getJsonPipelineContext());
  }

  /**
   * @param code the HTTP status code to send with the response
   * @return pipeline for the given input content
   */
  protected JsonPipeline newPipelineWithResponseCode(int code) {
    // we are using some valid JSON as response body, because one of the purpose of this methods is to ensure that
    // the content is not parsed when there is a >200 response code
    CaravanHttpResponse response = getJsonResponse(code, "{ responseCode:" + code + "}", -1);
    return new JsonPipelineImpl(new CaravanHttpRequestBuilder(SERVICE_NAME).append("/path").build(), Observable.just(response), getJsonPipelineContext());
  }

  /**
   * @param t the simulated error in the transport layer
   * @return pipeline that will fail when getting its input data
   */
  protected JsonPipeline newPipelineWithResponseError(Throwable t) {
    return new JsonPipelineImpl(new CaravanHttpRequestBuilder(SERVICE_NAME).append("/path").build(), Observable.error(t), getJsonPipelineContext());
  }

  static String getJsonString(String resourcePath) {
    try {
      return IOUtils.toString(AbstractJsonPipelineTest.class.getResourceAsStream(resourcePath));
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to read json response from " + resourcePath);
    }
  }

  static CaravanHttpResponse getJsonResponse(int statusCode, String content, int maxAge) {

    // the cache-control unit-tests expect the content to be cacheable
    Multimap<String, String> headers = LinkedHashMultimap.create();
    if (maxAge > 0 && statusCode == 200) {
      headers.put("Cache-Control", "max-age: " + maxAge);
    }

    return CaravanHttpResponse.create(statusCode, "Ok", headers, content, Charsets.UTF_8);
  }

  static JsonNode getJsonNode(String jsonText) {
    try {
      return new ObjectMapper().readTree(jsonText);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }


  static Observable<String> cachedContent(String json, int ageInSeconds) {

    CacheEnvelope envelope = CacheEnvelope.from200Response(JacksonFunctions.stringToNode(json), new LinkedList<CaravanHttpRequest>(),
        "cacheKey", "descriptor", getcacheMetadataProperties());
    envelope.setGeneratedDate(CacheDateUtils.formatRelativeTime(-ageInSeconds));

    return Observable.just(envelope.getEnvelopeString());
  }

  final class ExceptionExpectingObserver implements Observer<String> {

    private final Exception expected;

    protected ExceptionExpectingObserver(Exception expected) {
      this.expected = expected;
    }

    @Override
    public void onNext(String t) {
      fail("Only onError should be called");
    }

    @Override
    public void onCompleted() {
      fail("Only onError should be called");
    }

    @Override
    public void onError(Throwable e) {
      assertTrue(e instanceof JsonPipelineInputException);
      assertEquals(500, ((JsonPipelineInputException)e).getStatusCode());
      assertEquals(e.getCause(), expected);
    }
  }
}
