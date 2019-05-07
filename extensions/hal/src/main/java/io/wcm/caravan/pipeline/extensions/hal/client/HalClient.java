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
package io.wcm.caravan.pipeline.extensions.hal.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.hal.resource.util.HalBuilder;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineExceptionHandler;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.extensions.hal.action.BuildResource;
import io.wcm.caravan.pipeline.extensions.hal.action.CreateResource;
import io.wcm.caravan.pipeline.extensions.hal.action.ModifyResource;
import io.wcm.caravan.pipeline.extensions.hal.client.action.DeepEmbedLinks;
import io.wcm.caravan.pipeline.extensions.hal.client.action.EmbedLink;
import io.wcm.caravan.pipeline.extensions.hal.client.action.EmbedLinks;
import io.wcm.caravan.pipeline.extensions.hal.client.action.FollowLink;
import io.wcm.caravan.pipeline.extensions.hal.client.action.LoadLink;
import rx.functions.Action1;
import rx.functions.Func2;

/**
 * Factory for HAL specific {@link JsonPipelineAction}s.
 */
@ProviderType
public final class HalClient {

  private ServiceIdExtractor serviceIdExtractor;
  private final CacheStrategy cacheStrategy;
  private final Map<String, String> contextProperties;

  private final CaravanHttpRequest entryPointRequest;

  private List<JsonPipelineExceptionHandler> exceptionHandlers = Lists.newArrayList();
  private Logger logger;

  /**
   * @param serviceId Service ID
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   */
  public HalClient(String serviceId, CacheStrategy cacheStrategy) {
    this(serviceId, cacheStrategy, ImmutableMap.of());
  }

  /**
   * @param serviceId Service ID
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   * @param contextProperties a Map of properties to pass on to
   *          {@link JsonPipelineFactory#create(CaravanHttpRequest, Map)}
   */
  public HalClient(String serviceId, CacheStrategy cacheStrategy, Map<String, String> contextProperties) {
    this.serviceIdExtractor = (path) -> serviceId;
    this.cacheStrategy = cacheStrategy;
    this.contextProperties = contextProperties;
    this.entryPointRequest = new CaravanHttpRequestBuilder(serviceId).append("/").build();
  }

  /**
   * @param entryPointRequest the request to be executed to fetch the HAL entry point
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   * @param contextProperties a Map of properties to pass on to
   *          {@link JsonPipelineFactory#create(CaravanHttpRequest, Map)}
   */
  public HalClient(CaravanHttpRequest entryPointRequest, CacheStrategy cacheStrategy, Map<String, String> contextProperties) {
    this.serviceIdExtractor = (path) -> entryPointRequest.getServiceId();
    this.cacheStrategy = cacheStrategy;
    this.contextProperties = contextProperties;
    this.entryPointRequest = entryPointRequest;
  }

  /**
   * @param entryPointRequest the request to be executed to fetch the HAL entry point
   * @param serviceIdExtractor Service ID extractor
   * @param cacheStrategy default cache strategy to use for all actions that fetch additional resources
   * @param contextProperties a Map of properties to pass on to
   *          {@link JsonPipelineFactory#create(CaravanHttpRequest, Map)}
   */
  public HalClient(CaravanHttpRequest entryPointRequest, ServiceIdExtractor serviceIdExtractor, CacheStrategy cacheStrategy,
      Map<String, String> contextProperties) {
    this.serviceIdExtractor = (path) -> entryPointRequest.getServiceId();
    this.cacheStrategy = cacheStrategy;
    this.contextProperties = contextProperties;
    this.entryPointRequest = entryPointRequest;
  }

  /**
   * Adds an exception handler for the pipeline actions.
   * @param newExceptionHandler The exceptionHandler to set.
   * @return This HAL client
   */
  public HalClient addExceptionHandler(JsonPipelineExceptionHandler newExceptionHandler) {
    this.exceptionHandlers.add(newExceptionHandler);
    return this;
  }

  /**
   * Sets the logger for the pipeline actions.
   * @param value Logger to set.
   * @return This HAL client
   */
  public HalClient setLogger(Logger value) {
    this.logger = value;
    return this;
  }

