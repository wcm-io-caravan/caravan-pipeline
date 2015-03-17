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
import io.wcm.caravan.pipeline.JsonPipelineInputException;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.jayway.jsonpath.InvalidPathException;

@RunWith(MockitoJUnitRunner.class)
public class JsonPipelineAssertExistsTest extends AbstractJsonPipelineTest {

  public JsonPipelineAssertExistsTest() {
    super();
  }

  @Test
  public void assertExistsSuccess() throws JSONException {

    // check that a fulfilled assertion will not influence the pipeline output
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.a", 500, "a not found");

    String output = pipeline.getStringOutput().toBlocking().single();
    JSONAssert.assertEquals("{a: 123}", output, JSONCompareMode.STRICT);
  }

  @Test
  public void assertExistsFails() {


    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.b", 404, "b not found");

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void assertExistsAfterExtract() {

    // check that assertExist also fails with an exception if the pipeline's result is null because of a preceding extract

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .extract("$[?(@.a==456)]") // this will *not* match the root object, so the pipeline's output is null
        .assertExists("$.a", 404, "a not found"); // this used to fail with an InvalidArgumentException within JsonPathSelector, that is now avoided

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void assertExistsWithInvalidPath() {

    // check that assertExist fails with an InvalidPathException if the given JSONPath is not valid
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.a[invalid]", 404, "not found");

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(any(InvalidPathException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

}
