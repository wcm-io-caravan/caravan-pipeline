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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.jayway.jsonpath.InvalidPathException;

import io.wcm.caravan.pipeline.JsonPipeline;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineCollectTest extends AbstractJsonPipelineTest {

  public JsonPipelineCollectTest() {
    super();
  }

  @Test(expected = IllegalArgumentException.class)
  public void collectNullTargetPropertyException() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    pipeline.collect("$..label", null);
    fail();

  }

  @Test(expected = IllegalArgumentException.class)
  public void collectEmptyTargetPropertyException() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    pipeline.collect("$..label", "");

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

    // test extraction of a multiple *String* properties as ArrayNode without specified targetProperty
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }, b: { label: 'def' }}");
    JsonPipeline collected = pipeline.collect("$..label");

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
  public void collectArraysNoTargetProperty() throws JSONException {

    // test extraction of a multiple *Array* properties as ArrayNode without specified targetProperty
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[[1,2,3,4], [5,6,7,8]]", output, JSONCompareMode.STRICT_ORDER);

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
  public void collectAllArrayEntriesNoTargetProperty() throws JSONException {

    // test extraction of all items from multiple arrays as ArrayNode without specified targetProperty
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers[*]");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[1,2,3,4,5,6,7,8]", output, JSONCompareMode.STRICT_ORDER);

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

    // test extraction of multiple items with an *Array* as ArrayNode without specified targetProperty
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { numbers: [1,2,3,4] }, b: { numbers: [5,6,7,8] }}");
    JsonPipeline collected = pipeline.collect("$..numbers[3]");

    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[4,8]", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), collected.getDescriptor());
  }

  @Test
  public void collectNoResult() throws JSONException {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$.a[?(@.label=='def')]", "extracted");

    // make sure that only empty ArrayNode is returned and saved in ObjectNode with specified targetProperty when no result is found
    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [] }", output, JSONCompareMode.STRICT);
  }

  @Test
  public void collectNoResultNoTargetProperty() throws JSONException {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$.a[?(@.label=='def')]");

    // make sure that only empty ArrayNode is returned when no result is found
    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[]", output, JSONCompareMode.STRICT);
  }

  @Test
  public void collectJsonPathNotFound() throws JSONException {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$.a.numbers", "extracted");

    // make sure that only empty ArrayNode is returned and saved in ObjectNode with specified targetProperty when a PathNotFoundException is thrown
    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: [] }", output, JSONCompareMode.STRICT);
  }

  @Test
  public void collectJsonPathNotFoundNoTargetProperty() throws JSONException {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$.a.numbers");

    // make sure that only empty ArrayNode is returned when a PathNotFoundException is thrown
    String output = collected.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[]", output, JSONCompareMode.STRICT);
  }

  @Test(expected = InvalidPathException.class)
  public void collectInvalidJsonPath() {

    // test error handling for an JSON path that can not even be parsed
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline collected = pipeline.collect("$[");

    // this will trigger an InvalidPathException that should not be catched by the JsonPathSelector or CollectOperator
    collected.getStringOutput().toBlocking().single();
  }

  @Test
  public void collectTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex).collect("$..", "targetproperty");

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));

    verifyNoMoreInteractions(cacheAdapter);
  }

  @Test
  public void collectTransportErrorNoTargetProperty() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex).collect("$..");

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));

    verifyNoMoreInteractions(cacheAdapter);
  }

}
