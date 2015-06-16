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
package io.wcm.caravan.pipeline.extensions.hal.filter;

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

/**
 * Collection of common reporting {@link HalResourcePredicate}s which write negative predicate results into a HAL
 * document.
 */
public final class ReportHalResourceFilters {

  private ReportHalResourceFilters() {
    // nothing to do
  }

  /**
   * Executes all delegated filters and creates a report with error messages for each failed filter. If there are
   * negative predicate results, all further predicates still get executed.
   */
  public static HalResourcePredicate all(HalResource report, HalResourcePredicate... delegates) {

    return new HalResourcePredicate() {

      @Override
      public String getId() {
        List<String> ids = Streams.of(delegates).map(matcher -> matcher.getId()).collect(Collectors.toList());
        return "ALL(" + StringUtils.join(ids, '+') + ")";
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {

        List<String> errors = Streams.of(delegates)
            .filter(delegate -> !delegate.apply(halPath, hal))
            .map(delegate -> delegate.getId())
            .collect(Collectors.toList());
        if (!errors.isEmpty()) {
          report.addEmbedded("item", createReport(halPath, hal, errors));
        }
        return errors.isEmpty();

      }

    };

  }

  /**
   * @param report Report to write to
   * @param delegate Delegated filter
   */
  public static HalResourcePredicate report(HalResource report, HalResourcePredicate delegate) {

    return new HalResourcePredicate() {

      @Override
      public String getId() {
        return delegate.getId();
      }

      @Override
      public boolean apply(HalPath halPath, HalResource hal) {

        if (!delegate.apply(halPath, hal)) {
          report.addEmbedded("item", createReport(halPath, hal, Lists.newArrayList(delegate.getId())));
          return false;
        }
        return true;

      }

    };

  }

  private static HalResource createReport(HalPath halPath, HalResource hal, List<String> errors) {

    HalResource filterReport = HalResourceFactory.createResource(hal.getLink().getHref());
    filterReport.getModel().put("halPath", halPath.toString());
    ArrayNode errorContainer = filterReport.getModel().putArray("errors");
    for (String error : errors) {
      errorContainer.add(error);
    }

    filterReport.getModel().set("copy", hal.getModel().deepCopy());

    return filterReport;

  }

}
