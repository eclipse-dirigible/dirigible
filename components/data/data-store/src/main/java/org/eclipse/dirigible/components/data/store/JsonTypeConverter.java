/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class to recursively traverse a nested Map (deserialized from JSON) and safely convert
 * numeric types (Double/Float) into Long objects if they represent whole numbers or are identified
 * as ID fields.
 */
public class JsonTypeConverter {

    /**
     * Recursively traverses the map and converts numeric types (Double, Float) into Long if they
     * represent a whole number, or if the key suggests an ID field.
     *
     * @param data The map object deserialized from JSON.
     * @return The mutated map with normalized number types.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeNumericTypes(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        for (Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                normalizeNumericTypes(nestedMap);
            } else if (value instanceof Number) {
                boolean isIdKey = key.toLowerCase(Locale.ROOT)
                                     .endsWith("id");
                if (isIdKey) {
                    Object normalizedValue = safeToLong(value);
                    if (normalizedValue instanceof Long) {
                        entry.setValue(normalizedValue);
                    }
                }
            }
        }
        return data;
    }

    /**
     * Safely converts an object value into a Long if it represents a whole number, or if the key is an
     * ID key.
     *
     * @param value The raw object value (Double, Float, etc.).
     * @return The original object or the newly converted Long object.
     */
    private static Object safeToLong(Object value) {
        if (value == null || value instanceof Long) {
            return value;
        }

        if (value instanceof Double || value instanceof Float) {
            double doubleValue = ((Number) value).doubleValue();
            long longValue = (long) doubleValue;

            // Check if the fractional part is zero (e.g., 123.0)
            if (doubleValue == longValue) {
                return Long.valueOf(longValue);
            }
        }

        return value;
    }
}
