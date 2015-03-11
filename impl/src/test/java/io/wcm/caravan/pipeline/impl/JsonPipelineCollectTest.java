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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import io.wcm.caravan.pipeline.JsonPipeline;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.jayway.jsonpath.PathNotFoundException;

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

    // test extraction of a multiple *String* properties
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

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));

    verifyNoMoreInteractions(caching);
  }

}
