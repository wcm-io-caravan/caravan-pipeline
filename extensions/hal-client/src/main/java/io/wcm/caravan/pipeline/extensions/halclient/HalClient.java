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
package io.wcm.caravan.pipeline.extensions.halclient;

import io.wcm.caravan.commons.hal.HalBuilder;
import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.extensions.halclient.action.BuildResource;
import io.wcm.caravan.pipeline.extensions.halclient.action.ModifyResource;
import io.wcm.caravan.pipeline.extensions.halclient.action.DeepEmbedLinks;
import io.wcm.caravan.pipeline.extensions.halclient.action.EmbedLink;
import io.wcm.caravan.pipeline.extensions.halclient.action.EmbedLinks;
import io.wcm.caravan.pipeline.extensions.halclient.action.FollowLink;

import java.util.Collections;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import rx.functions.Action1;
import rx.functions.Func2;

import com.google.common.collect.ImmutableMap;

/**
 * Factory for HAL specific {@link JsonPipelineAction}s.
 */
@ProviderType
public final class HalClient {

  private final String serviceName;
  private final CacheStrategy cacheStrategy;
  private final Map<String, String> contextProperties;

  private final CaravanHttpRequest entryPointRequest;

  /**
   * @param serviceName Service name
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   */
  public HalClient(String serviceName, CacheStrategy cacheStrategy) {
    this(serviceName, cacheStrategy, ImmutableMap.of());
  }

  /**
   * @param serviceName Service name
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   * @param contextProperties a Map of properties to pass on to {@link JsonPipelineFactory#create(CaravanHttpRequest, Map)}
   */
  public HalClient(String serviceName, CacheStrategy cacheStrategy, Map<String, String> contextProperties) {
    this.serviceName = serviceName;
    this.cacheStrategy = cacheStrategy;
    this.contextProperties = contextProperties;
    this.entryPointRequest = new CaravanHttpRequestBuilder(serviceName).append("/").build();
  }

  /**
   * @param entryPointRequest the request to be executed to fetch the HAL entry point
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   * @param contextProperties a Map of properties to pass on to {@link JsonPipelineFactory#create(CaravanHttpRequest, Map)}
   */
  public HalClient(CaravanHttpRequest entryPointRequest, CacheStrategy cacheStrategy, Map<String, String> contextProperties) {
    this.serviceName = entryPointRequest.getServiceName();
    this.cacheStrategy = cacheStrategy;
    this.contextProperties = contextProperties;
    this.entryPointRequest = entryPointRequest;
  }

  /**
   * Creates a {@link JsonPipeline} that will fetch the entry point of the HAL service
   * @param factory the factory to use to create the pipeline
   * @return the pipeline
   */
  public JsonPipeline getEntryPoint(JsonPipelineFactory factory) {
    return create(factory, entryPointRequest);
  }

  /**
   * Creates a JSON pipeline for a service entry point.
   * @deprecated use {@link #getEntryPoint(JsonPipelineFactory)} instead
   * @param factory Pipeline factory
   * @return JSON pipeline
   */
  @Deprecated
  public JsonPipeline createEntryPoint(JsonPipelineFactory factory) {
    return create(factory, "/");
  }

  /**
   * Creates a JSON pipeline for a service and URL.
   * @deprecated specify the entry point in the constructor and use {@link #getEntryPoint(JsonPipelineFactory)} instead
   * @param factory Pipeline factory
   * @param url URL to start
   * @return JSON pipeline
   */
  @Deprecated
  public JsonPipeline create(JsonPipelineFactory factory, String url) {
    return create(factory, new CaravanHttpRequestBuilder(serviceName).append(url).build());
  }

  /**
   * Creates a JSON pipeline for a HTTP request.
   * @deprecated specify the entry point in the constructor and use {@link #getEntryPoint(JsonPipelineFactory)} instead
   * @param factory Pipeline factory
   * @param request Pre-configured HTTP request
   * @return JSON pipeline
   */
  @Deprecated
  public JsonPipeline create(JsonPipelineFactory factory, CaravanHttpRequest request) {
    JsonPipeline entryPoint = factory.create(request, contextProperties);
    if (cacheStrategy != null) {
      entryPoint = entryPoint.addCachePoint(cacheStrategy);
    }
    return entryPoint;
  }

