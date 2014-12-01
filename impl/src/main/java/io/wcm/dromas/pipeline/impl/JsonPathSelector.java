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
package io.wcm.dromas.pipeline.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Function that evaluates a JSONpath expression on a Jackson {@link JsonNode} tree, and returns an {@link ArrayNode}
 * with all matching results.
 */
public final class JsonPathSelector implements Func1<JsonNode, ArrayNode> {

  private static final Logger log = LoggerFactory.getLogger(JsonPathSelector.class);

  private static Configuration config = new ConfigurationBuilder()
  .options(Option.REQUIRE_PROPERTIES, Option.ALWAYS_RETURN_LIST).build();

  private final String jsonPath;

  JsonPathSelector(String jsonPath) {
    this.jsonPath = jsonPath;
  }

  @Override
  public ArrayNode call(JsonNode inputData) throws PathNotFoundException {
    Stopwatch watch = Stopwatch.createStarted();

    ArrayNode arrayNode = JsonPath
        .using(config)
        .parse(inputData)
        .read(jsonPath, ArrayNode.class);

    log.info("selected " + arrayNode.size() + " matches in " + watch.elapsed(MILLISECONDS) + " ms by applying jsonPath " + this.jsonPath);
    return arrayNode;
  }

}
