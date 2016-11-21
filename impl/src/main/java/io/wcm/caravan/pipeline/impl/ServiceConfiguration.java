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
package io.wcm.caravan.pipeline.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

/**
 * configuration for json pipeline.
 */
@Component(immediate = true, metatype = true, label = "wcm.io Caravan Pipeline Configuration")
@Service(ServiceConfiguration.class)
public class ServiceConfiguration {

  @Property(label = "io thread for response observable",
    description = "'perform response observable with io thread. The original thread will be released",
    boolValue = false )
  static final String IO_THREAD_FOR_RESPONSE_OBSERVABLE = "ioThreadForResponseObservable";

  private boolean useIoThreadForResponseObservable;

  @Activate
  private void activate(Map<String, Object> config) {
    useIoThreadForResponseObservable = PropertiesUtil.toBoolean(config.get(IO_THREAD_FOR_RESPONSE_OBSERVABLE), false);
  }

  public boolean useIoThreadForResponseObservable() {
    return useIoThreadForResponseObservable;
  }
}
