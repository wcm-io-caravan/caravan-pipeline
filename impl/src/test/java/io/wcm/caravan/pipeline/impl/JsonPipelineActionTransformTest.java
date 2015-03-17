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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineActions;
import io.wcm.caravan.pipeline.JsonPipelineInputException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineActionTransformTest extends AbstractJsonPipelineTest {

  public JsonPipelineActionTransformTest() {
    super();
  }

  @Test
  public void transformationSuccess() throws JSONException {
    JsonPipeline fetchId = newPipelineWithResponseBody("{id: 123}");

    JsonPipeline fetchIdThenName =
        fetchId.applyAction(JsonPipelineActions.simpleTransformation("fetchName", (fetchIdOutput) -> {
          int id = fetchIdOutput.get("id").asInt();
          return getJsonNode("{\"id\": " + id + ", \"name\": \"abc\"}");
        }));

    String output = fetchIdThenName.getStringOutput().toBlocking().first();
    JSONAssert.assertEquals("{id: 123, name: 'abc'}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void transformationPreviousPipelineFailed() {
    JsonPipeline fetchId = newPipelineWithResponseError(new RuntimeException());

    JsonPipeline fetchIdThenName =
        fetchId.applyAction(JsonPipelineActions.simpleTransformation("fetchName", (fetchIdOutput) -> {
          int id = fetchIdOutput.get("id").asInt();
          return getJsonNode("{\"id\": " + id + ", \"name\": \"abc\"}");
        }));

    fetchIdThenName.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void transformationActualPipelineFailed() {
    JsonPipeline fetchId = newPipelineWithResponseBody("{id: 123}");

    JsonPipeline fetchIdThenName =
        fetchId.applyAction(JsonPipelineActions.simpleTransformation("fetchName", (fetchIdOutput) -> {
          throw new RuntimeException("An expected exception has occured");
        }));

    fetchIdThenName.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

}
