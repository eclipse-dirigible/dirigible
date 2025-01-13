/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.helpers;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The GsonHelper utility class.
 */
public class JsonHelper {

    /** The GSON instance. */
    private static final transient Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                                                                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
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
        try {
            return GSON.fromJson(src, classOfT);
        } catch (JsonSyntaxException ex) {
            throw new JsonSyntaxException("Failed to deserialize json [" + src + "] to " + classOfT.getCanonicalName(), ex);
        }
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
     */
    public static JsonElement parseJson(String src) {
        return JsonParser.parseString(src);
    }

}
