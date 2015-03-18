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
package io.wcm.caravan.pipeline.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineActions;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rx.Observable;
import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineActionsTest {

  @Mock
  private JsonPipelineFactory factory;

  @Mock
  private JsonPipelineOutput jsonPipelineOutputPrevious;

  @Mock
  private JsonPipelineOutput jsonPipelineOutputResult;

  @Mock
  private JsonNode jsonNodeInput;

  @Mock
  private JsonNode jsonNodeTransformation;

  @Mock
  private Func1<JsonNode, JsonNode> mockFunction;

  @Test
  public void testSimpleTransformation() {

    Func1<JsonNode, JsonNode> function = mockFunction;
    when(jsonPipelineOutputPrevious.getPayload()).thenReturn(jsonNodeInput);
    when(function.call(jsonNodeInput)).thenReturn(jsonNodeTransformation);
    when(jsonPipelineOutputPrevious.withPayload(jsonNodeTransformation)).thenReturn(jsonPipelineOutputResult);

    JsonPipelineAction action = JsonPipelineActions.simpleTransformation("transformationId", function);
    assertNotNull(action.getId());
    assertEquals("transformationId", action.getId());

    Observable<JsonPipelineOutput> result = action.execute(jsonPipelineOutputPrevious, factory);
    assertNotNull(result);
    JsonPipelineOutput resultOutput = result.toBlocking().single();
    assertEquals(jsonPipelineOutputResult, resultOutput);
  }
}
