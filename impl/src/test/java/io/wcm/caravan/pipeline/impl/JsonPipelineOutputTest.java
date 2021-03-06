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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

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
  public void plainPipelineHasMaxAgeFromResponse() {

    // setup a JsonPipeline with a response that contains a Cache-Control/max-age of 10 seconds
    JsonPipeline pipeline = newPipelineWithResponseBodyAndMaxAge("{}", 10);

    // and assure the value is properly transferred to the JsonPipelineOutput metadata
    JsonPipelineOutput output = pipeline.getOutput().toBlocking().single();
    assertEquals(10, output.getMaxAge());
  }

  @Test
  public void plainPipelineTransportError() {

    // tests that errors from the transport layers are properly handled
    FileNotFoundException ex = new FileNotFoundException("Failed");

    JsonPipeline pipeline = newPipelineWithResponseError(ex);
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, with the FileNotFoundException thrown from the transport layer
    pipeline.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));
    verifyNoMoreInteractions(cacheAdapter);
  }

  @Test
  public void plainPipelineResourceNotFound() {

    // tests that 404 responses are not parsed as JSON, but treated as an error
    JsonPipeline pipeline = newPipelineWithResponseCode(404);
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void plainPipelineParseError() {

    // tests that invalid JSOn in the response is properly handled
    JsonPipeline pipeline = newPipelineWithResponseBody("<this> is not json</this>");
    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void plainPipelineOutputMissingNode() {

    // check that pipelines string output throws JsonPipelineOutputException when MissingNode is a root node of the pipeline
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString()).extract("$..videos");

    pipeline.getStringOutput().toBlocking().single();
  }

}
