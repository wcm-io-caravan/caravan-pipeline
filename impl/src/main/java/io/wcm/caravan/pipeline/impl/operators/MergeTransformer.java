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
package io.wcm.caravan.pipeline.impl.operators;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.JsonPipelineOutputException;

import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * a transformer that merges the output of a secondary {@link JsonPipeline} into the output of the primary pipeline.
 */
public class MergeTransformer implements Transformer<JsonPipelineOutput, JsonPipelineOutput> {

  private static final Logger log = LoggerFactory.getLogger(MergeTransformer.class);

  private final String primaryDescriptor;
  private final Observable<JsonPipelineOutput> secondaryOutput;
  private final String targetProperty;

  /**
   * @param primaryDescriptor the descriptor of the primary pipeline
   * @param secondaryOutput the observable that emits the output of the secondary pipeline
   * @param targetProperty the property that will be added to the primary pipeline'S output
   */
  public MergeTransformer(String primaryDescriptor, Observable<JsonPipelineOutput> secondaryOutput, String targetProperty) {
    this.primaryDescriptor = primaryDescriptor;
    this.secondaryOutput = secondaryOutput;
    this.targetProperty = targetProperty;
  }

  @Override
  public Observable<JsonPipelineOutput> call(Observable<JsonPipelineOutput> primaryOutput) {
    return primaryOutput.zipWith(secondaryOutput, (primaryModel, secondaryModel) -> {

      log.debug("zipping object from secondary source into target property " + targetProperty);

      JsonNode jsonFromPrimary = primaryModel.getPayload();
      JsonNode jsonFromSecondary = secondaryModel.getPayload();

      if (!(jsonFromPrimary.isObject())) {
        throw new JsonPipelineOutputException("Only pipelines with JSON *Objects* can be used as a target for a merge operation, but response data for "
            + primaryDescriptor + " contained " + jsonFromPrimary.getClass().getSimpleName());
      }

      // start with cloning the the response of the primary pipeline
      ObjectNode mergedObject = jsonFromPrimary.deepCopy();

      // if a target property is specified, the JSON to be merged is inserted into this property
      if (isNotBlank(targetProperty)) {

        if (!mergedObject.has(targetProperty)) {
          // the target property does not exist yet, so we just can set the property
          mergedObject.set(targetProperty, jsonFromSecondary);
        }
        else {

          // the target property already exists - let's hope we can merge!
          JsonNode targetNode = mergedObject.get(targetProperty);

          if (!targetNode.isObject()) {
            throw new JsonPipelineOutputException("When merging two pipelines into the same target property, both most contain JSON *Object* responses");
          }

          if (!(jsonFromSecondary.isObject())) {
            throw new JsonPipelineOutputException("Only pipelines with JSON *Object* responses can be merged into an existing target property");
          }

          mergeAllPropertiesInto((ObjectNode)jsonFromSecondary, (ObjectNode)targetNode);
        }
      }
      else {

        // if no target property is specified, all properties of the secondary pipeline are copied into the merged object
        if (!(jsonFromSecondary.isObject())) {
          throw new JsonPipelineOutputException("Only pipelines with JSON *Object* responses can be merged without specify a target property");
        }

        mergeAllPropertiesInto((ObjectNode)jsonFromSecondary, mergedObject);
      }

      return primaryModel.withPayload(mergedObject).withMaxAge(Math.min(primaryModel.getMaxAge(), secondaryModel.getMaxAge()));
    });
  }

  private void mergeAllPropertiesInto(ObjectNode nodeToMerge, ObjectNode targetNode) {

    // iterate over all properties of the given node
    Iterator<Entry<String, JsonNode>> propertyIterator = nodeToMerge.fields();
    while (propertyIterator.hasNext()) {
      Entry<String, JsonNode> nextProperty = propertyIterator.next();
      String propertyName = nextProperty.getKey();

      if (targetNode.has(propertyName)) {
        // what to do if the property already exists? for now, just throw an exception,
        throw new JsonPipelineOutputException("Target pipeline " + primaryDescriptor + " already has a property named " + propertyName);
      }

      targetNode.set(propertyName, nextProperty.getValue());
    }
  }
}
