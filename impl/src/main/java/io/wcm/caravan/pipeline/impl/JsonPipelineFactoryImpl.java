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
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;
import io.wcm.caravan.pipeline.impl.cache.MultiLayerCacheAdapter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.RankedServices;

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

  @Reference(name = "cacheAdapter", referenceInterface = CacheAdapter.class,
      cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private final RankedServices<CacheAdapter> cacheAdapters = new RankedServices<>();

  @Reference
  private MetricRegistry metricRegistry;

  @Reference
  private ServiceConfiguration serviceConfiguration;

  /** constructor used in a OSGi context */
  public JsonPipelineFactoryImpl() {
    // empty constructor
  }

  /**
   * explicit dependency injection (to be used in unit-tests)
   * @param transport the implementation to use to fetch responses
   * @param metricRegistry for gathering monitoring statistics
   */
  public JsonPipelineFactoryImpl(CaravanHttpClient transport, MetricRegistry metricRegistry) {
    this.transport = transport;
    this.metricRegistry = metricRegistry;
  }

  @Override
  public JsonPipeline create(final CaravanHttpRequest request) {
    return create(request, Collections.emptyMap());
  }

  @Override
  public JsonPipeline create(final CaravanHttpRequest request, Map<String, String> contextProperties) {

    // note that #execute will *not* actually start the request, but just create an observable that will initiate
    // the request when #subscribe is called on the pipeline's output observable
    Observable<CaravanHttpResponse> response = transport.execute(request);

    return new JsonPipelineImpl(request, response,
        new JsonPipelineContextImpl(this, createMultiLayerCacheAdapter(), metricRegistry, contextProperties, serviceConfiguration));
  }

  @Override
  public JsonPipeline createEmpty() {
    return createEmpty(Collections.emptyMap());
  }

  @Override
  public JsonPipeline createEmpty(Map<String, String> contextProperties) {

    CaravanHttpRequest dummyRequest = new CaravanHttpRequestBuilder("").build();

    // make sure to set a Cache-Control header to mark the empty response as indefinitely cacheable,
    // otherwise the default max-age value of zero would become effective
    ImmutableListMultimap<String, String> headers = ImmutableListMultimap.of("Cache-Control", "max-age=" + Long.toString(TimeUnit.DAYS.toSeconds(365)));

    CaravanHttpResponse emptyJsonResponse = new CaravanHttpResponseBuilder()
    .status(200)
    .reason("OK")
    .headers(headers)
    .body("{}", Charset.forName("UTF-8"))
    .build();

    return new JsonPipelineImpl(dummyRequest, Observable.just(emptyJsonResponse),
        new JsonPipelineContextImpl(this, createMultiLayerCacheAdapter(), metricRegistry, contextProperties, serviceConfiguration));
  }

  MultiLayerCacheAdapter createMultiLayerCacheAdapter() {
    return new MultiLayerCacheAdapter(new ArrayList<CacheAdapter>(cacheAdapters.get()));
  }

  void bindCacheAdapter(CacheAdapter service, Map<String, Object> props) {
    cacheAdapters.bind(service, props);
  }

  void unbindCacheAdapter(CacheAdapter service, Map<String, Object> props) {
    cacheAdapters.unbind(service, props);
  }

}
