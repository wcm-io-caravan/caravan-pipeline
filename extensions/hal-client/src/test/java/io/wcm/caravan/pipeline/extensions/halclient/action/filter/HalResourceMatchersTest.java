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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;

import org.junit.Test;


public class HalResourceMatchersTest {

  private static final HalResource DEFAULT_HAL_RESOURCE = HalResourceFactory.createResource("/resource");

  @Test
  public void relation_shouldReturnTrueForMatchingRelationName() {

    HalResourcePredicate matcher = HalResourceMatchers.relation("relationName");
    assertTrue(matcher.apply(new HalPath().add("relationName"), DEFAULT_HAL_RESOURCE));

  }

  @Test
  public void relation_shouldReturnFalseForWrongRelationName() {

    HalResourcePredicate matcher = HalResourceMatchers.relation("relationName");
    assertFalse(matcher.apply(new HalPath().add("otherRelationName"), DEFAULT_HAL_RESOURCE));

  }

  @Test
  public void collection_shouldReturnTrueForItemOfRelation() {

    HalResourcePredicate matcher = HalResourceMatchers.collection("relationName");
    assertTrue(matcher.apply(new HalPath().add("prefix").add("relationName").add("item"), DEFAULT_HAL_RESOURCE));

  }

  @Test
  public void collection_shouldReturnFalseForItemOfWrongRelation() {

    HalResourcePredicate matcher = HalResourceMatchers.collection("relationName");
    assertFalse(matcher.apply(new HalPath().add("prefix").add("otherRelationName").add("item"), DEFAULT_HAL_RESOURCE));

  }
}
