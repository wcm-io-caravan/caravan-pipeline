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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineExtractTest extends AbstractJsonPipelineTest {

  public JsonPipelineExtractTest() {
    super();
  }

  @Test(expected = IllegalArgumentException.class)
  public void extractNullTargetPropertyException() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    pipeline.extract("$.a", null);

  }

  @Test(expected = IllegalArgumentException.class)
  public void extractEmptyTargetPropertyException() {

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    pipeline.extract("$.a", "");
    fail();

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
    JsonPipeline extracted = pipeline.extract("$.a");

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
    JsonPipeline extracted = pipeline.extract("$.a.numbers");

    String output = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("[1,2,3,4]", output, JSONCompareMode.STRICT);

    assertNotEquals("descriptor has been updated?", pipeline.getDescriptor(), extracted.getDescriptor());
  }

  @Test
  public void extractNoResult() throws JSONException {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a[?(@.label=='def')]", "extracted");

    JsonNode output = extracted.getJsonOutput().toBlocking().single();
    assertTrue(output.get("extracted").isMissingNode());
    String stringOutput = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: null }", stringOutput, JSONCompareMode.STRICT);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void extractNoResultNoTargetProperty() {

    // test handling of a valid JSONPath for the given structure that has no results
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$.a[?(@.label=='def')]");

    // make sure that only MissingNode is returned when a no result found
    JsonNode output = extracted.getJsonOutput().toBlocking().single();
    assertTrue(output.isMissingNode());
    // make sure that JsonPipelineOutputException is thrown, when user tries to serialize MissingNode as String
    extracted.getStringOutput().toBlocking().single();
  }

  @Test
  public void extractJsonPathNotFound() throws JSONException {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$a.numbers", "extracted");

    // make sure that only MissingNode is returned when a PathNotFoundException is thrown
    JsonNode output = extracted.getJsonOutput().toBlocking().single();
    assertTrue(output.get("extracted").isMissingNode());
    // make sure a JsonObject is serialized and deserialized with the null value parameter
    String stringOutput = extracted.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{ extracted: null }", stringOutput, JSONCompareMode.STRICT);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void extractJsonPathNotFoundNoTargetProperty() {

    // test error handling if a property has been used in the JSONPath that does not exist in the whole document
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: { label: 'abc' }}");
    JsonPipeline extracted = pipeline.extract("$a.numbers");


    // make sure that only MissingNode is returned when a PathNotFoundException is thrown
    JsonNode output = extracted.getJsonOutput().toBlocking().single();
    assertTrue(output.isMissingNode());

    // make sure that JsonPipelineOutputException is thrown, when user tries to serialize MissingNode as String
    extracted.getStringOutput().toBlocking().single();
  }

  @Test
  public void extractTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex).extract("$..", "targetproperty");

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));

    verifyNoMoreInteractions(cacheAdapter);
  }

  @Test
  public void extractResourceNotFound() {

    // tests that 404 responses from the transport layers are properly handled
    JsonPipeline pipeline = newPipelineWithResponseCode(404).extract("$..", "targetproperty");
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

}
