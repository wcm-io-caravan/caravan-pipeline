package io.wcm.caravan.pipeline.extensions.halclient.action;

import io.wcm.caravan.commons.hal.resource.HalResource;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.pipeline.JsonPipelineAction;
import io.wcm.caravan.pipeline.JsonPipelineContext;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

/**
 * Removes all links for a HAL resource and it's embedded resources which don't fit the given relation names.
 */
public class RemoveAllLinks implements JsonPipelineAction {

  private final Set<String> relationsToIgnore;

  /**
   * @param relationsToIgnore Link relations not to remove
   */
  public RemoveAllLinks(String... relationsToIgnore) {
    this.relationsToIgnore = Sets.newHashSet(relationsToIgnore);
  }

  @Override
  public String getId() {
    return "REMOVE-LINKS(" + StringUtils.join(relationsToIgnore, '-') + ")";
  }

  @Override
  public Observable<JsonPipelineOutput> execute(JsonPipelineOutput previousStepOutput, JsonPipelineContext pipelineContext) {

    HalResource hal = new HalResource((ObjectNode)previousStepOutput.getPayload());
    removeLinksRecursive(hal);
    return Observable.just(previousStepOutput);

  }

  private void removeLinksRecursive(HalResource hal) {

    // remove links
    Streams.of(hal.getLinks().keySet())
        .filter(relation -> !relationsToIgnore.contains(relation))
        .forEach(relation -> hal.removeLinks(relation));
    // check embedded resources
    Streams.of(hal.getEmbedded().values())
        .forEach(embedded -> removeLinksRecursive(embedded));

  }

}