  /**
   * Creates a follow link action for the first relation specific link
   * @param relation Link relation
   * @return Follow link action
   */
  public FollowLink follow(String relation) {
    return follow(relation, Collections.emptyMap(), 0);
  }

  /**
   * Creates a follow link action for the first relation specific link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Follow link action
   */
  public FollowLink follow(String relation, Map<String, Object> parameters) {
    return follow(relation, parameters, 0);
  }

  /**
   * Creates a follow link action for the {@code index} specified link.
   * @param relation Link relation
   * @param index Link index
   * @return Follow link action
   */
  public FollowLink follow(String relation, int index) {
    return follow(relation, Collections.emptyMap(), index);
  }

  /**
   * Creates a follow link action for the {@code index} specified link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @param index Link index
   * @return Follow link action
   */
  public FollowLink follow(String relation, Map<String, Object> parameters, int index) {
    return new FollowLink(serviceName, relation, index, parameters).setCacheStrategy(cacheStrategy);
  }

  /**
   * Creates an embed links action for all relation specific links.
   * @param relation Link relation
   * @return Embed links action
   */
  public EmbedLinks embed(String relation) {
    return embed(relation, Collections.emptyMap());
  }

  /**
   * Creates an embed links action for all relation specific links with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Embed links action
   */
  public EmbedLinks embed(String relation, Map<String, Object> parameters) {
    return new EmbedLinks(serviceName, relation, parameters).setCacheStrategy(cacheStrategy);
  }

  /**
   * Creates an embed link action for the {@code index} specified link.
   * @param relation Link relation
   * @param index Link index
   * @return Embed links action
   */
  public EmbedLink embed(String relation, int index) {
    return embed(relation, Collections.emptyMap(), index);
  }

  /**
   * Creates an embed link action for the {@code index} specified link with the given URL parameters.
   * @param relation Link relation
   * @param parameters URL parameters
   * @param index Link index
   * @return Embed links action
   */
  public EmbedLink embed(String relation, Map<String, Object> parameters, int index) {
    return new EmbedLink(serviceName, relation, index, parameters).setCacheStrategy(cacheStrategy);
  }

  /**
   * Fetches the content of all links with the given relation in a HAL resource <strong>and all embedded resources</strong>, and replaces the links with the
   * corresponding embedded resources.
   * @param relation Link relation
   * @return Deep Embed links action
   */
  public DeepEmbedLinks deepEmbed(String relation) {
    return deepEmbed(relation, Collections.emptyMap());
  }

  /**
   * Fetches the content of all links with the given relation in a HAL resource <strong>and all embedded resources</strong>, and replaces the links with the
   * corresponding embedded resources.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Deep Embed links action
   */
  public DeepEmbedLinks deepEmbed(String relation, Map<String, Object> parameters) {
    return new DeepEmbedLinks(serviceName, relation, parameters).setCacheStrategy(cacheStrategy);
  }

  /**
   * Allows to create a {@link BuildResource} action by specifying the output href and a lambda
   * @param selfHref the path of the output resource to be used for the self helf
   * @param buildFunc the lambda that gets the previous step's output and a {@link HalBuilder} with the specified self-link
   * @return the action that executes the lambda build function
   **/
  public BuildResource buildResource(String selfHref, Func2<HalResource, HalBuilder, HalResource> buildFunc) {
    return new BuildResource(selfHref) {

      @Override
      public HalResource build(HalResource input, HalBuilder outputBuilder) {
        return buildFunc.call(input, outputBuilder);
      }
    };
  }

  /**
   * Allows to create a {@link ModifyResource} action by specifying the output href and a lambda
   * @param selfHref the path of the output resource to be used for the self helf
   * @param modifyFunc the lambda that gets a HalResource with the previous step's output and the specified self-link
   * @return the action that executes the lambda build function
   **/
  public ModifyResource modifyResource(String selfHref, Action1<HalResource> modifyFunc) {
    return new ModifyResource(selfHref) {

      @Override
      public void modify(HalResource output) {
        modifyFunc.call(output);
      }
    };
  }

}
