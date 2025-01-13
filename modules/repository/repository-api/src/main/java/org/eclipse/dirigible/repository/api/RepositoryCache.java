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

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.commons.config.Configuration;

/**
 * The Class RepositoryCache.
 */
public class RepositoryCache implements IRepositoryCache {

    /** The cache. */
    private static IRepositoryCache cache;

    /** The Constant ENABLED. */
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    static {
        ServiceLoader<IRepositoryCache> services = ServiceLoader.load(IRepositoryCache.class);
        for (IRepositoryCache next : services) {
            cache = next;
            break;
        }
        ENABLED.set(Boolean.parseBoolean(Configuration.get(IRepository.DIRIGIBLE_REPOSITORY_CACHE_ENABLED, Boolean.FALSE.toString())));
    }

    /**
     * Gets the.
     *
     * @param path the path
     * @return the byte[]
     */
    @Override
    public byte[] get(String path) {
        if (ENABLED.get() && cache != null) {
            return cache.get(path);
        }
        return null;
    }

    /**
     * Put.
     *
     * @param path the path
     * @param content the content
     */
    @Override
    public void put(String path, byte[] content) {
        if (ENABLED.get() && cache != null) {
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
        if (ENABLED.get() && cache != null) {
            cache.remove(path);
        }
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        if (ENABLED.get() && cache != null) {
            cache.clear();
        }
    }

    /**
     * Enable.
     */
    public static void enable() {
        Configuration.set(IRepository.DIRIGIBLE_REPOSITORY_CACHE_ENABLED, Boolean.TRUE.toString());
        ENABLED.set(true);
    }

    /**
     * Disable.
     */
    public static void disable() {
        Configuration.set(IRepository.DIRIGIBLE_REPOSITORY_CACHE_ENABLED, Boolean.FALSE.toString());
        ENABLED.set(false);
        if (ENABLED.get() && cache != null) {
            cache.clear();
        }
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     */
    public static boolean isEnabled() {
        return ENABLED.get();
    }

}
