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
package io.wcm.caravan.pipeline.cache;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CacheDateUtils {

  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  public static DateFormat getDateFormat() {
    return new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
  }

  public static String formatRelativeTime(int secondsRelativeToNow) {
    return getDateFormat().format(new Date(new Date().getTime() + 1000 * secondsRelativeToNow));
  }

  public static String formatCurrentTime() {
    return getDateFormat().format(new Date());
  }

  public static Date parse(String rfc1123Date) {
    try {
      return getDateFormat().parse(rfc1123Date);
    }
    catch (ParseException e) {
      throw new RuntimeException("Failed to parse date from string " + rfc1123Date, e);
    }
  }

  public static int getSecondsSince(String rfc1123Date) {

    Date reference = parse(rfc1123Date);
    Date now = new Date();

    return (int)(now.getTime() - reference.getTime()) / 1000;
  }
}
