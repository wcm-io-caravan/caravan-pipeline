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
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineMergeTest extends AbstractJsonPipelineTest {

  public JsonPipelineMergeTest() {
    super();
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeNullTargetPropertyException() {

    // test throw of IllegalArgumentException by null target property argument
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    a.merge(b, null);
  }

  @Test
  public void mergedPipelineSuccess() throws JSONException {

    // test successful merge of one pipeline into the other
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b, "c");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, c: {b: 456}}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergePipelineNoTargetProperty() throws JSONException {

    // test successful merge of one pipeline into the other *without adding another property*
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b);

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, b: 456}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergePipelineExtractedNodeIntoTargetProperty() throws JSONException {

    // test successful merge of one extracted results of pipeline into another pipeline node with specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.extract("b");

    JsonPipeline merged = a.merge(c, "next node");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{'a': 123, 'next node': 456}", output, JSONCompareMode.STRICT);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void mergePipelineExtractedNodeNoTargetProperty() {

    // test throw of JsonPipelineOutputException by merge of extracted node value into other pipeline node without specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.extract("b");

    JsonPipeline merged = a.merge(c);
    merged.getStringOutput().toBlocking().single();
  }

  @Test
  public void mergePipelineMissingNodeIntoTargetProperty() throws JSONException {

    // test successful merge of an extracted missing node into another pipeline node with specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.extract("c");

    JsonPipeline merged = a.merge(c, "next node");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{'a': 123, 'next node': null}", output, JSONCompareMode.STRICT);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void mergePipelineMissingNodeNoTargetProperty() {

    // test throw of JsonPipelineOutputException by merge of extracted missing node into other pipeline node without specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.extract("c");

    JsonPipeline merged = a.merge(c);
    merged.getStringOutput().toBlocking().single();
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void mergePipelineEmptyArrayNodeNoTargetProperty() {

    // test throw of JsonPipelineOutputException by merge of a collected empty array node into other pipeline node without specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.collect("c");

    JsonPipeline merged = a.merge(c);
    merged.getStringOutput().toBlocking().single();
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void mergePipelineArrayNodeNoTargetProperty() {

    // test throw of JsonPipelineOutputException by merge of a collected array node into other pipeline node without specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.collect("b");

    JsonPipeline merged = a.merge(c);
    merged.getStringOutput().toBlocking().single();
  }

  @Test
  public void mergePipelineEmptyArrayNodeIntoTargetProperty() throws JSONException {

    // test successful merge of a collected empty array node into another pipeline node with specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.collect("c");

    JsonPipeline merged = a.merge(c, "arrayNode");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, arrayNode: []}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergePipelineCollectedArrayNodeIntoTargetProperty() throws JSONException {

    // test successful merge of a collected array node into another pipeline node with specifying of target property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");
    JsonPipeline c = b.collect("b");

    JsonPipeline merged = a.merge(c, "arrayNode");

    assertNotEquals("descriptor has been updated?", a.getDescriptor(), merged.getDescriptor());
    assertNotEquals("desriptor not just taken from the other pipeline?", b.getDescriptor(), merged.getDescriptor());

    String output = merged.getStringOutput().toBlocking().single();

    JSONAssert.assertEquals("{a: 123, arrayNode: [456]}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void mergePipelineNotAnObject() {

    // test failed merge into already existing property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}").collect("a");
    JsonPipeline b = a.merge(newPipelineWithResponseBody("{b: 456}"));
    JsonPipeline c = b.merge(a, "c");

    JsonPipelineOutputException exception = new JsonPipelineOutputException("Target pipeline " + b.getDescriptor() + " already has a property named " + "a");

    c.getStringOutput().subscribe(new ExceptionExpectingObserver(exception));
    verifyNoMoreInteractions(cacheAdapter);
  }

  @Test
  public void mergePipelineIntoNoObjectNode() {
    TestJsonPipelineExceptionHandler handler = new TestJsonPipelineExceptionHandler();
    // test failed merge into already existing property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}").extract("b").merge(a).handleException(handler);

    // Check that JSON pipeline could not be merged into another JSON pipeline because another JSON pipeline does not contain ObjectNode
    JsonPipelineOutputException exception = new JsonPipelineOutputException(
        "Only pipelines with JSON *Objects* can be used as a target for a merge operation, but response data for GET(//testService/path)+EXTRACT(b) contained IntNode");
    b.getStringOutput().subscribe(new ExceptionExpectingObserver(exception));
    RuntimeException actualException = handler.getActualException();
    verifyNoMoreInteractions(cacheAdapter);
    assertEquals(exception.toString(), actualException.toString());
  }

  @Test
  public void mergeNoObjectNodePipelineIntoTargetNode() {
    TestJsonPipelineExceptionHandler handler = new TestJsonPipelineExceptionHandler();
    // test failed merge into already existing property
    JsonPipeline a = newPipelineWithResponseBody("{a : 123}").extract("a");
    JsonPipeline b = newPipelineWithResponseBody("{b: {b : 123}}").merge(a, "b").handleException(handler);

    // Check that JSON pipeline could not be merged into another JSON pipeline target node because it does not contain an Object node
    JsonPipelineOutputException exception = new JsonPipelineOutputException(
        "Only pipelines with JSON *Object* responses can be merged into an existing target property");
    b.getStringOutput().subscribe(new ExceptionExpectingObserver(exception));
    RuntimeException actualException = handler.getActualException();
    verifyNoMoreInteractions(cacheAdapter);
    assertEquals(exception.toString(), actualException.toString());
  }

  @Test
  public void mergePipelineIntoAlreadyExistingProperty() {
    TestJsonPipelineExceptionHandler handler = new TestJsonPipelineExceptionHandler();
    // test failed merge into already existing property
    JsonPipeline a = newPipelineWithResponseBody("{a : 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: {a : 456}}").merge(a, "b").handleException(handler);

    // Check that JSON pipeline could not be merged into another JSON pipeline target node because it does not contain an Object node
    JsonPipelineOutputException exception = new JsonPipelineOutputException(
        "Target pipeline GET(//testService/path) already has a property named a");
    b.getStringOutput().subscribe(new ExceptionExpectingObserver(exception));
    RuntimeException actualException = handler.getActualException();
    verifyNoMoreInteractions(cacheAdapter);
    assertEquals(exception.toString(), actualException.toString());
  }

  @Test
  public void mergePipelineIntoNonObjectChildNode() {
    TestJsonPipelineExceptionHandler handler = new TestJsonPipelineExceptionHandler();
    // test failed merge into already existing property
    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}").merge(a, "b").handleException(handler);

    // Check that Json pipeline could not be merged into another Json pipeline because it try no merge ObjectNode into a primitive child node
    JsonPipelineOutputException exception = new JsonPipelineOutputException(
        "When merging two pipelines into the same target property, both most contain JSON *Object* responses");
    b.getStringOutput().subscribe(new ExceptionExpectingObserver(exception));
    RuntimeException actualException = handler.getActualException();
    verifyNoMoreInteractions(cacheAdapter);
    assertEquals(exception.toString(), actualException.toString());
  }

  @Test
  public void mergedPipelineTransportError1() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline a = newPipelineWithResponseBody("{a: 123}");
    JsonPipeline b = newPipelineWithResponseError(ex);

    JsonPipeline merged = a.merge(b, "c");

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache

    merged.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));
    verifyNoMoreInteractions(cacheAdapter);
  }

  @Test
  public void mergedPipelineTransportError2() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline a = newPipelineWithResponseError(ex);
    JsonPipeline b = newPipelineWithResponseBody("{b: 456}");

    JsonPipeline merged = a.merge(b, "c");

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    merged.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));
    verifyNoMoreInteractions(cacheAdapter);
  }

  private static class TestJsonPipelineExceptionHandler implements JsonPipelineExceptionHandler {

    private RuntimeException actualException;

    @Override
    public Observable<JsonPipelineOutput> call(JsonPipelineOutput defaultFallbackContent, RuntimeException caughtException) {
      actualException = caughtException;
      throw caughtException;
    }

    public RuntimeException getActualException() {
      return actualException;
    }
  };

}
