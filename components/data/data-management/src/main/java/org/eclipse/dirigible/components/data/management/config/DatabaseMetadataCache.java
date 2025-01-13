/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.config;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.commons.config.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * The Class CaffeineDatabaseMetadataCache.
 */
public class DatabaseMetadataCache {

    /** The Constant DIRIGIBLE_DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES. */
    private static final String DIRIGIBLE_DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES =
            "DIRIGIBLE_DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES";

    /** The cache. */
    private Cache<String, String> cache;

    /**
     * Instantiates a new caffeine database metadata cache.
     */
    public DatabaseMetadataCache() {
        initCache();
    }

    /**
     * Inits the cache.
     */
    private void initCache() {
        long timePolicy = Long.parseLong(Configuration.get(DIRIGIBLE_DATABASE_METADATA_CACHE_TIME_LIMIT_IN_MINUTES, "60"));
        cache = Caffeine.newBuilder()
                        .expireAfterAccess(timePolicy, TimeUnit.MINUTES)
                        .build();
    }

    /**
     * Gets the internal cache.
     *
     * @return the internal cache
     */
    public Cache<String, String> getInternalCache() {
        if (cache == null) {
            initCache();
        }

        return cache;
    }

    /**
     * Gets the.
     *
     * @param path the path
     * @return the String
     */
    public String get(String path) {
        return cache.getIfPresent(path);
    }

    /**
     * Put.
     *
     * @param path the path
     * @param content the content
     */
    public void put(String path, String content) {
        if (content != null) {
            cache.put(path, content);
        }
    }

    /**
     * Removes the.
     *
     * @param path the path
     */
    public void remove(String path) {
        cache.invalidate(path);
    }

    /**
     * Clear.
     */
    public void clear() {
        cache.invalidateAll();
    }

}
