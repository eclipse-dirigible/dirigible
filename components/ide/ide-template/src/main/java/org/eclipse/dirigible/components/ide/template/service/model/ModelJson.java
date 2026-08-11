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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a model file into the untyped graph the generation pipeline works on.
 *
 * <p>
 * A plain {@link Gson} is used deliberately rather than the platform's shared helper, which is
 * configured to exclude fields without an exposure annotation and to pretty-print - neither of
 * which suits a map-shaped graph that is compared byte-for-byte after rendering. Parsing into maps
 * yields insertion-ordered maps and doubles for every number, which is exactly the typing the
 * template engines see.
 */
final class ModelJson {

    /** The parser. */
    private static final Gson GSON = new Gson();

    /** The type token for an untyped object graph. */
    private static final TypeToken<Map<String, Object>> OBJECT_TYPE = new TypeToken<>() {};

    /**
     * Not instantiable.
     */
    private ModelJson() {}

    /**
     * Parses a JSON object.
     *
     * @param json the JSON text
     * @return the parsed object, empty when the text is null or parses to null
     */
    static Map<String, Object> parseObject(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> parsed = GSON.fromJson(json, OBJECT_TYPE);
        return parsed == null ? new LinkedHashMap<>() : parsed;
    }

    /**
     * Deep-copies a graph node, so a merged node can be mutated without touching its source.
     *
     * @param node the node
     * @return the copy
     */
    static Object deepCopy(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
            }
            return copy;
        }
        if (node instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(deepCopy(element));
            }
            return copy;
        }
        return node;
    }

}
