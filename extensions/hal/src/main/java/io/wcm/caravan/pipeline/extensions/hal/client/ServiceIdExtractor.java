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
package io.wcm.caravan.pipeline.extensions.hal.client;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;

/**
 * Extract the serviceId from a given URL.
 * This interface was introduced to address one shortcoming of the {@link HalClient}: it was assuming that all links in
 * a {@link HalResource} are pointing to the same service. Therefore all {@link CaravanHttpRequest}s were created with
 * the serviceId that was initially passed when creating the HalClient, not the serviceId where the URL is actually
 * pointing to. This interface now allows to specify a rule that can extract the serviceId from a given path.
 */
@FunctionalInterface
public interface ServiceIdExtractor {

  /**
   * @param href an absolute path from a link that is followed by the HalCient
   * @return the serviceId that should be used when creating a {@link CaravanHttpRequest} for that link
   */
  String getServiceId(String href);
}