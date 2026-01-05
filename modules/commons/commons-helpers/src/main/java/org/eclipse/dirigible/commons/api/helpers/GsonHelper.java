/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.lang.reflect.Type;

/**
 * The GsonHelper utility class.
 */
public class GsonHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonHelper.class);

    /** The GSON instance. */
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                                                      .setPrettyPrinting()
                                                      .create();

    /**
     * To json.
     *
     * @param src the src
     * @return the string
     */
    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    /**
     * To json.
     *
     * @param <T> the generic type
     * @param src the src
     * @param classOfT the class of T
     * @return the string
     */
    public static <T> String toJson(Object src, Class<T> classOfT) {
        return GSON.toJson(src);
    }

    /**
     * From json.
     *
     * @param <T> the generic type
     * @param src the src
     * @param classOfT the class of T
     * @return the t
     */
    public static <T> T fromJson(String src, Class<T> classOfT) {
        return GSON.fromJson(src, classOfT);
    }

    public static <T> T fromJson(String src, TypeToken<T> classOfT) {
        return GSON.fromJson(src, classOfT);
    }

    /**
     * From json.
     *
     * @param <T> the generic type
     * @param src the src
     * @param type the type
     * @return the t
     */
    public static <T> T fromJson(InputStreamReader src, Type type) {
        return GSON.fromJson(src, type);
    }

    /**
     * From json.
     *
     * @param <T> the generic type
     * @param src the src
     * @param type the type
     * @return the t
     */
    public static <T> T fromJson(String src, Type type) {
        return GSON.fromJson(src, type);
    }

    /**
     * To json tree.
     *
     * @param value the value
     * @return the json element
     */
    public static JsonElement toJsonTree(Object value) {
        return GSON.toJsonTree(value);
    }

    /**
     * Parses the json.
     *
     * @param src the src
     * @return the json element
     * @throws JsonSyntaxException in case of invalid json
     */
    public static JsonElement parseJson(String src) throws JsonSyntaxException {
        try {
            LOGGER.debug("Parsing:\n{}", src);
            return JsonParser.parseString(src);
        } catch (JsonSyntaxException ex) {
            throw new JsonSyntaxException("Invalid json content [" + src + "]", ex);
        }
    }

}
