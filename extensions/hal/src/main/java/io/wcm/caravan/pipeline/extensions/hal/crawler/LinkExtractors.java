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
package io.wcm.caravan.pipeline.extensions.hal.crawler;

import io.wcm.caravan.commons.hal.HalUtil;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.Link;

import com.google.common.collect.ListMultimap;

/**
 * Common link extractors.
 */
public final class LinkExtractors {

  private LinkExtractors() {
    // nothing to do
  }

  /**
   * Returns all relations and links in a HAL resource.
   * @return All links
   */
  public static LinkExtractor all() {
    return new LinkExtractor() {

      @Override
      public ListMultimap<String, Link> extract(HalResource hal) {
        return HalUtil.getAllLinks(hal);
      }

      @Override
      public String getId() {
        return "ALL";
      }

    };
  }

  /**
   * Returns all relations and links in a HAL resource except the CURI links.
   * @return All links
   */
  public static LinkExtractor allExceptCuries() {

    return new LinkExtractor() {

      @Override
      public String getId() {
        return "ALL-EXCEPT-CURIES";
      }

      @Override
      public ListMultimap<String, Link> extract(HalResource hal) {
        return HalUtil.getAllLinksExceptCuries(hal);
      }
    };

  }
}
