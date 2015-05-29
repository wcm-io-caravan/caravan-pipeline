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
package io.wcm.caravan.pipeline.extensions.halclient.action.filter;

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import rx.functions.Func1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * Default filtering predicates for HAL resources.
 */
public final class HalResourceFilters {

  private static final Configuration JSON_PATH_CONF = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS);

  /**
   * Executes all predicates and combines the result by logical {@code and}. If there are negative predicate results, all further predicates still get executed.
   * @param predicates Predicates to check
   * @return True if all predicates return true
   */
  public static HalResourcePredicate all(HalResourcePredicate... predicates) {
    return new HalResourcePredicate() {

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {

        boolean result = true;
        for (HalResourcePredicate predicate : predicates) {
          result = predicate.apply(halPath, hal) && result;
        }
        return result;

      }

      @Override
      public String getId() {
        List<String> ids = Streams.of(predicates).map(matcher -> matcher.getId()).collect(Collectors.toList());
        return "ALL(" + StringUtils.join(ids, '-') + ")";
      }
    };
  }

  /**
   * @param jsonPath JSON path to check
   * @return True if model has field(s) for given JSON path and all values are not null
   */
  public static HalResourcePredicate hasPathNonNull(String jsonPath) {
    JsonPath compiledPath = JsonPath.compile(jsonPath);
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "HAS(" + jsonPath + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        ObjectNode json = hal.getModel();
        ArrayNode matches = compiledPath.read(json, JSON_PATH_CONF);
        // check for empty result list
        if (matches == null || matches.size() == 0) {
          return false;
        }
        // check for null value
        for (JsonNode match : matches) {
          if (match == null || match.isNull()) {
            return false;
          }
        }
        return true;
      }

    };
  }

  /**
   * @param fieldName JSON field name
   * @return True if field exists and is not null
   */
  public static HalResourcePredicate hasNonNull(String fieldName) {
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "HAS(" + fieldName + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        return hal.getModel().hasNonNull(fieldName);
      }

    };
  }

  /**
   * @param relation Relation name of the embedded resource
   * @return True if HAL resource has one or more embedded resources for the given relation name
   */
  public static HalResourcePredicate hasEmbedded(String relation) {
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "HAS-EMBEDDED(" + relation + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        return hal.hasEmbedded(relation) && !hal.getEmbedded(relation).isEmpty();
      }

    };
  }

  /**
   * Converts the state of the HAL resource to an object of the given type and applies the provided function.
   * @param <S> Type of the HAL resource state
   * @param clazz Class type of the HAL resource state
   * @param function Function to apply on the object
   * @return Return value of the executed function
   */
  public static <S> HalResourcePredicate object(Class<S> clazz, Func1<S, Boolean> function) {
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "OBJECT(" + clazz.getSimpleName() + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        S object = HalResourceFactory.getStateAsObject(hal, clazz);
        return function.call(object);
      }

    };

  }

}
