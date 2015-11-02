/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.pipeline.extensions.hal.client.action;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

import java.util.List;

/**
 * Contains common implementations of {@link LinkSelectionStrategy}
 */
public final class LinkSelectionStrategies {

  private LinkSelectionStrategies() {
    // static methods only
  }

  /**
   * @param relation too look for
   * @param index to pick
   * @return a strategy that picks the link with the given relation and index
   */
  public static LinkSelectionStrategy byRelationAndIndex(String relation, int index) {

    return new LinkSelectionStrategy() {

      @Override
      public Link pickLink(HalResource resource) {
        List<Link> links = resource.getLinks(relation);
        if (links.size() <= index) {
          throw new IllegalStateException("HAL resource has no link with relation " + relation + " and index " + index);
        }
        return links.get(index);

      }

      @Override
      public String getId() {
        return "rel=" + relation + "&index=" + index;
      }
    };
  }

  /**
   * @param relation too look for
   * @param name of the link to pick
   * @return a strategy that picks the first link with the given relation and name
   */
  public static LinkSelectionStrategy byRelationAndName(String relation, String name) {

    return new LinkSelectionStrategy() {

      @Override
      public Link pickLink(HalResource resource) {
        List<Link> links = resource.getLinks(relation);

        for (Link link : links) {
          if (name.equals(link.getName())) {
            return link;
          }
        }
        throw new IllegalStateException("HAL resource has no link with relation " + relation + " and name " + name);
      }


      @Override
      public String getId() {
        return "rel=" + relation + "&name=" + name;
      }
    };
  }
}
