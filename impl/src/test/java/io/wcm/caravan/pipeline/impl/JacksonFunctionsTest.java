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
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import org.junit.Test;

import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;


public class JacksonFunctionsTest {

  @Test
  public void testPojoToString() {
    String result = JacksonFunctions.pojoToString(createSimplePojo(123, "abc"));

    // match generated String from POJO with expected JSON as String
    assertEquals("{\"id\":123,\"name\":\"abc\"}", result);
  }

  @Test(expected = JsonPipelineOutputException.class)
  public void testObjectToString() {

    // fail with JsonPipelineInputException by providing incorrect POJO class
    JacksonFunctions.pojoToString(new Object());
  }

  @Test
  public void testPojoToNode() {
    JsonNode result = JacksonFunctions.pojoToNode(createSimplePojo(123, "abc"));

    // match generated JSON from POJO with expected JSON as String
    assertEquals("{\"id\":123,\"name\":\"abc\"}", result.toString());
  }

  @Test(expected = JsonPipelineInputException.class)
  public void testObjectToNode() {

    // fail with JsonPipelineInputException by providing incorrect POJO class
    JacksonFunctions.pojoToNode(new Object());
  }

  @Test
  public void testStringToObjectNode() {
    JsonNode result = JacksonFunctions.stringToObjectNode("{\"id\":123,\"name\":\"abc\"}");

    // match generated JSON from String with expected JSON as String
    assertEquals("{\"id\":123,\"name\":\"abc\"}", result.toString());
  }

  @Test(expected = JsonPipelineInputException.class)
  public void testStringToObjectNodeJsonSyntaxError() {

    // fail with JsonPipelineInputException by providing incorrect JSON syntax
    JacksonFunctions.stringToObjectNode("{id:123,name:abc}");
  }

  @Test
  public void testNodeToString() {
    JsonNode node = JacksonFunctions.stringToObjectNode("{\"id\":123,\"name\":\"abc\"}");
    String actual = JacksonFunctions.nodeToString(node);

    // match generated JSON string with expected
    assertEquals("{\"id\":123,\"name\":\"abc\"}", actual);
  }

  @Test
  public void testStringToPojo() {
    Func1<String, SimplePojo> coverter = JacksonFunctions.stringToPojo(SimplePojo.class);
    SimplePojo pojo = coverter.call("{\"id\":123,\"name\":\"abc\"}");

    // match provided JSON fields name/value with POJO fields name/value
    assertEquals(Integer.valueOf(123), pojo.getId());
    assertEquals("abc", pojo.getName());
  }

  @Test(expected = JsonPipelineInputException.class)
  public void testStringToPojoFail() {
    Func1<String, SimplePojo> coverter = JacksonFunctions.stringToPojo(SimplePojo.class);

    // fail with JsonPipelineInputException by providing JSON input with incorrect field names
    coverter.call("{\"idE\":123,\"nameE\":\"abc\"}");
  }

  @Test
  public void testNodeToPojo() {
    Func1<JsonNode, SimplePojo> coverter = JacksonFunctions.nodeToPojo(SimplePojo.class);
    JsonNode node = JacksonFunctions.stringToObjectNode("{\"id\":123,\"name\":\"abc\"}");
    SimplePojo pojo = coverter.call(node);

    // match provided POJO fields name/value with expected values
    assertEquals(Integer.valueOf(123), pojo.getId());
    assertEquals("abc", pojo.getName());
  }

  private Object createSimplePojo(Integer id, String name) {
    SimplePojo pojo = new SimplePojo();
    pojo.setId(id);
    pojo.setName(name);
    return pojo;
  }

  public static class SimplePojo {

    private Integer id;
    private String name;

    public Integer getId() {
      return this.id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }


  }

}
