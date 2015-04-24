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
import static org.mockito.Matchers.isA;
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

import com.fasterxml.jackson.databind.JsonNode;
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
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void assertExistsAfterExtractWithNoResult() {

    // check that assertExist also fails with an exception if the pipeline's result is a missing node because of a preceding extract

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .extract("$.b") // this will *not* match anything in the response, so the pipeline output will be a missing node
        .assertExists("$..*", 404, "no node was extracted"); // the JsonPath expression means "All members of JSON structure"

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void assertExistsAfterExtractWithNoResultIntoProperty() {

    // check that assertExist also fails with an exception if the pipeline's result is a missing node because of a preceding extract

    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .extract("$.b", "c") // this will *not* match anything in the response, so the pipeline will contain an object with a missing node in the c property
        .assertExists("$.[?(@.c)]", 404, "a not found"); // the JSONPath expression means: the root node which has a non-null property named "c"

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }


  @Test(expected = InvalidPathException.class)
  public void assertExistsFailsWithInvalidPath() {

    // check that assertExist fails with an InvalidPathException if the given JSONPath is not valid
    JsonPipeline pipeline = newPipelineWithResponseBody("{a: 123}")
        .assertExists("$.a[invalid", 404, "not found");

    pipeline.getStringOutput().subscribe(stringObserver);

    // make sure that only #onError was called, and there wasn't any other interaction with the observer or cache
    verify(stringObserver).onError(isA(InvalidPathException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  // below are some additional tests/examples how you can use assertExists after extract/collect
  // to verify that the result of the extract/collect operation matches your expectations before you go on
  // with additional pipeline steps

  @Test
  public void assertExistsAfterExtractIntoTargetProperty() {

    // check that a fulfilled assertion completes for preceding extract operation
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString())
        .extract("$..book[0]", "firstBook")
        .assertExists("$.firstBook.author", 500, "first book has no author (or wasn't found)");

    pipeline.getStringOutput().toBlocking().single();
  }

  @Test
  public void assertExistsAfterExtractNoResultIntoTargetProperty() {

    // check that a fulfilled assertion fails for preceding extract operation because of extracted null
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString())
        .extract("$..movie[0]", "firstMovie")
        .assertExists("$.firstMovie.title", 500, "first movie not found (or it doesn't have a title)");

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }

  @Test
  public void assertExistsAfterCollectIntoTargetProperty() {

    // check that a fulfilled assertion completes for preceding collect operation
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString())
        .collect("$..book[?(@.price<10)]", "cheapBooks")
        .assertExists("$.cheapBooks[*]", 500, "no books found with price < 10");

    JsonNode output = pipeline.getJsonOutput().toBlocking().single();
    assertEquals(2, output.get("cheapBooks").size());
  }

  @Test
  public void assertExistsAfterEmptyCollectIntoTargetProperty() {

    // check that a fulfilled assertion fails for preceding extract operation because an empty array is extracted
    JsonPipeline pipeline = newPipelineWithResponseBody(getBooksString())
        .collect("$..movie[?(@.price<10)]", "cheapMovies")
        .assertExists("$.cheapMovies[*]", 500, "no movies found with price < 10");

    pipeline.getStringOutput().subscribe(stringObserver);

    verify(stringObserver).onError(isA(JsonPipelineInputException.class));
    verifyNoMoreInteractions(stringObserver, cacheAdapter);
  }


}
