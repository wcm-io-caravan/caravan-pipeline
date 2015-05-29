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
package io.wcm.caravan.pipeline.extensions.hal.filter;

import static io.wcm.caravan.pipeline.extensions.hal.filter.HalResourceFilters.all;
import static io.wcm.caravan.pipeline.extensions.hal.filter.HalResourceFilters.hasEmbedded;
import static io.wcm.caravan.pipeline.extensions.hal.filter.HalResourceFilters.hasNonNull;
import static io.wcm.caravan.pipeline.extensions.hal.filter.HalResourceFilters.hasPathNonNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.pipeline.extensions.hal.filter.HalPath;
import io.wcm.caravan.pipeline.extensions.hal.filter.HalResourceFilters;
import io.wcm.caravan.pipeline.extensions.hal.filter.HalResourcePredicate;
import io.wcm.caravan.testing.json.JsonTestEnvironment;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class HalResourceFiltersTest {

  private ObjectNode item1 = HalResourceFactory.createResource("/item1")
      .getModel().put("key", "val1").putNull("particularNull");
  private ObjectNode item2 = HalResourceFactory.createResource("/item1")
      .getModel().put("key", "val2").put("particularNull", "value");
  private HalResource payload = HalResourceFactory.createResource("/")
      .addEmbedded("item", new HalResource(item1), new HalResource(item2));

  @Before
  public void setup() {
    JsonTestEnvironment.init();
  }

  @Test
  public void hasPath_shouldReturnTrueForExistingPath() {

    HalResourcePredicate predicate = hasPathNonNull("$._embedded.item[*].key");
    assertTrue(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void hasPath_shouldReturnFalseForMissingPath() {

    HalResourcePredicate predicate = hasPathNonNull("$._embedded.item[0].missing");
    assertFalse(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void hasPath_shouldReturnFalseForOneNull() {

    HalResourcePredicate predicate = hasPathNonNull("$._embedded.item[0].particularNull");
    assertFalse(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void all_shouldReturnTrueForAllPositive() {

    HalResourcePredicate[] predicates = new HalResourcePredicate[10];
    for (int i = 0; i < predicates.length; i++) {
      predicates[i] = Mockito.mock(HalResourcePredicate.class);
      Mockito.when(predicates[i].apply(Matchers.any(), Matchers.any())).thenReturn(true);
    }
    HalResourcePredicate predicate = all(predicates);
    assertTrue(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void all_shouldReturnFlaseForOneNegative() {

    HalResourcePredicate[] predicates = new HalResourcePredicate[10];
    for (int i = 0; i < predicates.length; i++) {
      predicates[i] = Mockito.mock(HalResourcePredicate.class);
      Mockito.when(predicates[i].apply(Matchers.any(), Matchers.any())).thenReturn(i != 5);
    }
    HalResourcePredicate predicate = all(predicates);
    assertFalse(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void all_shouldExecuteAlwaysAllPredicates() {

    HalResourcePredicate[] predicates = new HalResourcePredicate[10];
    for (int i = 0; i < predicates.length; i++) {
      predicates[i] = Mockito.mock(HalResourcePredicate.class);
      Mockito.when(predicates[i].apply(Matchers.any(), Matchers.any())).thenReturn(i != 5);
    }
    HalResourcePredicate predicate = all(predicates);
    predicate.apply(new HalPath(), payload);

    for (int i = 0; i < predicates.length; i++) {
      Mockito.verify(predicates[i]).apply(Matchers.any(), Matchers.any());
    }

  }

  @Test
  public void hasNonNull_shouldReturnTrueForNonNullField() {

    HalResourcePredicate predicate = hasNonNull("particularNull");
    assertTrue(predicate.apply(new HalPath(), payload.getEmbedded("item").get(1)));

  }

  @Test
  public void hasNonNull_shouldReturnFalseForNullField() {

    HalResourcePredicate predicate = hasNonNull("particularNull");
    assertFalse(predicate.apply(new HalPath(), payload.getEmbedded("item").get(0)));

  }

  @Test
  public void hasNonNull_shouldReturnFalseForMissingField() {

    HalResourcePredicate predicate = hasNonNull("missing");
    assertFalse(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void hasEmbedded_shouldReturnTrueForExistingEmbeddedResource() {

    HalResourcePredicate predicate = hasEmbedded("item");
    assertTrue(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void hasEmbedded_shouldReturnFalseForMissingEmbeddedResource() {

    HalResourcePredicate predicate = hasEmbedded("missing");
    assertFalse(predicate.apply(new HalPath(), payload));

  }

  @Test
  public void object_shouldReturnFunctionResponse() {

    HalResourcePredicate predicate = HalResourceFilters.object(Item.class, item -> item.particularNull != null);
    assertFalse(predicate.apply(new HalPath(), payload.getEmbedded("item").get(0)));
    assertTrue(predicate.apply(new HalPath(), payload.getEmbedded("item").get(1)));

  }

  private static class Item {

    public String key;
    public String particularNull;
  }

}
