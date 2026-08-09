/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Value handling for the model-generation parameter graph.
 *
 * <p>
 * The graph is an untyped bag of maps, lists and scalars - the shape a template context has. Two
 * conventions in here are load-bearing for byte-identical output and must not be "cleaned up":
 *
 * <ul>
 * <li><b>Every number is a {@link Double}.</b> The graph reaches a template engine as the result of
 * parsing JSON with Gson, whose object type adapter maps every JSON number to {@code Double}. A
 * number computed while deriving parameters must therefore be stored as {@code Double} too, or the
 * same template renders {@code 6} where it used to render {@code 6.0} (or the reverse).</li>
 * <li><b>Absent is not null.</b> Assigning {@code undefined} to a key in JavaScript makes the key
 * vanish from the serialized graph, while a Java {@code null} value serializes as a present key
 * with a null value. Use {@link #remove(Map, String)} where the original assigned
 * {@code undefined}.</li>
 * </ul>
 */
final class ModelValues {

    /**
     * Not instantiable.
     */
    private ModelValues() {}

    /**
     * Views a graph node as a map.
     *
     * @param value the node
     * @return the map, or null when the node is not a map
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /**
     * Views a graph node as a list, tolerating an absent node the way a JavaScript {@code (x || [])}
     * guard does.
     *
     * @param value the node
     * @return the list, empty when the node is absent or not a list
     */
    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : Collections.emptyList();
    }

    /**
     * Views a graph node as a list of maps - the shape of every entity / property / glue collection.
     *
     * @param value the node
     * @return the list of maps, empty when the node is absent
     */
    static List<Map<String, Object>> asMaps(Object value) {
        List<Object> raw = asList(value);
        List<Map<String, Object>> result = new ArrayList<>(raw.size());
        for (Object element : raw) {
            Map<String, Object> map = asMap(element);
            if (map != null) {
                result.add(map);
            }
        }
        return result;
    }

    /**
     * Reads a string-valued key.
     *
     * @param map the node
     * @param key the key
     * @return the value as a string, or null when absent
     */
    static String str(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Reads a string-valued key, substituting a fallback when absent or empty - the JavaScript
     * {@code x ? x : fallback} idiom.
     *
     * @param map the node
     * @param key the key
     * @param fallback the fallback
     * @return the value, or the fallback
     */
    static String strOr(Map<String, Object> map, String key, String fallback) {
        String value = str(map, key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    /**
     * Tests a key for the string {@code "true"} - the model persists its flags as strings, and the
     * derivation pass coerces them.
     *
     * @param map the node
     * @param key the key
     * @return true when the value is the string or boolean true
     */
    static boolean isTrue(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    /**
     * Tests a node the way JavaScript truthiness does: present, not empty, not zero, not false.
     *
     * @param value the node
     * @return true when the value is truthy
     */
    static boolean truthy(Object value) {
        if (value == null || Boolean.FALSE.equals(value)) {
            return false;
        }
        if (value instanceof String string) {
            return !string.isEmpty();
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0d && !Double.isNaN(number.doubleValue());
        }
        return true;
    }

    /**
     * Tests a key for JavaScript truthiness.
     *
     * @param map the node
     * @param key the key
     * @return true when the value is truthy
     */
    static boolean truthy(Map<String, Object> map, String key) {
        return map != null && truthy(map.get(key));
    }

    /**
     * Stores a number, as the {@link Double} every number in the graph is.
     *
     * @param map the node
     * @param key the key
     * @param value the number
     */
    static void putNumber(Map<String, Object> map, String key, double value) {
        map.put(key, Double.valueOf(value));
    }

    /**
     * Removes a key, which is what assigning {@code undefined} to it does in the JavaScript original.
     *
     * @param map the node
     * @param key the key
     */
    static void remove(Map<String, Object> map, String key) {
        if (map != null) {
            map.remove(key);
        }
    }

    /**
     * Shallow-copies a node, the equivalent of a JavaScript object spread. Nested nodes stay shared,
     * exactly as they do there - the scrub in {@link #clean(Object)} is expected to reach them.
     *
     * @param map the node
     * @return a new map over the same nested nodes
     */
    static Map<String, Object> copy(Map<String, Object> map) {
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    /**
     * Recursively drops every key whose value is not-a-number, in place, before the node is handed to a
     * template engine. A {@code NaN} would otherwise reach the output as the literal text {@code NaN};
     * the model carries them because widget lengths and orders are parsed from free-text fields.
     *
     * @param data the node
     * @return the same node
     */
    static Object clean(Object data) {
        if (data instanceof List<?> list) {
            for (Object element : list) {
                clean(element);
            }
            return data;
        }
        Map<String, Object> map = asMap(data);
        if (map == null) {
            return data;
        }
        map.entrySet()
           .removeIf(entry -> isNotANumber(entry.getValue()));
        for (Object value : map.values()) {
            clean(value);
        }
        return data;
    }

    /**
     * Cleans a node and returns it as a map, for the common call right before rendering.
     *
     * @param map the node
     * @return the same node, scrubbed
     */
    static Map<String, Object> cleaned(Map<String, Object> map) {
        clean(map);
        return map;
    }

    /**
     * Tests whether a value is a not-a-number, in either the numeric or the already-stringified form
     * the model can carry.
     *
     * @param value the value
     * @return true when the value is NaN
     */
    private static boolean isNotANumber(Object value) {
        if (value instanceof Double number) {
            return number.isNaN();
        }
        if (value instanceof Float number) {
            return number.isNaN();
        }
        return "NaN".equals(value);
    }

}
