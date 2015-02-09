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

import static io.wcm.caravan.pipeline.impl.AbstractJsonPipelineTest.getBooksString;
import static org.junit.Assert.assertEquals;
import io.wcm.caravan.commons.jsonpath.impl.JsonPathDefaultConfig;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.PathNotFoundException;

/** Tests for the {@link JsonPathSelector} functions */
@RunWith(MockitoJUnitRunner.class)
public class JsonPathSelectorTest {

  private JsonNode booksJson;

  @BeforeClass
  public static void initJsonPath() {
    Configuration.setDefaults(JsonPathDefaultConfig.INSTANCE);
  }

  @Before
  public void loadTestData() {
    booksJson = JacksonFunctions.stringToNode(getBooksString());
  }

  @Test
  public void testExtractArraySingleObject() {

    ArrayNode result = new JsonPathSelector("$.store.bicycle").call(booksJson);

    assertEquals(1, result.size());
    assertEquals("red", result.get(0).get("color").asText());
  }

  @Test
  public void testExtractArraySingleString() {

    ArrayNode result = new JsonPathSelector("$.store.bicycle.color").call(booksJson);

    assertEquals(1, result.size());
    assertEquals("red", result.get(0).asText());
  }

  @Test
  public void testExtractArraySingleArray() {

    // note that this will not give you the array of books directly
    // to achieve that, use either $.store.book[*] or use the NodeSelector
    ArrayNode result = new JsonPathSelector("$.store.book").call(booksJson);

    assertEquals(1, result.size());

    ArrayNode books = (ArrayNode)result.get(0);
    assertEquals(4, books.size());

    assertEquals("Nigel Rees", books.get(0).get("author").asText());
    assertEquals("Evelyn Waugh", books.get(1).get("author").asText());
  }

  @Test
  public void testExtractArrayMultipleObjects() {

    ArrayNode result = new JsonPathSelector("$.store.book[*]").call(booksJson);

    assertEquals(4, result.size());
    assertEquals("Nigel Rees", result.get(0).get("author").asText());
    assertEquals("Evelyn Waugh", result.get(1).get("author").asText());
  }


  @Test
  public void testExtractArrayMultipleStrings() {

    ArrayNode result = new JsonPathSelector("$.store.book[*].author").call(booksJson);

    assertEquals(4, result.size());
    assertEquals("Nigel Rees", result.get(0).asText());
    assertEquals("Evelyn Waugh", result.get(1).asText());
  }

  @Test
  public void testExtractArrayPathNoResults() {

    // if no selection matches the expression, but the property in the query exists, no results are returned
    ArrayNode result = new JsonPathSelector("$..book[?(@.title=='No such title')]").call(booksJson);

    assertEquals(0, result.size());
  }

  @Test(expected = PathNotFoundException.class)
  public void testExtractArrayPathNotFound() {

    // if the query includes a property that does not exist at all in the data, a PathNotFoundException is thrown
    ArrayNode result = new JsonPathSelector("$.store.cars[*]").call(booksJson);

    assertEquals(0, result.size());
  }
}
