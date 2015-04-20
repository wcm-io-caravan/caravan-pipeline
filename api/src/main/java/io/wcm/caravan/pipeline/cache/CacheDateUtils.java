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
import java.util.TimeZone;

import org.osgi.annotation.versioning.ProviderType;

/**
 * some formatting/parsing functions for the date-format to be used in HTTP headers (see
 * http://tools.ietf.org/html/rfc2616#section-3.3.1)
 */
@ProviderType
public final class CacheDateUtils {

  private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
  private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");

  private CacheDateUtils() {

  }

  private static DateFormat getDateFormat() {
    SimpleDateFormat df = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
    df.setTimeZone(GMT_TIME_ZONE);
    return df;
  }

  /**
   * @return the current date and time in RFC-1123 format (e.g. "Sun, 06 Nov 1994 08:49:37 GMT")
   */
  public static String formatCurrentTime() {
    return getDateFormat().format(new Date());
  }

  /**
   * @param secondsRelativeToNow offset to the current time (positive or negative!)
   * @return that time in RFC-1123 format (e.g. "Sun, 06 Nov 1994 08:49:37 GMT")
   */
  public static String formatRelativeTime(int secondsRelativeToNow) {
    return getDateFormat().format(new Date(new Date().getTime() + 1000L * secondsRelativeToNow));
  }

  /**
   * @param rfc1123Date date and time in RFC-1123 format (e.g. "Sun, 06 Nov 1994 08:49:37 GMT")
   * @return the parsed date
   */
  public static Date parse(String rfc1123Date) {
    try {
      return getDateFormat().parse(rfc1123Date);
    }
    catch (ParseException e) {
      throw new RuntimeException("Failed to parse date from string " + rfc1123Date, e);
    }
  }

  /**
   * @param rfc1123Date date and time in RFC-1123 format (e.g. "Sun, 06 Nov 1994 08:49:37 GMT")
   * @return the number of seconds that have passed since the given date
   */
  public static int getSecondsSince(String rfc1123Date) {

    Date reference = parse(rfc1123Date);
    Date now = new Date();

    return (int)((now.getTime() - reference.getTime()) / 1000L);
  }

  /**
   * @param rfc1123Date date and time in RFC-1123 format (e.g. "Sun, 06 Nov 1994 08:49:37 GMT")
   * @return the number of seconds until the given date
   */
  public static int getSecondsUntil(String rfc1123Date) {
    return -getSecondsSince(rfc1123Date);
  }
}
