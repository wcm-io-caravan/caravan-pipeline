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

import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import java.io.IOException;
import java.io.StringWriter;

import rx.functions.Func1;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains common conversion functions between Jackson JSON nodes, Strings and Objects.
 */
public final class JacksonFunctions {

  private static ObjectMapper objectMapper = new ObjectMapper()
  .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
  .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private static JsonFactory jsonFactory = new JsonFactory(objectMapper)
  .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES) // this is mostly useful to write JSON like { a: 123 } in unit tests
  .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES); // this was also just added for convenience.

  private static JsonNodeFactory nodeFactory = JsonNodeFactory.withExactBigDecimals(false);

  // TODO: all these features should be made configurable

  private JacksonFunctions() {
  }
  // the basic conversions that don't need a type can simply be defined as static methods
  // when they are used as functions for Observable#map, you can reference them RxJackson::stringToNode


  /**
   * Parse the given JSON string using the default factory
   * @param jsonString a string with a valid JSON document
   * @return either a {@link ObjectNode} or {@link ArrayNode}, depending on the input
   * @throws JsonPipelineInputException if the input is not valid JSON
   */
  public static JsonNode stringToNode(String jsonString) {
    JsonNode node = null;
    try {
      node = jsonFactory.createParser(jsonString).readValueAsTree();
    }
    catch (IOException ex) {
      throw new JsonPipelineInputException(500, "Failed to parse JSON: " + jsonString, ex);
    }
    return node;
  };

  /**
   * Parse the given JSON string using the default factory
   * @param jsonString a string with a valid JSON document
   * @return a {@link ObjectNode}
   * @throws JsonPipelineInputException if the input is not valid a JSON *Object*
   */
  public static ObjectNode stringToObjectNode(String jsonString) {
    ObjectNode node = null;
    try {
      node = jsonFactory.createParser(jsonString).readValueAsTree();
    }
    catch (IOException | ClassCastException ex) {
      throw new JsonPipelineInputException(500, "Failed to parse JSON object from: " + jsonString, ex);
    }
    return node;
  };

  /**
   * Serializes the given JSON node using the default factory
   * @param node
   * @return JSON string
   * @throws JsonPipelineOutputException if the given node can not be serialized
   */
  public static String nodeToString(JsonNode node) {
    if (node.isMissingNode()) {
      throw new JsonPipelineOutputException(
          "Received a MissingNode from the JSON pipeline output. Please do not serialize this result as a String and handle MissingNode in your client.");
    }
    try {
      StringWriter writer = new StringWriter();
      JsonGenerator generator = jsonFactory.createGenerator(writer);
      generator.writeObject(node);
      return writer.toString();
    }
    catch (IOException ex) {
      throw new JsonPipelineOutputException("Failed to serialize JsonNode. This was quite unexpected.", ex);
    }
  };

  /**
   * Creates a new JSON object with a single property
   * @param targetProperty the name of the single property
   * @param propertyValue the value of the
   * @return the new ObjectNode
   */
  public static ObjectNode wrapInObject(String targetProperty, JsonNode propertyValue) {
    ObjectNode objectNode = nodeFactory.objectNode();
    objectNode.set(targetProperty, propertyValue);
    return objectNode;
  }

  /**
   * @return an empty ObjectNode
   */
  public static ObjectNode emptyObject() {
    return nodeFactory.objectNode();
  }

  /**
   * Create a JSON tree with the same structure as the given map
   * @param object that can be properly mapped to JSON with Jackson (e.g. a POJO or Map)
   * @return an {@link ObjectNode}
   * @throws JsonPipelineInputException if the input is not valid JSON
   */
  public static JsonNode pojoToNode(Object object) {
    JsonNode node = null;
    try {
      node = objectMapper.valueToTree(object);
    }
    catch (IllegalArgumentException ex) {
      throw new JsonPipelineInputException(500, "Failed to create JSONNode from object of class " + object.getClass().getName(), ex);
    }
    return node;
  };


  /**
   * Serializes the given POJO into a JSON string using the default factory
   * @param pojo an object that can be serialized with Jackson's default {@link ObjectMapper}
   * @return JSON string
   * @throws JsonPipelineOutputException if the given object can not be serialized
   */
  public static String pojoToString (Object pojo) {
    try {
      StringWriter writer = new StringWriter();
      objectMapper.writeValue(writer, pojo);
      return writer.toString();
    }
    catch (IOException ex) {
      throw new JsonPipelineOutputException("Failed to serialize entity to JSON string", ex);
    }
  };

  // the functions that convert to a Java type depend on the target class, that's why we can't make them as plain
  // static functions. Instead they return a lambda that can be passed to Observable#map

  /**
   * Returns a function that will instantiate a bean of the given class, with values copied from the JSON string
   * @param targetType the POJO class that matches the expected JSON structure
   * @return a function that takes a JSON string and returns a new instance of the target type
   * @throws JsonPipelineInputException if the JSON could not be parsed, or the object not instantiated
   */
  public static <T> Func1<String, T> stringToPojo(Class<T> targetType) {
    return jsonString -> {
      try {
        return jsonFactory.createParser(jsonString).readValueAs(targetType);
      }
      catch (IOException ex) {
        throw new JsonPipelineInputException(500, "Failed to create entity of " + targetType.getName() + " from JSON", ex);
      }
    };
  }

  /**
   * Returns a function that will instantiate a bean of the given class, with values copied from the JSON string
   * @param targetType the POJO class that matches the expected JSON structure
   * @return a function that takes a JsonNode, and returns a new instance of the target type
   * @throws JsonPipelineInputException if the JSON could not be parsed, or the object not instantiated
   */
  public static <T> Func1<JsonNode, T> nodeToPojo(Class<T> targetType) {
    return jsonNode -> {
      String jsonString = nodeToString(jsonNode);
      return stringToPojo(targetType).call(jsonString);
    };
  }

}
