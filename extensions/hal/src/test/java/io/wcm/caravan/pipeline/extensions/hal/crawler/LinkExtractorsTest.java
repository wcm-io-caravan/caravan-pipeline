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

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.hal.resource.HalResourceFactory;
import io.wcm.caravan.commons.hal.resource.Link;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ListMultimap;


public class LinkExtractorsTest {

  private HalResource payload;

  @Before
  public void setUp() {

    payload = HalResourceFactory.createResource("/resource")
        .addLinks("item", HalResourceFactory.createLink("/link-1"), HalResourceFactory.createLink("/link-2"))
        .addEmbedded("item", HalResourceFactory.createResource("/embedded-1")
            .addLinks("item", HalResourceFactory.createLink("/embedded-1-link1"), HalResourceFactory.createLink("/embedded-1-link2")));
  }

  @Test
  public void all_shouldReturnAllLinks() {

    ListMultimap<String, Link> links = LinkExtractors.all().extract(payload);
    assertEquals(6, links.size());
    assertEquals("/resource", links.get("self").get(0).getHref());

  }

}
