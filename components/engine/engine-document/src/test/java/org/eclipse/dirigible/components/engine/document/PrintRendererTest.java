/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code .print} render pipeline (parse + bind + XSL-FO rendering).
 */
class PrintRendererTest {

    private static final String TEMPLATE = """
            <document id="sales-invoice">
                <page>
                    <section>
                        <field label="Number">{{document.number}}</field>
                        <field label="Customer">{{document.customer}}</field>
                    </section>
                    <table source="items">
                        <column width="2*">{{name}}</column>
                        <column width="*" align="right">{{amount}}</column>
                    </table>
                    <total align="right">{{document.total}}</total>
                </page>
            </document>
            """;

    @Test
    void mergesDocumentValuesIntoFo() {
        String fo = PrintRenderer.renderFo(TEMPLATE, data());

        assertTrue(fo.contains("INV-001"), "document.number should be merged");
        assertTrue(fo.contains("ACME Ltd."), "document.customer should be merged");
        assertTrue(fo.contains("123.45"), "document.total should be merged");
    }

    @Test
    void expandsTableRowsFromItems() {
        String fo = PrintRenderer.renderFo(TEMPLATE, data());

        assertTrue(fo.contains("Widget"), "first item should be rendered");
        assertTrue(fo.contains("Gadget"), "second item should be rendered");
        assertTrue(fo.contains("100.00"), "first item amount should be rendered");
        assertTrue(fo.contains("23.45"), "second item amount should be rendered");
    }

    @Test
    void unresolvedPlaceholdersNeverLeakRawBraces() {
        String fo = PrintRenderer.renderFo(TEMPLATE, Map.of("items", List.of()));

        assertFalse(fo.contains("{{"), "unresolved placeholders must render empty, not as raw braces");
    }

    private static Map<String, Object> data() {
        return Map.of("document", Map.of("number", "INV-001", "customer", "ACME Ltd.", "total", "123.45"), "items",
                List.of(Map.of("name", "Widget", "amount", "100.00"), Map.of("name", "Gadget", "amount", "23.45")));
    }
}
