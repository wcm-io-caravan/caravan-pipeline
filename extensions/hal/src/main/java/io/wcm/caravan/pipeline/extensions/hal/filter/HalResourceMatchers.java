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

import io.wcm.caravan.hal.resource.HalResource;

import org.apache.commons.lang3.StringUtils;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Default HAL resource matchers.
 */
@ProviderType
public final class HalResourceMatchers {

  private HalResourceMatchers() {
    // nothing to do
  }

  /**
   * @param relationToMatch HAL resource relation
   * @return True if resource has given relation
   */
  public static HalResourcePredicate relation(String relationToMatch) {
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "MATCHES(/" + relationToMatch + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        return StringUtils.equals(relationToMatch, halPath.current());
      }

    };
  }

  /**
   * @param relationToMatch Relation of the {@code item}
   * @return True if resource is an {@code item} of the given relation.
   */
  public static HalResourcePredicate collection(String relationToMatch) {
    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return "MATCHES(/" + relationToMatch + "/item)";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {
        return StringUtils.equals("item", halPath.current())
            && StringUtils.equals(relationToMatch, halPath.last(1));
      }
    };
  }

}
