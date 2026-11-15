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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureSchemaTest {

    private static final String CONFIDENCE = "{\"type\":\"string\",\"enum\":[\"high\",\"medium\",\"low\"]}";

    private static String nullable(String type) {
        return "{\"anyOf\":[{" + type + "},{\"type\":\"null\"}]}";
    }

    @Test
    void headerAndItems() {
        CaptureSchema schema = CaptureSchema.create()
                                            .field("Number", CaptureType.STRING, "The document number")
                                            .field("Date", CaptureType.DATE, null)
                                            .item("Qty", CaptureType.NUMBER, "Quantity")
                                            .item("Name", CaptureType.STRING, null);
        assertTrue(schema.hasItems());
        String expected = "{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"fields\",\"items\"],\"properties\":{"
                + "\"fields\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"Number\",\"Date\"],\"properties\":{"
                + "\"Number\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"value\",\"confidence\"],\"description\":\"The document number\","
                + "\"properties\":{\"value\":" + nullable("\"type\":\"string\"") + ",\"confidence\":" + CONFIDENCE + "}},"
                + "\"Date\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"value\",\"confidence\"],"
                + "\"properties\":{\"value\":" + nullable("\"type\":\"string\",\"format\":\"date\"") + ",\"confidence\":" + CONFIDENCE
                + "}}}},"
                + "\"items\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"Qty\",\"Name\",\"confidence\"],"
                + "\"properties\":{\"Qty\":{\"anyOf\":[{\"type\":\"number\"},{\"type\":\"null\"}],\"description\":\"Quantity\"},"
                + "\"Name\":" + nullable("\"type\":\"string\"") + ",\"confidence\":" + CONFIDENCE + "}}}}}";
        assertEquals(JsonParser.parseString(expected), JsonParser.parseString(schema.toJsonSchema()));
        JsonObject root = JsonParser.parseString(schema.toJsonSchema())
                                    .getAsJsonObject();
        assertEquals(List.of("Number", "Date"), new ArrayList<>(root.getAsJsonObject("properties")
                                                                    .getAsJsonObject("fields")
                                                                    .getAsJsonObject("properties")
                                                                    .keySet()));
    }

    @Test
    void headerOnly() {
        CaptureSchema schema = CaptureSchema.create()
                                            .field("Total", CaptureType.NUMBER, "")
                                            .field("Paid", CaptureType.BOOLEAN, null)
                                            .field("Count", CaptureType.INTEGER, null);
        assertFalse(schema.hasItems());
        JsonObject root = JsonParser.parseString(schema.toJsonSchema())
                                    .getAsJsonObject();
        JsonObject items = root.getAsJsonObject("properties")
                               .getAsJsonObject("items");
        JsonElement expectedItems =
                JsonParser.parseString("{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,"
                        + "\"required\":[\"confidence\"],\"properties\":{\"confidence\":" + CONFIDENCE + "}}}");
        assertEquals(expectedItems, items);
        JsonObject total = root.getAsJsonObject("properties")
                               .getAsJsonObject("fields")
                               .getAsJsonObject("properties")
                               .getAsJsonObject("Total");
        assertFalse(total.has("description"));
        assertEquals(JsonParser.parseString(nullable("\"type\":\"number\"")), total.getAsJsonObject("properties")
                                                                                   .get("value"));
        assertTrue(schema.toJsonSchema()
                         .contains("\"type\":\"boolean\""));
        assertTrue(schema.toJsonSchema()
                         .contains("\"type\":\"integer\""));
    }
}
