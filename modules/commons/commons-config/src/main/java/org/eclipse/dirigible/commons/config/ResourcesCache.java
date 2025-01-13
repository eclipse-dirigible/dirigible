/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Class ResourcesCache.
 */
public class ResourcesCache {

    /** The Constant WEB_CACHE. */
    private static final Cache WEB_CACHE = new Cache();

    /** The Constant THEME_CACHE. */
    private static final Cache THEME_CACHE = new Cache();

    /**
     * Gets the web cache.
     *
     * @return the web cache
     */
    public static Cache getWebCache() {
        return WEB_CACHE;
    }

    /**
     * Gets the theme cache.
     *
     * @return the theme cache
     */
    public static Cache getThemeCache() {
        return THEME_CACHE;
    }

    /**
     * Clear.
     */
    public static void clear() {
        WEB_CACHE.clear();
        THEME_CACHE.clear();
    }

    /**
     * Instantiates a new resources cache.
     */
    private ResourcesCache() {

    }

    /**
     * The Class Cache.
     */
    public static class Cache {

        /** The Constant CACHE. */
        private static final Map<String, String> CACHE = Collections.synchronizedMap(new HashMap<String, String>());

        /**
         * Instantiates a new cache.
         */
        private Cache() {

        }

        /**
         * Gets the tag.
         *
         * @param id the id
         * @return the tag
         */
        public String getTag(String id) {
            return CACHE.get(id);
        }

        /**
         * Sets the tag.
         *
         * @param id the id
         * @param tag the tag
         */
        public void setTag(String id, String tag) {
            CACHE.put(id, tag);
        }

        /**
         * Generate tag.
         *
         * @return the string
         */
        public String generateTag() {
            return UUID.randomUUID()
                       .toString();
        }

        /**
         * Clear.
         */
        public void clear() {
            CACHE.clear();
        }
    }
}
