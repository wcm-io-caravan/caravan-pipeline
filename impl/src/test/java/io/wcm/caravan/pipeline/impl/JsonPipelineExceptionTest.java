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

import static io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers.fallbackFor404;
import static io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers.fallbackFor50x;
import static io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers.rethrow404;
import static io.wcm.caravan.pipeline.JsonPipelineExceptionHandlers.rethrow50x;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineExceptionTest extends AbstractJsonPipelineTest {

  public JsonPipelineExceptionTest() {
    super();
  }

  @Test
  public void handleExceptionSuccess() throws JSONException {

    String responseJson = "{a: 123}";

    JsonPipeline pipeline = newPipelineWithResponseBody(responseJson)
        .handleException((fallbackContent, ex) -> {
          fail("this should not be called");
          return Observable.just(fallbackContent);
        });

    String output = pipeline.getStringOutput().toBlocking().first();

    JSONAssert.assertEquals(responseJson, output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleException404Rethrow() {

    RuntimeException rethrown = new RuntimeException();

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .handleException(rethrow404(rethrown));

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void handleException404Fallback() throws JSONException {

    String fallbackJson = "{fallback: true}";

    int fallbackTtl = 15;

    JsonPipeline pipeline = newPipelineWithResponseCode(404)
        .handleException(fallbackFor404(JacksonFunctions.stringToNode(fallbackJson), fallbackTtl));

    JsonPipelineOutput output = pipeline.getOutput().toBlocking().first();

    assertEquals(200, output.getStatusCode());
    assertEquals(fallbackTtl, output.getMaxAge());

    String jsonOutput = JacksonFunctions.nodeToString(output.getPayload());
    JSONAssert.assertEquals(fallbackJson, jsonOutput, JSONCompareMode.STRICT);
  }

  @Test
  public void handleException500Rethrow() {

    RuntimeException rethrown = new RuntimeException("Rethrown");

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        .handleException(rethrow50x(rethrown));

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void handleException500Fallback() throws JSONException {

    String fallbackJson = "{fallback: true}";

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        .handleException(fallbackFor50x(JacksonFunctions.stringToNode(fallbackJson), 0));

    String output = pipeline.getStringOutput().toBlocking().first();

    JSONAssert.assertEquals(fallbackJson, output, JSONCompareMode.STRICT);
  }

  @Test
  public void handleExceptionChaining500() {

    RuntimeException rethrown = new RuntimeException("Rethrown");

    JsonPipeline pipeline = newPipelineWithResponseError(new RuntimeException("Original"))
        // first register an exception handler that provides fallback content for a 404, but rethrows any other exceptions
        .handleException(fallbackFor404(JacksonFunctions.stringToNode("{}"), 0))
        // then add the other handler that should be actually triggered here
        .handleException((fallbackContent, ex) -> {
          assertEquals(500, fallbackContent.getStatusCode());
          throw rethrown;
        });

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(rethrown);
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

}
