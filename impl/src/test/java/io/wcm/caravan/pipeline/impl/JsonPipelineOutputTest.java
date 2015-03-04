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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument.Bicycle;
import io.wcm.caravan.pipeline.impl.testdata.BooksDocument.Book;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineOutputTest extends AbstractJsonPipelineTest {

  public JsonPipelineOutputTest() {
    super();
  }

  @Test
  public void plainPipelineOutput() throws JSONException {

    // check that a plain pipeline will return the JSON emitted by the transport layer
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString());

    JsonNode output = pipeline.getJsonOutput().toBlocking().single();
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
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));
    verifyNoMoreInteractions(caching);
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

}
