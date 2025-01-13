/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CacheFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheFacade.class);

    private static final Cache<String, Object> cache = Caffeine.newBuilder()
                                                               .expireAfterWrite(30, TimeUnit.MINUTES)
                                                               .maximumSize(1500)
                                                               .build();


    static class JsonHolder {

        private static final Gson GSON = new GsonBuilder().create();

        private final String json;

        JsonHolder(String json) {
            this.json = json;
        }

        Map<?, ?> getObject() {
            return GSON.fromJson(json, Map.class);
        }

        static JsonHolder toJsonHolder(Object object) {
            return new JsonHolder(GSON.toJson(object));
        }
    }

    public static boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Getter for the value of the property by its key.
     *
     * @param key the key
     * @return the string
     */
    public static Object get(String key) {
        Object cachedValue = cache.getIfPresent(key);

        if (cachedValue instanceof JsonHolder jsonHolder) {
            return jsonHolder.getObject();
        }
        return cachedValue;

    }

    /**
     * Set.
     *
     * @param key the path
     * @param content the content
     */
    public static void set(String key, Object content) {
        if (content != null) {
            Object cacheValue = shouldBeConverted(content) ? JsonHolder.toJsonHolder(content) : content;
            cache.put(key, cacheValue);
        }
    }

    private static boolean shouldBeConverted(Object value) {
        return null != value && !(value instanceof String || value instanceof Character || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Float
                || value instanceof Boolean);
    }

    /**
     * Deletes the instance.
     *
     * @param key the path
     */
    public static void delete(String key) {
        cache.invalidate(key);
    }

    /**
     * Clear.
     */
    public static void clear() {
        cache.invalidateAll();
    }
}
