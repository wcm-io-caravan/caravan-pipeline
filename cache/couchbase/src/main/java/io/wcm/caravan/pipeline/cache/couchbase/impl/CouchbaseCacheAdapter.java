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
package io.wcm.caravan.pipeline.cache.couchbase.impl;

import io.wcm.caravan.commons.couchbase.CouchbaseClientProvider;
import io.wcm.caravan.pipeline.cache.spi.CacheAdapter;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.commons.lang3.CharEncoding;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.RawJsonDocument;

/**
 * {@link CacheAdapter} implementation for Couchbase.
 */
@Component(immediate = true, metatype = true,
label = "wcm.io Caravan Pipeline Cache Adapter for Couchbase",
description = "Configure pipeline caching in couchbase.")
@Service(CacheAdapter.class)
public class CouchbaseCacheAdapter implements CacheAdapter {

  @Property(label = "Cache Key Prefix", description = "Prefix for caching keys.")
  static final String CACHE_KEY_PREFIX_PROPERTY = "cacheKeyPrefix";
  private static final String CACHE_KEY_PREFIX_DEFAULT = "json-pipeline:";

  private static final int MAX_CACHE_KEY_LENGTH = 250;

  private static final Logger log = LoggerFactory.getLogger(CouchbaseCacheAdapter.class);

  @Reference
  private CouchbaseClientProvider couchbaseClientProvider;

  private String keyPrefix;

  @Activate
  private void activate(Map<String, Object> config) {
    keyPrefix = PropertiesUtil.toString(config.get(CACHE_KEY_PREFIX_PROPERTY), CACHE_KEY_PREFIX_DEFAULT);
  }

  @Override
  public String getCacheKey(String servicePrefix, String descriptor) {
    String prefix = keyPrefix + servicePrefix + ":";

    String cacheKey = prefix + descriptor;
    if (cacheKey.length() < MAX_CACHE_KEY_LENGTH) {
      return cacheKey;
    }

    int charactersToKeep = MAX_CACHE_KEY_LENGTH - prefix.length() - 41;

    String toKeep = descriptor.substring(0, charactersToKeep);
    String toHash = descriptor.substring(charactersToKeep);

    String hash = calculateHash(toHash);

    return prefix + toKeep + "#" + hash;
  }


  @Override
  public Observable<String> get(String cacheKey, boolean extendExpiry, int expirySeconds) {
    AsyncBucket bucket = couchbaseClientProvider.getCacheBucket();

    Observable<RawJsonDocument> fromCache;
    if (extendExpiry) {
      fromCache = bucket.getAndTouch(cacheKey, expirySeconds, RawJsonDocument.class);
    }
    else {
      fromCache = bucket.get(cacheKey, RawJsonDocument.class);
    }

    return fromCache.map(doc -> {
      String content = doc.content();

      log.trace("Succesfully retrieved document with id {}: {}", doc.id(), doc.content());

      return content;

    });
  }

  @Override
  public void put(String cacheKey, String jsonString, int expirySeconds) {
    AsyncBucket bucket = couchbaseClientProvider.getCacheBucket();

    RawJsonDocument doc = RawJsonDocument.create(cacheKey, expirySeconds, jsonString);
    Observable<RawJsonDocument> insertionObservable = bucket.upsert(doc);

    insertionObservable.subscribe(new Observer<RawJsonDocument>() {

      @Override
      public void onNext(RawJsonDocument insertedDoc) {
        log.trace("Document {} has been succesfully put into the Couchbase cache:\n {}", insertedDoc.id(), insertedDoc.content());
      }

      @Override
      public void onCompleted() {
        // nothing
      }

      @Override
      public void onError(Throwable e) {
        log.error("Failed to put document " + cacheKey + " into the Couchbase cache", e);
      }

    });
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
