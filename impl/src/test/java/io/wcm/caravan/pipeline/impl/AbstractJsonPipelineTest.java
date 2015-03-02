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

import io.wcm.caravan.commons.jsonpath.impl.JsonPathDefaultConfig;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.request.RequestTemplate;
import io.wcm.caravan.io.http.response.Response;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.cache.CacheDateUtils;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.operators.CachePointTransformer.CacheEnvelope;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;

import com.google.common.base.Charsets;
import com.jayway.jsonpath.Configuration;


public class AbstractJsonPipelineTest {

  protected static final String SERVICE_NAME = "testService";

  @Mock
  protected CacheAdapter caching;
  @Mock
  protected Observer<String> stringObserver;
  @Mock
  protected Observer<BooksDocument> booksObserver;

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
    Response response = getJsonResponse(200, json, -1);
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(response), caching);
  }

  /**
   * @param json the input content for pipeline
   * @return pipeline for the given input content
   */
  protected JsonPipeline newPipelineWithResponseBodyAndMaxAge(String json, int maxAge) {
    Response response = getJsonResponse(200, json, maxAge);
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(response), caching);
  }

  /**
   * @param code the HTTP status code to send with the response
   * @return pipeline for the given input content
   */
  protected JsonPipeline newPipelineWithResponseCode(int code) {
    // we are using some valid JSON as response body, because one of the purpose of this methods is to ensure that
    // the content is not parsed when there is a >200 response code
    Response response = getJsonResponse(code, "{ responseCode:" + code + "}", -1);
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(response), caching);
  }

  /**
   * @param t the simulated error in the transport layer
   * @return pipeline that will fail when getting its input data
   */
  protected JsonPipeline newPipelineWithResponseError(Throwable t) {
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.error(t), caching);
  }

  static String getJsonString(String resourcePath) {
    try {
      return IOUtils.toString(JsonPipelineImplTest.class.getResourceAsStream(resourcePath));
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to read json response from " + resourcePath);
    }
  }

  static Response getJsonResponse(int statusCode, String content, int maxAge) {

    // the cache-control unit-tests expect the content to be cacheable
    HashMap<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
    if (maxAge > 0 && statusCode == 200) {
      headers.put("Cache-Control", Arrays.asList("max-age: " + maxAge));
    }

    return Response.create(statusCode, "Ok", headers, content, Charsets.UTF_8);
  }

  public AbstractJsonPipelineTest() {
    super();
  }

  static Observable<String> cachedContent(String json, int ageInSeconds) {

    CacheEnvelope envelope = CacheEnvelope.from200Response(JacksonFunctions.stringToNode(json), new TreeSet<String>(), new LinkedList<Request>(), "cacheKey",
        "descriptor");
    envelope.setGeneratedDate(CacheDateUtils.formatRelativeTime(-ageInSeconds));

    return Observable.just(envelope.getEnvelopeString());
  }
}
