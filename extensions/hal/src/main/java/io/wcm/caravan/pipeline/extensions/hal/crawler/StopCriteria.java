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

public final class StopCriteria {

  private StopCriteria() {
    // nothing to do
  }

  /**
   * Allows crawler to be enabled always
   * @return stop criteria which is always enabled
   */
  public static StopCriterion alwaysEnabled() {
    return new StopCriterion() {

      @Override
      public boolean isStopRequested() {
        return false;
      }

      @Override
      public String getId() {
        return "ALWAYS-ENABLED";
      }

    };
  }

}
