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

import static org.junit.Assert.assertEquals;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.pipeline.JsonPipelineOutput;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import rx.Subscriber;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;


public class ResponseHandlingOperatorTest {

  @SuppressWarnings("unchecked")
  @Test
  public void test_HeadersCacheControlMaxAge() {
    ResponseHandlingOperator operator = new ResponseHandlingOperator("test-url");
    Subscriber subscriber = Mockito.mock(Subscriber.class);
    Subscriber<? super CaravanHttpResponse> operatorSubscriber = operator.call(subscriber);
    Multimap<String, String> headers = ImmutableListMultimap.of("Cache-Control", "max-age: 10");
    CaravanHttpResponse response = CaravanHttpResponse.create(200, "OK", headers, new byte[0]);
    operatorSubscriber.onNext(response);
    ArgumentCaptor<JsonPipelineOutput> captor = ArgumentCaptor.forClass(JsonPipelineOutput.class);
    Mockito.verify(subscriber).onNext(captor.capture());
    assertEquals(10, captor.getValue().getMaxAge());
  }
}
