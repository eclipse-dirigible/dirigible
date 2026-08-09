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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the three ways this writer has to differ from Gson to reproduce {@code JSON.stringify}.
 * Each one shows up in the generation descriptor and the translation catalogs, which are
 * content-compared across regenerations.
 */
class JavaScriptJsonTest {

    @Test
    void keepsNullValuedKeys() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("fk", null);
        assertEquals("{\n  \"fk\": null\n}", JavaScriptJson.pretty(value));
    }

    @Test
    void doesNotEscapeHtmlCharacters() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("label", "Sales & Returns <all>");
        assertEquals("{\"label\":\"Sales & Returns <all>\"}", JavaScriptJson.compact(value));
    }

    @Test
    void writesAnIntegralNumberWithoutAFraction() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("minLength", Double.valueOf(0));
        value.put("widgetSize", Double.valueOf(6));
        value.put("price", Double.valueOf(12.5));
        assertEquals("{\"minLength\":0,\"widgetSize\":6,\"price\":12.5}", JavaScriptJson.compact(value));
    }

    @Test
    void writesNotANumberAsNullTheWayJavaScriptDoes() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("maxLength", Double.NaN);
        assertEquals("{\"maxLength\":null}", JavaScriptJson.compact(value));
    }

    @Test
    void preservesKeyOrder() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("zebra", 1d);
        value.put("apple", 2d);
        assertEquals("{\"zebra\":1,\"apple\":2}", JavaScriptJson.compact(value));
    }

    @Test
    void indentsNestedStructuresByTwoSpaces() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("name", "Book");
        List<Object> list = new ArrayList<>();
        list.add(inner);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("entities", list);
        assertEquals("""
                {
                  "entities": [
                    {
                      "name": "Book"
                    }
                  ]
                }""", JavaScriptJson.pretty(value));
    }

    @Test
    void writesEmptyStructuresInline() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("roles", new ArrayList<>());
        value.put("perspectives", new LinkedHashMap<>());
        assertEquals("{\n  \"roles\": [],\n  \"perspectives\": {}\n}", JavaScriptJson.pretty(value));
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\"", JavaScriptJson.compact("a\"b\\c\nd\te"));
        assertEquals("\"\\u0001\"", JavaScriptJson.compact("\u0001"));
    }

}