  /**
   * Replaces the default service id extractor (that uses the same serviceId for all requests) with a custom logic
   * @param extractor the ServiceIdExtractor to use
   * @return this hal client
   */
  public HalClient setServiceIdExtractor(ServiceIdExtractor extractor) {
    this.serviceIdExtractor = extractor;
    return this;
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
    return create(factory, new CaravanHttpRequestBuilder(serviceIdExtractor.getServiceId(url)).append(url).build());
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
    return (FollowLink)new FollowLink(serviceIdExtractor, relation, index, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
  }

  /**
   * Creates a follow link action for the forst link with the given relation and name
   * @param relation Link relation
   * @param parameters URL parameters
   * @param name of the link to follow
   * @return Follow link action
   */
  public FollowLink follow(String relation, Map<String, Object> parameters, String name) {
    return (FollowLink)new FollowLink(serviceIdExtractor, relation, name, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
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
    return (EmbedLinks)new EmbedLinks(serviceIdExtractor, relation, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
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
    return (EmbedLink)new EmbedLink(serviceIdExtractor, relation, index, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
  }

  /**
   * Fetches the content of all links with the given relation in a HAL resource <strong>and all embedded
   * resources</strong>, and replaces the links with the
   * corresponding embedded resources.
   * @param relation Link relation
   * @return Deep Embed links action
   */
  public DeepEmbedLinks deepEmbed(String relation) {
    return deepEmbed(relation, Collections.emptyMap());
  }

  /**
   * Fetches the content of all links with the given relation in a HAL resource <strong>and all embedded
   * resources</strong>, and replaces the links with the
   * corresponding embedded resources.
   * @param relation Link relation
   * @param parameters URL parameters
   * @return Deep Embed links action
   */
  public DeepEmbedLinks deepEmbed(String relation, Map<String, Object> parameters) {
    return (DeepEmbedLinks)new DeepEmbedLinks(serviceIdExtractor, relation, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
  }

  /**
   * Allows to create a {@link BuildResource} action by specifying the output href and a lambda
   * @param selfHref the path of the output resource to be used for the self helf
   * @param buildFunc the lambda that gets the previous step's output and a {@link HalBuilder} with the specified
   *          self-link
   * @return the action that executes the lambda build function
   * @deprecated use createResource instead
   **/
  @Deprecated
  public BuildResource buildResource(String selfHref, Func2<HalResource, HalBuilder, HalResource> buildFunc) {
    return new BuildResource(selfHref) {

      @Override
      public HalResource build(HalResource input, HalBuilder outputBuilder) {
        return buildFunc.call(input, outputBuilder);
      }
    };
  }

  /**
   * Allows to create a {@link CreateResource} action by specifying the output href and a lambda.
   * The {@link CreateResource} action will automatically generated a cache-key based on the given href, and also
   * set the self-link of the resource returned by the lambda
   * @param selfHref the path of the output resource to be used for the self helf
   * @param buildFunc the lambda that gets the previous step's output and should return a new HalResource
   * @return the action that executes the lambda build function
   **/
  public CreateResource createResource(String selfHref, Function<HalResource, HalResource> buildFunc) {
    return new CreateResource(selfHref) {

      @Override
      public HalResource createOutput(HalResource input) {
        return buildFunc.apply(input);
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

  /**
   * Creates a {@link LoadLink} action for the given link.
   * @param link Link to load
   * @return Load link action
   */
  public LoadLink load(Link link) {
    return load(link, Collections.emptyMap());
  }

  /**
   * Creates a {@link LoadLink} action for the given link and URI parameters.
   * @param link Link to load
   * @param parameters URI parameters
   * @return Load link action
   */
  public LoadLink load(Link link, Map<String, Object> parameters) {
    return (LoadLink)new LoadLink(serviceIdExtractor, link, parameters)
        .setCacheStrategy(cacheStrategy)
        .setExceptionHandlers(exceptionHandlers)
        .setLogger(logger);
  }

}
