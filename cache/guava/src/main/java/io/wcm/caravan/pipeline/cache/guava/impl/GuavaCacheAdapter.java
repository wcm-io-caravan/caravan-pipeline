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
package io.wcm.caravan.pipeline.cache.guava.impl;

import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.CharEncoding;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import rx.Observable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * {@link CacheAdapter} implementation for Guava.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Guava",
description = "Configure pipeline caching in guava.")
@Service(CacheAdapter.class)
public class GuavaCacheAdapter implements CacheAdapter {

  private static final int MAX_CACHE_KEY_LENGTH = 250;

  private Cache<String, String> guavaCache;

  /**
   * Default constructor
   */
  public GuavaCacheAdapter() {
    this.guavaCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build();
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {

    String cacheKey = servicePrefix + descriptor;
    if (cacheKey.length() < MAX_CACHE_KEY_LENGTH) {
      return cacheKey;
    }

    int charactersToKeep = MAX_CACHE_KEY_LENGTH - servicePrefix.length() - 41;
    String toKeep = descriptor.substring(0, charactersToKeep);
    String toHash = descriptor.substring(charactersToKeep);
    String hash = calculateHash(toHash);

    return servicePrefix + toKeep + "#" + hash;
  }

  @Override
  public Observable<String> get(String cacheKey, boolean extendExpiry, int expirySeconds) {

    Observable<String> entryObservable = Observable.create(subscriber -> {
      subscriber.onNext(guavaCache.getIfPresent(cacheKey));
      subscriber.onCompleted();
    });

    return entryObservable;
  }

  @Override
  public void put(String cacheKey, String jsonString, int expirySeconds) {
    guavaCache.put(cacheKey, jsonString);
  }

  private static String calculateHash(String message) {
    try {

      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(message.getBytes(CharEncoding.UTF_8));
      byte[] digestBytes = digest.digest();

      return javax.xml.bind.DatatypeConverter.printHexBinary(digestBytes).toLowerCase();

    }
    catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
      throw new RuntimeException("Failed to create sha1 Hash from " + message, ex);
    }
  }

}
