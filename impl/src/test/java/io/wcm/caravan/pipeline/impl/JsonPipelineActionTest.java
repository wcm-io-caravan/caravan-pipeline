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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineActionTest extends AbstractJsonPipelineTest {

  @Mock
  protected JsonPipelineAction action;

  public JsonPipelineActionTest() {
    super();
  }

  @Test
  public void applyActionSuccess() throws JSONException {
    JsonPipeline previousStep = newPipelineWithResponseBody("{id:123}");
    JsonPipeline nextStep = newPipelineWithResponseBody("{name:'abc'}");
    when(action.execute(any(), any())).thenReturn(nextStep.getOutput());

    JsonPipeline result = previousStep.applyAction(action);
    assertNotNull(result);
    JSONAssert.assertEquals("{name:'abc'}", result.getStringOutput().toBlocking().single(), JSONCompareMode.STRICT);
  }

  @Test
  public void applyActionAnonymousClass() throws JSONException {
    JsonPipeline previousStep = newPipelineWithResponseBody("{id:123}");

    JsonPipeline result = previousStep.applyAction(new JsonPipelineAction() {

      @Override
      public String getId() {
        return "identifier";
      }

      @Override
      public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext context) {
        ObjectNode nextObject = previousStepOutput.getPayload().deepCopy();
        nextObject.put("name", "abc");
        JsonPipelineOutput transformedOutput = previousStepOutput.withPayload(nextObject);
        return Observable.just(transformedOutput);
      }
    });

    assertNotNull(result);
    JSONAssert.assertEquals("{id:123, name:'abc'}", result.getStringOutput().toBlocking().single(), JSONCompareMode.STRICT);
  }

  @Test
  public void applyActionPreviousPipelineFailed() {
    IOException failure = new IOException();
    JsonPipeline previousStep = newPipelineWithResponseError(failure);

    JsonPipeline result = previousStep.applyAction(action);
    result.getStringOutput().subscribe(stringObserver);

    // Exceptions when fetching the HTTP response will always be wrapped in a JsonPipelineInputException
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void applyActionActualPipelineFailed() {
    JsonPipeline previousStep = newPipelineWithResponseBody("{id: 123}");

    RuntimeException failure = new RuntimeException();
    when(action.execute(any(), any())).thenThrow(failure);

    JsonPipeline result = previousStep.applyAction(action);
    result.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(eq(failure));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }


}
