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

import io.wcm.caravan.io.http.ResilientHttp;
import io.wcm.caravan.io.http.request.Request;
import io.wcm.caravan.io.http.request.RequestTemplate;
import io.wcm.caravan.io.http.response.Response;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import rx.Observable;

/**
 * Default implementation of {@link JsonPipelineFactory}.
 */
@Component
@Service(JsonPipelineFactory.class)
public final class JsonPipelineFactoryImpl implements JsonPipelineFactory {

  @Reference
  private ResilientHttp transport;

  @Reference
  private CacheAdapter cacheAdapter;

  @Override
  public JsonPipeline create(String serviceName, Request request) {

    // note that #execute will *not* actually start the request, but just create an observable that will initiate
    // the request when #subscribe is called on the pipeline's output observable
    Observable<Response> response = transport.execute(serviceName, request);

    return new JsonPipelineImpl(serviceName, request, response, cacheAdapter);
  }

  @Override
  public JsonPipeline createEmpty() {

    Request dummyRequest = new RequestTemplate().request();
    Response emptyJsonResponse = Response.create(200, "Ok", new HashMap<String, Collection<String>>(), "{}", Charset.forName("UTF-8"));

    return new JsonPipelineImpl("", dummyRequest, Observable.just(emptyJsonResponse), cacheAdapter);
  }

}
