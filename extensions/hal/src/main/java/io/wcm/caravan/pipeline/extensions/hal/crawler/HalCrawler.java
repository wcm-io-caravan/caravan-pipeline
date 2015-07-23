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

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategy;
import io.wcm.caravan.pipeline.extensions.hal.client.HalClient;
import io.wcm.caravan.pipeline.extensions.hal.client.action.LoadLink;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

import rx.Observable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

/**
 * Crawler walking on a HAL resource graph.
 */
@ProviderType
public final class HalCrawler implements JsonPipelineAction {

  /**
   * HTTP header for HAL link relation
   */
  public static final String HEADER_CRAWLER_RELATION = "Caravan-Crawler-HAL-Link-Relation";

  private final Set<String> startedUrls = Sets.newConcurrentHashSet();
  private final Set<String> processedUrls = Sets.newConcurrentHashSet();

  private final HalClient client;
  private final LinkExtractor linkExtractor;
  private final UriParametersProvider uriParametersProvider;
  private final OutputProcessor outputProcessor;

  private CacheStrategy cacheStrategy;

  /**
   * @param client HAL client
   * @param linkExtractor Link extractor
   * @param uriParametersProvider URI parameter provider
   * @param outputProcessor Output processor
   */
  public HalCrawler(HalClient client, LinkExtractor linkExtractor, UriParametersProvider uriParametersProvider, OutputProcessor outputProcessor) {
    this.client = client;
    this.linkExtractor = linkExtractor;
    this.uriParametersProvider = uriParametersProvider;
    this.outputProcessor = outputProcessor;
  }

  /**
   * @param strategy The cacheStrategy to set.
   * @return This crawler
   */
  public HalCrawler setCacheStrategy(CacheStrategy strategy) {
    this.cacheStrategy = strategy;
    return this;
  }

  @Override
  public String getId() {
    return "HAL-CRAWLER-(" + linkExtractor.getId() + '-' + uriParametersProvider.getId() + '-' + outputProcessor.getId() + ')';
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    String currentUrl = getCurrentUrl(previousStepOutput);
    processedUrls.add(currentUrl);
    JsonPipeline startingPipeline = pipelineContext.getFactory().createEmpty(pipelineContext.getProperties());

    HalResource currentHalResource = getCurrentHalResource(previousStepOutput, currentUrl);
    ListMultimap<String, Link> links = linkExtractor.extract(currentHalResource);

    return Observable.from(links.entries())
        // create pipeline action
        .map(entry -> {
          String relation = entry.getKey();
          Link link = entry.getValue();
          Map<String, Object> parameters = uriParametersProvider.getParameters(currentHalResource, relation, link);
          LoadLink action = client.load(link, parameters);
          action.setHttpHeaders(ImmutableMultimap.of(HEADER_CRAWLER_RELATION, relation));
          if (cacheStrategy != null) {
            action.setCacheStrategy(cacheStrategy);
          }
          return action;
        })
        // filter unique by URL
        .distinct(action -> action.getUrl())
        // filter already processed URLs
        .filter(action -> !startedUrls.contains(action.getUrl()) && !processedUrls.contains(action.getUrl()))
        // add URL to processed and create pipeline
        .map(action -> {
          startedUrls.add(action.getUrl());
          return startingPipeline.applyAction(action);
        })
        // add this action to the pipeline
        .map(pipeline -> pipeline.applyAction(this))
        // get pipeline outputs
        .flatMap(JsonPipeline::getOutput)
        // get pipeline outputs list
        .toList()
        // process output
        .map(linkOutputs -> outputProcessor.process(previousStepOutput, linkOutputs));

  }

  private String getCurrentUrl(JsonPipelineOutput output) {

    List<CaravanHttpRequest> requests = output.getRequests();
    if (requests.isEmpty()) {
      return "unknown";
    }
    CaravanHttpRequest request = requests.get(requests.size() - 1);
    return request.getUrl();

  }

  private HalResource getCurrentHalResource(JsonPipelineOutput previousStepOutput, String currentUrl) {

    JsonNode json = previousStepOutput.getPayload();
    if (!(json instanceof ObjectNode)) {
      HalResource hal = HalResourceFactory.createResource(currentUrl);
      hal.getModel().set("content", json);
      return hal;
    }
    return new HalResource((ObjectNode)json);

  }

}
