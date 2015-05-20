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
package io.wcm.caravan.pipeline.cache.guava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;


public class GuavaCacheAdapterCachingStrategyTest extends AbstractGuavaTestCase {

  private static final String ENTRY_SAMPLE = "entrySample";

  private static final String BIGGER_ENTRY_SAMPLE = "biggerEntrySample";

  private static final String SMALLER_ENTRY_SAMPLE = "entry";

  @Override
  protected Map<String, Object> getCacheConfig() {

    // define the size of an actual cache entry in bytes
    BigDecimal bigDecimal = new BigDecimal((8 * ((((ENTRY_SAMPLE.length()) * 2) + 45) / 8)) + (8 * (((("entryKey00".length()) * 2) + 45) / 8)));
    double cacheMaximumWeightInMegaBytes = bigDecimal.divide(new BigDecimal(1024)).divide(new BigDecimal(1024)).doubleValue();

    // define the maximum cache weight in bytes
    cacheMaximumWeightInMegaBytes = cacheMaximumWeightInMegaBytes * 4;

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("maxCacheSizeMB", cacheMaximumWeightInMegaBytes);
    return map;
  }

  @Test
  public void testCachingStrategySingleEntry() {
    cacheAdapter.put("entryKey", ENTRY_SAMPLE, null);
    assertEntry("entryKey", ENTRY_SAMPLE);
  }

  @Test
  public void testCachingStrategyBiggerEntry() {
    LinkedList<String> keysList = new LinkedList<String>();

    // bigger entry can not be put into cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey123", BIGGER_ENTRY_SAMPLE, "entryKey123", keysList);

  }

  @Test
  public void testCachingStrategyDoubleEntry() {
    cacheAdapter.put("entryKey", ENTRY_SAMPLE, null);
    assertEntry("entryKey", ENTRY_SAMPLE);
    cacheAdapter.put("entryKey2", ENTRY_SAMPLE, null);
    assertEntry("entryKey", ENTRY_SAMPLE);
    assertEntry("entryKey2", ENTRY_SAMPLE);
  }

  @Test
  public void testCachingStrategyMultipleSmallEntries() {
    LinkedList<String> keysList = new LinkedList<String>();

    // put and get single entry
    assertAddNewEntry("entryKey", SMALLER_ENTRY_SAMPLE, keysList);

    // put second entry and check if the first and the second entries are available
    assertAddNewEntry("entryKey2", SMALLER_ENTRY_SAMPLE, keysList);

    // put third entry
    assertAddNewEntry("entryKey3", SMALLER_ENTRY_SAMPLE, keysList);

    // put fourth entry
    // the third entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey4", SMALLER_ENTRY_SAMPLE, "entryKey3", keysList);

    // put fifth entry
    assertAddNewEntry("entryKey5", SMALLER_ENTRY_SAMPLE, keysList);

    // put sixth entry
    // the fifth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey6", SMALLER_ENTRY_SAMPLE, "entryKey5", keysList);

    // put seventh entry
    // the second entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey7", SMALLER_ENTRY_SAMPLE, "entryKey2", keysList);

    // put eight entry
    // the seventh entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey8", SMALLER_ENTRY_SAMPLE, "entryKey7", keysList);

    // put ninth entry
    // the first entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey9", SMALLER_ENTRY_SAMPLE, "entryKey", keysList);

    // put tenth entry
    // the ninth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey10", SMALLER_ENTRY_SAMPLE, "entryKey9", keysList);

    // put eleventh entry
    // the sixth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey11", SMALLER_ENTRY_SAMPLE, "entryKey6", keysList);
  }



  @Test
  public void testCachingStrategyMultipleEntries() {
    LinkedList<String> keysList = new LinkedList<String>();

    // put and get single entry
    assertAddNewEntry("entryKey", ENTRY_SAMPLE, keysList);

    // put second entry and check if the first and the second entries are available
    assertAddNewEntry("entryKey2", ENTRY_SAMPLE, keysList);

    // put third entry
    assertAddNewEntry("entryKey3", ENTRY_SAMPLE, keysList);

    // put fourth entry
    // the third entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey4", ENTRY_SAMPLE, "entryKey3", keysList);

    // put fifth entry
    assertAddNewEntry("entryKey5", ENTRY_SAMPLE, keysList);

    // put sixth entry
    // the fifth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey6", ENTRY_SAMPLE, "entryKey5", keysList);

    // put seventh entry
    // the second entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey7", ENTRY_SAMPLE, "entryKey2", keysList);

    // put eight entry
    // the seventh entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey8", ENTRY_SAMPLE, "entryKey7", keysList);

    // put ninth entry
    // the first entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey9", ENTRY_SAMPLE, "entryKey", keysList);

    // put tenth entry
    // the ninth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey10", ENTRY_SAMPLE, "entryKey9", keysList);

    // put eleventh entry
    // the sixth entry was cleaned from the cache
    assertAddNewEntryAndExpectedToBeRemoved("entryKey11", ENTRY_SAMPLE, "entryKey6", keysList);
  }

  private void assertAddNewEntryAndExpectedToBeRemoved(String newKey, String entry, String expectedToRemoveKey, List<String> entryKeys) {
    entryKeys.add(newKey);
    cacheAdapter.put(newKey, entry, null);
    if (expectedToRemoveKey != null) {
      assertEntryRemoved(expectedToRemoveKey);
      entryKeys.remove(expectedToRemoveKey);
    }
    assertEntries(entryKeys, entry);
  }

  private void assertAddNewEntry(String newKey, String entry, List<String> entryKeys) {
    cacheAdapter.put(newKey, entry, null);
    entryKeys.add(newKey);
    assertEntries(entryKeys, entry);
  }

  private void assertEntries(List<String> entryKeys, String entry) {
    for (String entryKey : entryKeys) {
      assertEntry(entryKey, entry);
    }
  }

  private void assertEntry(String entryKey, String entry) {
    String receivedEntry = cacheAdapter.get(entryKey, null).toBlocking().first();
    assertEquals("EntryKey: " + entryKey, entry, receivedEntry);
  }

  private void assertEntryRemoved(String entryKey) {
    assertTrue("EntryKey: " + entryKey, cacheAdapter.get(entryKey, null).isEmpty().toBlocking().single());
  }
}
