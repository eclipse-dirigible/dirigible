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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Quick JSON serialization helpers around Jackson's default {@link ObjectMapper}. Convenient when
 * you just need {@code String}/object conversion without configuring a mapper instance — for
 * non-trivial schemas (custom serializers, mixed polymorphism, date format overrides) inject a
 * Jackson {@code ObjectMapper} bean instead.
 * <p>
 * XML conversion lives in {@link Xml}; CSV parsing requires a dedicated library (Apache Commons CSV
 * or OpenCSV) — this class deliberately does not pretend to handle quoting and escaping around the
 * edges of CSV semantics.
 */
public final class Converter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Converter() {}

    public static String toJson(Object input) {
        try {
            return MAPPER.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize to JSON: " + input, ex);
        }
    }

    public static <T> T fromJson(String input, Class<T> type) {
        try {
            return MAPPER.readValue(input, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse JSON to " + type.getName(), ex);
        }
    }

    public static Object fromJson(String input) {
        return fromJson(input, Object.class);
    }
}
