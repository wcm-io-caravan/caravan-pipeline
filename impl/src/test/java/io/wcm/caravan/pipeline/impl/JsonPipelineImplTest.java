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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.just;
import io.wcm.caravan.commons.jsonpath.impl.JsonPathDefaultConfig;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.request.RequestTemplate;
import io.wcm.caravan.io.http.response.Response;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument.Bicycle;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument.Book;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
import com.jayway.jsonpath.InvalidPathException;
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
    Response response = getJsonResponse(200, json);
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(response), caching);
  }

  /**
   * @param code the HTTP status code to send with the response
   * @return pipeline for the given input content
   */
  private JsonPipeline newPipelineWithResponseCode(int code) {
    // we are using some valid JSON as response body, because one of the purpose of this methods is to ensure that
    // the content is not parsed when there is a >200 response code
    Response response = getJsonResponse(code, "{ responseCode:" + code + "}");
    return new JsonPipelineImpl(SERVICE_NAME, new RequestTemplate().append("/path").request(), Observable.just(response), caching);
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
  public void plainPipelineResourceNotFound() {

    // tests that 404 responses are not parsed as JSON, but treated as an error
    JsonPipeline pipeline = newPipelineWithResponseCode(404);
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
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
  public void extractJsonPathNotFound() {

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
  public void extractResourceNotFound() {

    // tests that 404 responses from the transport layers are properly handled
    JsonPipeline pipeline = newPipelineWithResponseCode(404).extract("$..", "targetproperty");
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
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
  public void collectAllArrayEntries() throws JSONException {

    // test extraction of all items from multiple arrays
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers[*]", "extracted");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [1,2,3,4,5,6,7,8] }", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }


  @Test
  public void collectSpecificArrayEntries() throws JSONException {

    // test extraction of the third item from each of multiple arrays
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
  public void collectJsonPathNotFound() {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.extract("$a.numbers", "extracted");

    collected.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called with a PathNotFoundException
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
  public void assertExistsSuccess() throws JSONException {

    // check that a fulfilled assertion will not influence the pipeline output
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.a", new RuntimeException("a not found"));

    String output = pipeline.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void assertExistsFails() {

    // check that an unfulfilled assertion will throw the specified exception
    RuntimeException expectedEx = new RuntimeException("b not found");

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.b", expectedEx);

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(eq(expectedEx));
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void assertExistsAfterExtract() {

    // check that assertExist also fails with the given runtime exception if the pipeline's result is null because of a preceding extract
    RuntimeException expectedEx = new RuntimeException("a not found");

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .extract("$[?(@.a==456)]", null) // this will *not* match the root object, so the pipeline's output is null
        .assertExists("$.a", expectedEx); // this used to fail with an InvalidArgumentException within JsonPathSelector, that is now avoided

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(eq(expectedEx));
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void assertExistsWithInvalidPath() {

    // check that assertExist fails with an InvalidPathException if the given JSONPath is not valid
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.a[invalid]", new RuntimeException());

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(InvalidPathException.class));
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

    CacheStrategy strategy = CacheStrategies.timeToLive(1, TimeUnit.SECONDS);

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline cached = a.addCachePoint(strategy);

    String cacheKey = "abcdef";

    when(caching.getCacheKey(SERVICE_NAME, a.getDescriptor()))
    .thenReturn(cacheKey);

    when(caching.get(eq(cacheKey), eq(strategy), any(Request.class)))
    .thenReturn(Observable.just("{ metadata: {}, content: {b: 456}}"));

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

    CacheStrategy strategy = CacheStrategies.timeToLive(1, TimeUnit.SECONDS);

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

    // put must have been called with an cache envelope version of the JSON, that contains an additional _cacheInfo
    verify(caching).put(eq(cacheKey), Matchers.argThat(new BaseMatcher<String>() {

      @Override
      public boolean matches(Object item) {
        ObjectNode storedNode = JacksonFunctions.stringToObjectNode(item.toString());

        return storedNode.has("metadata")
            && storedNode.has("content")
            && storedNode.get("content").get("a").asInt() == 123;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected storedObject to contain original value & _cacheInfo");
      }

    }), eq(strategy), any(Request.class));

    // the _cacheInfo however should not be contained in the output
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleExceptionSuccess() throws JSONException {

    String responseJson = "{a: 123}";

    JsonPipeline pipeline = newPipelineWithResponseBody(responseJson)
        .handleException((status, ex) -> {
          fail("this should not be called");
          return just(JacksonFunctions.stringToNode(responseJson));
        });

    String output = pipeline.getStringOutput().toBlocking().first();

    JSONAssert.assertEquals(responseJson, output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleException404Rethrow() {

    RuntimeException rethrown = new RuntimeException();

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .handleException((status, ex) -> {
          assertEquals(404, status);
          throw rethrown;
        });

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void handleException404Fallback() throws JSONException {

    String fallbackJson = "{fallback: true}";

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .handleException((status, ex) -> {
          assertEquals(404, status);
          return just(JacksonFunctions.stringToNode(fallbackJson));
        });

    String output = pipeline.getStringOutput().toBlocking().first();

    JSONAssert.assertEquals(fallbackJson, output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleException500Rethrow() {

    RuntimeException rethrown = new RuntimeException("Rethrown");

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        .handleException((status, ex) -> {
          assertEquals(500, status);
          throw rethrown;
        });

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, caching);
  }

  @Test
  public void handleException500Fallback() throws JSONException {

    String fallbackJson = "{fallback: true}";

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        .handleException((status, ex) -> {
          assertEquals(500, status);
          return just(JacksonFunctions.stringToNode(fallbackJson));
        });


    String output = pipeline.getStringOutput().toBlocking().first();

    JSONAssert.assertEquals(fallbackJson, output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleExceptionChaining500() {

    RuntimeException rethrown = new RuntimeException("Rethrown");

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        // first register an exception handler that provides fallback content for a 404, but rethrows any other exceptions
        .handleException((status, ex) -> {
          if (status == 404) {
            return Observable.just(JacksonFunctions.stringToNode("{}"));
          }
          throw ex;
        })
        // then add the other handler that should be actually triggered here
        .handleException((status, ex) -> {
          assertEquals(500, status);
          throw rethrown;
        });

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, caching);
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

  static Response getJsonResponse(int statusCode, String content) {
    return Response.create(statusCode, "Ok", Collections.emptyMap(), content, Charsets.UTF_8);
  }

}
