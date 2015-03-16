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

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import rx.Observable;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableListMultimap;

/**
 * Default implementation of {@link JsonPipelineFactory}.
 */
@Component
@Service(JsonPipelineFactory.class)
public final class JsonPipelineFactoryImpl implements JsonPipelineFactory {

  @Reference
  private CaravanHttpClient transport;

  @Reference
  private CacheAdapter cacheAdapter;

  @Reference
  private MetricRegistry metricRegistry;

  @Override
  public JsonPipeline create(final CaravanHttpRequest request) {
    return create(request, Collections.emptyMap());
  }


  public JsonPipeline create(final CaravanHttpRequest request, Map<String, String> cacheMetadataProperties) {

    // note that #execute will *not* actually start the request, but just create an observable that will initiate
    // the request when #subscribe is called on the pipeline's output observable
    Observable<CaravanHttpResponse> response = transport.execute(request);

    return new JsonPipelineImpl(request, response, cacheAdapter, metricRegistry, cacheMetadataProperties);
  }

  @Override
  public JsonPipeline createEmpty() {
    return createEmpty(Collections.emptyMap());
  }


  public JsonPipeline createEmpty(Map<String, String> cacheMetadataProperties) {

    CaravanHttpRequest dummyRequest = new CaravanHttpRequestBuilder("").build();

    // make sure to set a Cache-Control header to mark the empty response as indefinitely cacheable,
    // otherwise the default max-age value of zero would become effective
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.of("Cache-Control", "max-age: " + Long.toString(TimeUnit.DAYS.toSeconds(365)));

    CaravanHttpResponse emptyJsonResponse = CaravanHttpResponse.create(200, "Ok", headers, "{}", Charset.forName("UTF-8"));

    return new JsonPipelineImpl(dummyRequest, Observable.just(emptyJsonResponse), cacheAdapter, metricRegistry, cacheMetadataProperties);
  }

}
