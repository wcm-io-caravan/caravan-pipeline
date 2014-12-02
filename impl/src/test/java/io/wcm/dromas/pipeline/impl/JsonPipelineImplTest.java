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
package io.wcm.dromas.pipeline.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import io.wcm.dromas.commons.jsonpath.impl.JsonPathDefaultConfig;
import io.wcm.dromas.io.http.request.Request;
import io.wcm.dromas.io.http.request.RequestTemplate;
import io.wcm.dromas.io.http.response.Response;
import io.wcm.dromas.pipeline.JsonPipeline;
import io.wcm.dromas.pipeline.JsonPipelineInputException;
import io.wcm.dromas.pipeline.cache.CacheStrategies;
import io.wcm.dromas.pipeline.cache.CacheStrategy;
import io.wcm.dromas.pipeline.cache.spi.CacheAdapter;
import io.wcm.dromas.pipeline.impl.testdata.BooksDocument;
import io.wcm.dromas.pipeline.impl.testdata.BooksDocument.Bicycle;
import io.wcm.dromas.pipeline.impl.testdata.BooksDocument.Book;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import rx.Observable;
import rx.Observer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.PathNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineImplTest {

  private static final String SERVICE_NAME = "testService";

  @Mock
  private CacheAdapter caching;

  @Mock
  private Observer<String> stringObserver;

  @Mock
  private Observer<BooksDocument> booksObserver;

  @BeforeClass
  public static void initJsonPath() {
    Configuration.setDefaults(JsonPathDefaultConfig.INSTANCE);
  }

  /**
   * @param json the input content for pipeline
   * @return pipeline for the given input content
   */
  private JsonPipeline newPipelineWithResponseBody(String json) {
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(getJsonResponse(json)), caching);
  }

  /**
   * @param t the simulated error in the transport layer
   * @return pipeline that will fail when getting its input data
   */
  private JsonPipeline newPipelineWithResponseError(Throwable t) {
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.error(t), caching);
  }


  @Test
  public void plainPipelineOutput() throws JSONException {

    // check that a plain pipeline will return the JSON emitted by the transport layer
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString());

    JsonNode output = pipeline.getOutput().toBlocking().single();
    JSONAssert.assertEquals(getBooksString(), JacksonFunctions.nodeToString(output), JSONCompareMode.STRICT_ORDER);
  }


  @Test
  public void plainPipelineStringOutput() throws JSONException {

    // check that a plain pipeline will return the JSON emitted by the transport layer
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString());

    String output = pipeline.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals(getBooksString(), output, JSONCompareMode.STRICT_ORDER);
  }

  @Test
  public void plainPipelineTypedOutput() {

    // check that the book.json is also properly mapped to the BooksDocument pojo
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString());

    BooksDocument doc = pipeline.getTypedOutput(BooksDocument.class).toBlocking().single();

    assertEquals("number of books", 4, doc.getStore().getBook().size());
    assertNotNull("existence of bike", doc.getStore().getBicycle());

    Book firstBook = doc.getStore().getBook().get(0);
    assertEquals("book category", "reference", firstBook.getCategory());
    assertEquals("book author", "Nigel Rees", firstBook.getAuthor());
    assertEquals("book title", "Sayings of the Century", firstBook.getTitle());
    assertNull("book isbn", firstBook.getIsbn());
    assertEquals("book price", firstBook.getPrice(), 8.95, 0.0);

    Bicycle bike = doc.getStore().getBicycle();
    assertEquals("bicycle colour", "red", bike.getColor());
    assertEquals("bicycle price", 19.95, bike.getPrice(), 0.0);
  }

  @Test
  public void plainPipelineTypedOutputUnknownProperty() {

    //check handling of property "frame" which is not available in the Bicycle class
    String json = "{store: { bicycle: { color: 'black', price: 100.0, frame: 'steel'}}}";

    JsonPipeline pipeline = newPipelineWithResponseBody(json);
    pipeline.getTypedOutput(BooksDocument.class).subscribe(booksObserver);

    // make sure that only #error was called, and there was no other interaction with the obsever or cache
    verify(booksObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(booksObserver, caching);
  }

  @Test
  public void plainPipelineTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex);
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    verify(stringObserver).onError(ex);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void plainPipelineParseError() {

    // tests that invalid JSOn in the response is properly handled
    JsonPipeline pipeline = newPipelineWithResponseBody("<this> is not json</this>");
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void plainPipelineParseErrorTypedOutput() {

    // tests that invalid JSOn in the response is properly handled
    JsonPipeline pipeline = newPipelineWithResponseBody("<this> is not json</this>");
    pipeline.getTypedOutput(BooksDocument.class).subscribe(booksObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(booksObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(booksObserver, caching);
  }


  @Test
  public void extractObject() throws JSONException {

    // test extraction of a single *Object* property
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a", "extracted");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: { label: 'abc' }}", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), extracted.getDescriptor());
  }

  @Test
  public void extractObjectNoTargetProperty() throws JSONException {

    // test extraction of a single *Object* property *without specify a target property*
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a", "");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ label: 'abc' }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), extracted.getDescriptor());
  }

  @Test
  public void extractArray() throws JSONException {

    // test extraction of a single *Array* property
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }}");
    JsonPipeline extracted = pipeline.extract("$.a.numbers", "extracted");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [1,2,3,4] }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), extracted.getDescriptor());
  }

  @Test
  public void extractArrayNoTargetProperty() throws JSONException {

    // test extraction of a single *Array* property *without specify a target property*
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }}");
    JsonPipeline extracted = pipeline.extract("$.a.numbers", "");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[1,2,3,4]", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), extracted.getDescriptor());
  }

  @Test
  public void extractNoResult() throws JSONException {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a[?(@.label=='def')]", "extracted");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: null }", output, JSONCompareMode.STRICT);
  }


  @Test
  public void extractPathNotFound() {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$a.numbers", "extracted");

    extracted.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    verify(stringObserver).onError(any(PathNotFoundException.class));
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void extractTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex).extract("$..", "targetproperty");

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    verify(stringObserver).onError(ex);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void collectStrings() throws JSONException {

    // test extraction of a multiple *String* properties
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline collected = pipeline.collect("$..label", "extracted");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: ['abc', 'def'] }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectStringsNoTargetProperty() throws JSONException {

    // test extraction of a multiple *String* properties
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline collected = pipeline.collect("$..label", "");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("['abc', 'def']", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectArrays() throws JSONException {

    // test extraction of a multiple *Array* properties
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers", "extracted");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [[1,2,3,4], [5,6,7,8]] }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectArrayEntries() throws JSONException {

    // test extraction of multiple items with an *Array*
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers[3]", "extracted");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [4,8] }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectArrayEntriesNoTargetProperty() throws JSONException {

    // test extraction of multiple items with an *Array*
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers[3]", "");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[4,8]", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectNoResult() throws JSONException {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$.a[?(@.label=='def')]", "extracted");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [] }", output, JSONCompareMode.STRICT);
  }

  @Test
  public void collectPathNotFound() {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.extract("$a.numbers", "extracted");

    collected.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    verify(stringObserver).onError(any(PathNotFoundException.class));
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void collectTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex).collect("$..", "targetproperty");

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    verify(stringObserver).onError(ex);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void mergedPipelineSuccess() throws JSONException {

    // test successful merging of one pipeline into the other
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b, "c");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, c: {b: 456}}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergedPipelineNoTargetProperty() throws JSONException {

    // test successful merging of one pipeline into the other *without adding another property*
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b, null);

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergedPipelineTransportError1() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseError(ex);

    JsonPipeline merged = a.merge(b, "c");
    merged.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(ex);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void mergedPipelineTransportError2() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline a = newPipelineWithResponseError(ex);
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b, "c");
    merged.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(ex);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void cacheHit() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(1);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(caching.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(caching.get(eq(cacheKey), eq(strategy), any(Request.class)))
    .thenReturn(Observable.just("{b: 456}"));

    String output = cached.getStringOutput().toBlocking().single();

    // only getCacheKey and get should have been called to check if it is available in the cache
    verify(caching).getCacheKey(SERVICE_NAME, a.getDescriptor());
    verify(caching).get(eq(cacheKey), eq(strategy), any(Request.class));
    verifyNoMoreInteractions(caching);

    // make sure that the version from the cache is emitted in the response
    JSONAssert.assertEquals("{b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void cacheMissAndStore() throws JSONException {

    CacheStrategy strategy = CacheStrategies.timeToLive(1);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(caching.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(caching.get(eq(cacheKey), eq(strategy), any(Request.class)))
    .thenReturn(Observable.empty());

    String output = cached.getStringOutput().toBlocking().single();

    // get must have been called to check if the document is available in the cache
    verify(caching).get(eq(cacheKey), eq(strategy), any(Request.class));

    // put must have been called with an altered version of the JSON, that contains an additional _cacheInfo
    verify(caching).put(eq(cacheKey), Matchers.argThat(new BaseMatcher<String>() {

      @Override
      public boolean matches(Object item) {
        ObjectNode storedNode = (ObjectNode)JacksonFunctions.stringToNode(item.toString());

        return storedNode.get("a").asInt() == 123 && storedNode.has("_cacheInfo");
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected storedObject to contain original value & _cacheInfo");
      }

    }), eq(strategy), any(Request.class));

    // the _cacheInfo however should not be contained in the output
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

  // helper methods to get example JSON response data from src/test/resources

  static String getBooksString() {
    return getJsonString("/json/books.json");
  }

  static String getJsonString(String resourcePath) {
    try {
      return IOUtils.toString(JsonPipelineImplTest.class.getResourceAsStream(resourcePath));
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to read json response from " + resourcePath);
    }
  }

  static Response getJsonResponse(String content) {
    return Response.create(200, "Ok", Collections.emptyMap(), content, Charsets.UTF_8);
  }

}
