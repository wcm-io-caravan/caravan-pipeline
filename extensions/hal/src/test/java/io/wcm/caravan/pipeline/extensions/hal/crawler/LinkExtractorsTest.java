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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class LinkExtractorsTest {

  private HalResource payload;

  @Before
  public void setUp() {

    payload = new HalResource("/resource")
        .addLinks("item", new Link("/link-1"), new Link("/link-2"))
        .addLinks("templated", new Link("/template{?param}"))
        .addEmbedded("item", new HalResource("/embedded-1")
            .addLinks("item", new Link("/embedded-1-link1"), new Link("/embedded-1-link2")));
  }

  @Test
  public void all_shouldReturnAllLinks() {
    ListMultimap<String, Link> links = LinkExtractors.all().extract(payload);
    assertEquals(7, links.size());
    assertEquals("/resource", links.get("self").get(0).getHref());
    assertTrue(links.containsKey("templated"));
  }

  @Test
  public void noUriTemplate_shouldReturnOnlyLinksWithoutTemplateExpresssion() {
    ListMultimap<String, Link> links = LinkExtractors.noUriTemplates().extract(payload);
    assertEquals(6, links.size());
    assertFalse(links.containsKey("templated"));
  }

  @Test
  public void onlyUrisStartingWith_shouldFilterLinksWithOtherPrefixes() {

    LinkExtractor delegate = Mockito.mock(LinkExtractor.class);
    ListMultimap<String, Link> inputList = ImmutableListMultimap.of("item", new Link("/correct/item/1"), "item",
        new Link("/other/item/2"));
    Mockito.when(delegate.extract(any())).thenReturn(inputList);
    ListMultimap<String, Link> result = LinkExtractors.filterByPrefix("/correct", delegate).extract(null);
    assertEquals(1, result.size());
    assertEquals("/correct/item/1", result.get("item").get(0).getHref());

  }

}
