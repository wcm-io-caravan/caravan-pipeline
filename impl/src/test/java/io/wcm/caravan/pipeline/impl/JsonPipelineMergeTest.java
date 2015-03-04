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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import io.wcm.caravan.pipeline.JsonPipeline;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineMergeTest extends AbstractJsonPipelineTest {

  public JsonPipelineMergeTest() {
    super();
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

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache

    merged.getStringOutput().subscribe(new ExceptionExpectingObserver(ex));
    verifyNoMoreInteractions(caching);
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
    verifyNoMoreInteractions(caching);
  }

}
