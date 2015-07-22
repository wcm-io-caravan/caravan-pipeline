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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(MockitoJUnitRunner.class)
public class ReportHalResourceFiltersTest {

  private static final HalPath HAL_PATH = new HalPath().add("section").add("item");

  @Mock
  private HalResourcePredicate delegate1;
  @Mock
  private HalResourcePredicate delegate2;

  private HalResource report;
  private HalResource input;

  @Before
  public void setUp() {
    report = HalResourceFactory.createResource("/report");
    Mockito.when(delegate1.getId()).thenReturn("HAS-EMBEDDED(x)");
    Mockito.when(delegate2.getId()).thenReturn("HAS-EMBEDDED(y)");
    input = HalResourceFactory.createResource("/resource");
  }

  @Test
  public void report_shouldIgnoreFilterReturningNoError() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(true);
    ReportHalResourceFilters.report(report, delegate1).apply(HAL_PATH, input);
    assertTrue(report.getEmbedded("item").isEmpty());
  }

  @Test
  public void report_shouldSetSelfLinkForErrorResource() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.report(report, delegate1).apply(HAL_PATH, input);
    assertEquals(input.getLink().getHref(), report.getEmbedded("item").get(0).getLink().getHref());
  }

  @Test
  public void report_shouldSetHalPathForErrorResource() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.report(report, delegate1).apply(HAL_PATH, input);
    assertEquals("/section/item", report.getEmbedded("item").get(0).getModel().get("halPath").asText());
  }

  @Test
  public void report_shouldSetMessageForErrorResource() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.report(report, delegate1).apply(HAL_PATH, input);
    assertEquals("HAS-EMBEDDED(x)", report.getEmbedded("item").get(0).getModel().get("errors").get(0).asText());
  }

  @Test
  public void report_shouldCopyTheJsonNodeHavingAnError() {

    input.getModel().put("att1", "value").putObject("att2").put("att3", "value3");
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.report(report, delegate1).apply(HAL_PATH, input);
    JsonNode copy = report.getEmbedded("item").get(0).getModel().get("copy");
    assertEquals(input.getModel().toString(), copy.toString());
    input.getModel().remove("att1");
    assertNotEquals(input.getModel().toString(), copy.toString());

  }

  @Test
  public void all_shouldIgnoreFilterReturningNoError() {

    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(true);
    Mockito.when(delegate2.apply(HAL_PATH, input)).thenReturn(true);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    assertTrue(report.getEmbedded("item").isEmpty());

  }

  @Test
  public void all_shouldSetSelfLinkForErrorResource() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    assertEquals(input.getLink().getHref(), report.getEmbedded("item").get(0).getLink().getHref());
  }

  @Test
  public void all_shouldSetHalPathForErrorResource() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    assertEquals("/section/item", report.getEmbedded("item").get(0).getModel().get("halPath").asText());
  }

  @Test
  public void all_shouldSetMessagesForErrorResource() {

    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    Mockito.when(delegate2.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    assertEquals("HAS-EMBEDDED(x)", report.getEmbedded("item").get(0).getModel().get("errors").get(0).asText());
    assertEquals("HAS-EMBEDDED(y)", report.getEmbedded("item").get(0).getModel().get("errors").get(1).asText());

  }

  @Test
  public void all_shouldExecuteAllFiltersEvenIfFirstFails() {
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    Mockito.verify(delegate2).apply(HAL_PATH, input);
  }

  @Test
  public void all_shouldCopyTheJsonNodeHavingAnError() {

    input.getModel().put("att1", "value").putObject("att2").put("att3", "value3");
    Mockito.when(delegate1.apply(HAL_PATH, input)).thenReturn(false);
    ReportHalResourceFilters.all(report, delegate1, delegate2).apply(HAL_PATH, input);
    JsonNode copy = report.getEmbedded("item").get(0).getModel().get("copy");
    assertEquals(input.getModel().toString(), copy.toString());
    input.getModel().remove("att1");
    assertNotEquals(input.getModel().toString(), copy.toString());

  }

}
