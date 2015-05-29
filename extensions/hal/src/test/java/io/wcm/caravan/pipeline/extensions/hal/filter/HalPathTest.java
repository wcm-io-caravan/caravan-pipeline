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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class HalPathTest {

  private static final HalPath ROOT = new HalPath();
  private static final HalPath DEFAULT = ROOT.add("p1").add("p2").add("p3");

  @Test
  public void get_shouldReturnNullForNegativeIndex() {
    assertNull(DEFAULT.get(-1));
  }

  @Test
  public void get_shouldReturnNullForTooBigIndex() {
    assertNull(DEFAULT.get(10));
  }

  @Test
  public void get_shouldReturnNthRelation() {
    assertEquals("p2", DEFAULT.get(1));
  }

  @Test
  public void toString_shouldReturnSlashForNoRelations() {
    assertEquals("/", ROOT.toString());
  }

  @Test
  public void toString_shouldReturnSlashedPath() {
    assertEquals("/p1/p2/p3", DEFAULT.toString());
  }

  @Test
  public void last_shouldReturnNullForNoRelations() {
    assertNull(ROOT.last(1));
  }

  @Test
  public void last_shouldReturnNullForMissingRelation() {
    assertNull(DEFAULT.last(5));
  }

  @Test
  public void last_shouldReturnRelationForGivenIndex() {
    assertEquals("p2", DEFAULT.last(1));
  }

  @Test
  public void add_shouldCreateNewObject() {
    HalPath path = ROOT.add("path");
    assertNotEquals(ROOT, path);
  }

  @Test
  public void current_shouldReturnNullForRoot() {
    assertNull(ROOT.current());
  }

  @Test
  public void current_shouldReturnLastRelation() {
    assertEquals("p3", DEFAULT.current());
  }

  @Test
  public void first_shouldReturnNullForRoot() {
    assertNull(ROOT.first());
  }

  @Test
  public void first_shouldReturnFirstRelation() {
    assertEquals("p1", DEFAULT.first());
  }

  @Test
  public void size_shouldReturnNumberOfRelations() {
    assertEquals(3, DEFAULT.size());
  }

}
