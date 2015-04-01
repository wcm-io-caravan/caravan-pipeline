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
package io.wcm.caravan.pipeline.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineActions;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineOutputUtilTest {

  @Mock
  private JsonPipelineOutput jsonPipelineOutputPrevious;

  @Mock
  private JsonPipelineOutput jsonPipelineOutputResult;

  @Mock
  private JsonPipelineOutput jsonPipelineOutputAnother;

  @Test
  public void testEnrichWithLowestMaxAge_takePrevious() {

    when(jsonPipelineOutputAnother.getMaxAge()).thenReturn(2000);
    when(jsonPipelineOutputPrevious.getMaxAge()).thenReturn(1000);

    // check the original JSON pipeline output was returned because it has the lowest max age value
    JsonPipelineOutput result = JsonPipelineOutputUtil.enrichWithLowestMaxAge(jsonPipelineOutputPrevious, jsonPipelineOutputAnother);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputPrevious, result);
  }

  @Test
  public void testEnrichWithLowestMaxAge_takeAnother() {
    when(jsonPipelineOutputAnother.getMaxAge()).thenReturn(1000);
    when(jsonPipelineOutputPrevious.getMaxAge()).thenReturn(2000);
    when(jsonPipelineOutputPrevious.withMaxAge(1000)).thenReturn(jsonPipelineOutputResult);

    // check a new JSON pipeline output was returned because another max age value had to be added to the original pipeline
    JsonPipelineOutput result = JsonPipelineOutputUtil.enrichWithLowestMaxAge(jsonPipelineOutputPrevious, jsonPipelineOutputAnother);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputResult, result);
  }

  @Test
  public void testEnrichWithLowestMaxAge_compareToNull() {
    JsonPipelineAction action = JsonPipelineActions.enrichWithLowestMaxAge(null);
    assertNotNull(action);
    assertNotNull(action.getId());

    // check the original JSON pipeline output was returned because it is the only one possible result
    JsonPipelineOutput result = JsonPipelineOutputUtil.enrichWithLowestMaxAge(jsonPipelineOutputPrevious, null);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputPrevious, result);
  }

  @Test
  public void testEnrichWithLowestMaxAge_compareNull() {
    JsonPipelineAction action = JsonPipelineActions.enrichWithLowestMaxAge(null);
    assertNotNull(action);
    assertNotNull(action.getId());

    // check the null is returned because no actual output is defined to be enriched
    JsonPipelineOutput result = JsonPipelineOutputUtil.enrichWithLowestMaxAge(null, jsonPipelineOutputAnother);
    assertNull(result);
  }

  @Test
  public void testMinMaxAge_takePrevious() {

    when(jsonPipelineOutputAnother.getMaxAge()).thenReturn(2000);
    when(jsonPipelineOutputPrevious.getMaxAge()).thenReturn(1000);

    // check the original JSON pipeline output was returned because it has the lowest max age value
    JsonPipelineOutput result = JsonPipelineOutputUtil.minMaxAge(jsonPipelineOutputPrevious, jsonPipelineOutputAnother);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputPrevious, result);
  }

  @Test
  public void testMinMaxAge_takeAnother() {
    when(jsonPipelineOutputAnother.getMaxAge()).thenReturn(1000);
    when(jsonPipelineOutputPrevious.getMaxAge()).thenReturn(2000);

    // check the another JSON pipeline output was returned because it has the lowest max age value
    JsonPipelineOutput result = JsonPipelineOutputUtil.minMaxAge(jsonPipelineOutputPrevious, jsonPipelineOutputAnother);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputAnother, result);
  }

  @Test
  public void testMinMaxAge_compareToNull() {
    JsonPipelineAction action = JsonPipelineActions.enrichWithLowestMaxAge(null);
    assertNotNull(action);
    assertNotNull(action.getId());

    // check the original JSON pipeline output was returned because it is the only one possible result
    JsonPipelineOutput result = JsonPipelineOutputUtil.minMaxAge(jsonPipelineOutputPrevious, null);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputPrevious, result);
  }

  @Test
  public void testMinMaxAge_compareNull() {

    when(jsonPipelineOutputPrevious.getMaxAge()).thenReturn(2000);

    JsonPipelineAction action = JsonPipelineActions.enrichWithLowestMaxAge(null);
    assertNotNull(action);
    assertNotNull(action.getId());

    // check the another JSON pipeline output was returned because it is the only one possible result
    JsonPipelineOutput result = JsonPipelineOutputUtil.minMaxAge(null, jsonPipelineOutputAnother);
    assertNotNull(result);
    assertEquals(jsonPipelineOutputAnother, result);
  }

}
