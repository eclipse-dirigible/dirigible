/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * JSON (de)serialization for client Java code. A thin wrapper over a {@link Gson} instance
 * pre-configured to handle the {@code java.time} types the generated entities use
 * ({@link LocalDate}, {@link LocalDateTime}, {@link LocalTime}), serializing them as ISO-8601
 * strings.
 * <p>
 * A bare {@code new Gson()} cannot serialize these on JDK 17+: Gson falls back to field reflection
 * and {@code java.base} does not open {@code java.time} to other modules, so the call fails with
 * {@code InaccessibleObjectException}. Use this helper (instead of constructing {@code Gson}
 * directly) whenever entity instances - which routinely carry date/time fields - are involved, e.g.
 * publishing an entity to a message topic or parsing one back.
 */
public final class Json {

    private static final Gson GSON = new GsonBuilder()
                                                      .registerTypeAdapter(LocalDate.class,
                                                              (JsonSerializer<LocalDate>) (value, type,
                                                                      context) -> new JsonPrimitive(value.toString()))
                                                      .registerTypeAdapter(LocalDate.class,
                                                              (JsonDeserializer<LocalDate>) (json, type,
                                                                      context) -> LocalDate.parse(json.getAsString()))
                                                      .registerTypeAdapter(LocalDateTime.class,
                                                              (JsonSerializer<LocalDateTime>) (value, type,
                                                                      context) -> new JsonPrimitive(value.toString()))
                                                      .registerTypeAdapter(LocalDateTime.class,
                                                              (JsonDeserializer<LocalDateTime>) (json, type,
                                                                      context) -> LocalDateTime.parse(json.getAsString()))
                                                      .registerTypeAdapter(LocalTime.class,
                                                              (JsonSerializer<LocalTime>) (value, type,
                                                                      context) -> new JsonPrimitive(value.toString()))
                                                      .registerTypeAdapter(LocalTime.class,
                                                              (JsonDeserializer<LocalTime>) (json, type,
                                                                      context) -> LocalTime.parse(json.getAsString()))
                                                      .create();

    private Json() {}

    /**
     * Serialize a value to its JSON representation.
     *
     * @param value the value (entity, map, collection, ...)
     * @return the JSON string
     */
    public static String stringify(Object value) {
        return GSON.toJson(value);
    }

    /**
     * Deserialize JSON into an instance of the given type.
     *
     * @param json the JSON string
     * @param type the target type
     * @param <T> the target type
     * @return the deserialized instance
     */
    public static <T> T parse(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
