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

import java.io.IOException;
import java.lang.reflect.Method;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * JSON (de)serialization for client Java code. A thin wrapper over a {@link Gson} instance
 * configured to handle the {@code java.time} types the generated entities use, serializing each as
 * its ISO-8601 string form.
 * <p>
 * A bare {@code new Gson()} cannot serialize these on JDK 17+: Gson falls back to field reflection
 * and {@code java.base} does not open {@code java.time} to other modules, so the call fails with
 * {@code InaccessibleObjectException} (e.g. on {@code LocalDate#year} or {@code Instant#seconds}).
 * Use this helper - instead of constructing {@code Gson} directly - whenever entity instances are
 * involved (publishing one to a message topic, parsing one back, ...), since they routinely carry
 * date/time fields. The generated entities map {@code date} to {@link java.time.LocalDate},
 * {@code time} to {@link java.time.LocalTime} and {@code timestamp}/audit fields to
 * {@link java.time.Instant}; this helper covers those and every other {@code java.time} type that
 * exposes a static {@code parse(CharSequence)} (Local/Offset/Zoned date-times, Year, Duration,
 * ...).
 */
public final class Json {

    /**
     * Serializes any {@code java.time.*} value via {@link Object#toString()} (ISO-8601) and reads it
     * back through that type's static {@code parse(CharSequence)} - the uniform contract those types
     * share - so no per-type registration is needed and new ones are handled automatically. Types
     * without such a factory method are left to Gson's default handling.
     */
    private static final TypeAdapterFactory JAVA_TIME = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<? super T> raw = typeToken.getRawType();
            if (!raw.getName()
                    .startsWith("java.time.")) {
                return null;
            }
            final Method parse;
            try {
                parse = raw.getMethod("parse", CharSequence.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.toString());
                    }
                }

                @Override
                @SuppressWarnings("unchecked")
                public T read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    String text = in.nextString();
                    try {
                        return (T) parse.invoke(null, text);
                    } catch (ReflectiveOperationException e) {
                        throw new IOException("Cannot parse " + raw.getName() + " from [" + text + "]", e);
                    }
                }
            };
        }
    };

    private static final Gson GSON = new GsonBuilder().registerTypeAdapterFactory(JAVA_TIME)
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
