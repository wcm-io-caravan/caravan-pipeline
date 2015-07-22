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

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.hal.resource.util.HalUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.annotation.versioning.ProviderType;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.ListMultimap;

/**
 * Common link extractors.
 */
@ProviderType
public final class LinkExtractors {

  private LinkExtractors() {
    // nothing to do
  }

  /**
   * Returns all relations and links in a HAL resource except CURI links.
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
   * Returns all relations and links in a HAL resource having no URI template expressions.
   * @return Filtered links
   */
  public static LinkExtractor noUriTemplates() {
    return new LinkExtractor() {

      @Override
      public ListMultimap<String, Link> extract(HalResource hal) {
        return HalUtil.getAllLinks(hal, new Predicate<Pair<String, Link>>() {

          @Override
          public boolean apply(Pair<String, Link> input) {
            return UriTemplate.fromTemplate(input.getValue().getHref()).expressionCount() == 0;
          }
        });
      }

      @Override
      public String getId() {
        return "NO-URI-TEMPLATES";
      }


    };
  }

  /**
   * Returns all relations and links provided by the {@code delegate}d {@link LinkExtractor} where the URI starts with
   * the given prefix.
   * @param prefix URI prefix
   * @param delegate Delegated link extractor
   * @return Link extractor for prefixed URIs
   */
  public static LinkExtractor filterByPrefix(String prefix, LinkExtractor delegate) {
    return new LinkExtractor() {

      @Override
      public String getId() {
        return "ONLY-STARTING-WITH('" + prefix + "', " + delegate.getId() + ")";
      }

      @Override
      public ListMultimap<String, Link> extract(HalResource hal) {

        ListMultimap<String, Link> fromDelegate = delegate.extract(hal);
        Builder<String, Link> builder = ImmutableListMultimap.builder();
        Streams.of(fromDelegate.entries())
            .filter(entry -> StringUtils.startsWith(entry.getValue().getHref(), prefix))
            .forEach(entry -> builder.put(entry));
        return builder.build();

      }
    };
  }

}
