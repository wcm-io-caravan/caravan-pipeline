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

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * Represents a path of embedded resources in a HAL resource by concatenating the relation names of the embedded ones.
 */
public class HalPath {

  private final ImmutableList<String> relations;

  /**
   * Initializes a root
   */
  public HalPath() {
    relations = ImmutableList.of();
  }

  private HalPath(ImmutableList<String> relations) {
    this.relations = relations;
  }

  /**
   * Adds a relation to the path
   * @param relation Relation name of the embedded resource
   * @return New HAL path
   */
  public HalPath add(String relation) {
    return new HalPath(ImmutableList.<String> builder().addAll(relations).add(relation).build());
  }

  /**
   * @param index Position in the path
   * @return Relation for the given index. Can be null
   */
  public String get(int index) {
    return index < 0 || index > relations.size() - 1 ? null : relations.get(index);
  }

  /**
   * @return The first relation name in the path. Can be null
   */
  public String first() {
    return get(0);
  }


  /**
   * @see HalPath#last(int) {@code last(0)}
   * @return Relation name of the current embedded resource
   */
  public String current() {
    return last(0);
  }
  /**
   * Returns the n-th last relations name in the path
   * @param last Step from the end of the path
   * @return Relation name. Can be {@code null}
   */
  public String last(int last) {
    int index = relations.size() - 1 - last;
    return get(index);
  }

  /**
   * @return Number of relation names
   */
  public int size() {
    return relations.size();
  }

  @Override
  public String toString() {
    return '/' + StringUtils.join(relations, '/');
  }

}
