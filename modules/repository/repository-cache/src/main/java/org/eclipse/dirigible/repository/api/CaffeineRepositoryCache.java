/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.commons.config.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * The Class CaffeineRepositoryCache.
 */
public class CaffeineRepositoryCache implements IRepositoryCache {

    /** The cache. */
    private static Cache<String, byte[]> cache;

    /**
     * Instantiates a new caffeine repository cache.
     */
    public CaffeineRepositoryCache() {
        initCache();
    }

    /**
     * Inits the cache.
     */
    private static void initCache() {
        long timePolicy = Long.parseLong(Configuration.get(IRepository.DIRIGIBLE_REPOSITORY_CACHE_TIME_LIMIT_IN_MINUTES, "10"));
        long sizePolicy = Long.parseLong(Configuration.get(IRepository.DIRIGIBLE_REPOSITORY_CACHE_SIZE_LIMIT_IN_MEGABYTES, "100"));
        cache = Caffeine.newBuilder()
                        .expireAfterAccess(timePolicy, TimeUnit.MINUTES)
                        .maximumWeight(sizePolicy * 1024 * 1024)
                        .weigher((String k, byte[] v) -> v.length)
                        .build();
    }

    /**
     * Gets the internal cache.
     *
     * @return the internal cache
     */
    public static Cache<String, byte[]> getInternalCache() {
        if (cache == null) {
            initCache();
        }

        return cache;
    }

    /**
     * Gets the.
     *
     * @param path the path
     * @return the byte[]
     */
    @Override
    public byte[] get(String path) {
        return cache.getIfPresent(path);
    }

    /**
     * Put.
     *
     * @param path the path
     * @param content the content
     */
    @Override
    public void put(String path, byte[] content) {
        if (content != null) {
            cache.put(path, content);
        }
    }

    /**
     * Removes the.
     *
     * @param path the path
     */
    @Override
    public void remove(String path) {
        cache.invalidate(path);
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        cache.invalidateAll();
    }

}
