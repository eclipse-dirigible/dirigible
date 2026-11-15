/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable description of what to read off a document: header fields and, optionally, the fields
 * of each line item. Each builder call returns a new schema.
 */
public final class CaptureSchema {

    private static final Gson GSON = new Gson();

    private record Field(String name, CaptureType type, String description) {
    }

    private final List<Field> fields;
    private final List<Field> items;

    private CaptureSchema(List<Field> fields, List<Field> items) {
        this.fields = fields;
        this.items = items;
    }

    /**
     * An empty schema.
     *
     * @return the schema
     */
    public static CaptureSchema create() {
        return new CaptureSchema(List.of(), List.of());
    }

    /**
     * Add a header field.
     *
     * @param name the field name
     * @param type the value type
     * @param description what to read, may be null
     * @return the extended schema
     */
    public CaptureSchema field(String name, CaptureType type, String description) {
        List<Field> next = new ArrayList<>(fields);
        next.add(new Field(name, type, description));
        return new CaptureSchema(List.copyOf(next), items);
    }

    /**
     * Add a field of each line item.
     *
     * @param name the field name
     * @param type the value type
     * @param description what to read, may be null
     * @return the extended schema
     */
    public CaptureSchema item(String name, CaptureType type, String description) {
        List<Field> next = new ArrayList<>(items);
        next.add(new Field(name, type, description));
        return new CaptureSchema(fields, List.copyOf(next));
    }

    /**
     * Whether line items are read.
     *
     * @return true when at least one item field is declared
     */
    public boolean hasItems() {
        return !items.isEmpty();
    }

    /**
     * The JSON schema the provider's answer must follow.
     *
     * @return the schema as JSON text
     */
    public String toJsonSchema() {
        JsonObject headerProperties = new JsonObject();
        JsonArray headerRequired = new JsonArray();
        for (Field f : fields) {
            JsonObject property = object();
            property.add("required", array("value", "confidence"));
            if (hasText(f.description())) {
                property.addProperty("description", f.description());
            }
            JsonObject inner = new JsonObject();
            inner.add("value", nullable(f.type()));
            inner.add("confidence", confidence());
            property.add("properties", inner);
            headerProperties.add(f.name(), property);
            headerRequired.add(f.name());
        }
        JsonObject header = new JsonObject();
        header.addProperty("type", "object");
        header.addProperty("additionalProperties", false);
        header.add("required", headerRequired);
        header.add("properties", headerProperties);

        JsonObject itemProperties = new JsonObject();
        JsonArray itemRequired = new JsonArray();
        for (Field f : items) {
            JsonObject property = nullable(f.type());
            if (hasText(f.description())) {
                property.addProperty("description", f.description());
            }
            itemProperties.add(f.name(), property);
            itemRequired.add(f.name());
        }
        itemProperties.add("confidence", confidence());
        itemRequired.add("confidence");
        JsonObject item = new JsonObject();
        item.addProperty("type", "object");
        item.addProperty("additionalProperties", false);
        item.add("required", itemRequired);
        item.add("properties", itemProperties);
        JsonObject itemsArray = new JsonObject();
        itemsArray.addProperty("type", "array");
        itemsArray.add("items", item);

        JsonObject properties = new JsonObject();
        properties.add("fields", header);
        properties.add("items", itemsArray);
        JsonObject root = new JsonObject();
        root.addProperty("type", "object");
        root.addProperty("additionalProperties", false);
        root.add("required", array("fields", "items"));
        root.add("properties", properties);
        return GSON.toJson(root);
    }

    private static JsonObject object() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "object");
        o.addProperty("additionalProperties", false);
        return o;
    }

    private static JsonArray array(String... values) {
        JsonArray a = new JsonArray();
        for (String v : values) {
            a.add(v);
        }
        return a;
    }

    private static JsonObject confidence() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "string");
        o.add("enum", array("high", "medium", "low"));
        return o;
    }

    private static JsonObject nullable(CaptureType type) {
        JsonObject t = new JsonObject();
        switch (type) {
            case STRING -> t.addProperty("type", "string");
            case INTEGER -> t.addProperty("type", "integer");
            case NUMBER -> t.addProperty("type", "number");
            case DATE -> {
                t.addProperty("type", "string");
                t.addProperty("format", "date");
            }
            case BOOLEAN -> t.addProperty("type", "boolean");
        }
        JsonObject isNull = new JsonObject();
        isNull.addProperty("type", "null");
        JsonArray anyOf = new JsonArray();
        anyOf.add(t);
        anyOf.add(isNull);
        JsonObject o = new JsonObject();
        o.add("anyOf", anyOf);
        return o;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
