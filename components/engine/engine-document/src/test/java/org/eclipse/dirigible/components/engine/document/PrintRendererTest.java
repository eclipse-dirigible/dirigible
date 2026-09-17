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

import org.eclipse.dirigible.parsers.document.renderer.ImageResolver;
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
        String fo = renderFo(TEMPLATE, data());

        assertTrue(fo.contains("INV-001"), "document.number should be merged");
        assertTrue(fo.contains("ACME Ltd."), "document.customer should be merged");
        assertTrue(fo.contains("123.45"), "document.total should be merged");
    }

    @Test
    void expandsTableRowsFromItems() {
        String fo = renderFo(TEMPLATE, data());

        assertTrue(fo.contains("Widget"), "first item should be rendered");
        assertTrue(fo.contains("Gadget"), "second item should be rendered");
        assertTrue(fo.contains("100.00"), "first item amount should be rendered");
        assertTrue(fo.contains("23.45"), "second item amount should be rendered");
    }

    @Test
    void unresolvedPlaceholdersNeverLeakRawBraces() {
        String fo = renderFo(TEMPLATE, Map.of("items", List.of()));

        assertFalse(fo.contains("{{"), "unresolved placeholders must render empty, not as raw braces");
    }

    /**
     * The alternative-operand placeholder, rendered through the whole pipeline: a filled first operand
     * wins, a blank one falls through to the next, and all-blank renders empty exactly as a single
     * unresolved path does. The twin-field case this exists for - an optional locally registered name
     * beside the canonical one - must never leave a hole in the printed document.
     */
    @Test
    void alternativePlaceholderOperandsRenderTheFirstNonBlankValue() {
        String fo = renderFo(FALLBACK_TEMPLATE,
                Map.of("document", Map.of("NameLocal", "Metafor OOD", "Name", "Metaphor Ltd."), "items", List.of()));

        assertTrue(fo.contains("Metafor OOD"), "the filled first operand should win");
        assertFalse(fo.contains("Metaphor Ltd."), "the fallback must not be rendered when the first operand is filled");
    }

    @Test
    void aBlankFirstOperandFallsThroughToTheNext() {
        String fo = renderFo(FALLBACK_TEMPLATE,
                Map.of("document", Map.of("Name", "Metaphor Ltd.", "Spaces", "   ", "Note", "the note"), "items", List.of()));

        assertTrue(fo.contains("Metaphor Ltd."), "an absent first operand should fall through to the canonical name");
        assertTrue(fo.contains("the note"), "a whitespace-only first operand should fall through as well");
    }

    @Test
    void allBlankOperandsRenderEmpty() {
        String fo = renderFo(FALLBACK_TEMPLATE, Map.of("items", List.of()));

        assertFalse(fo.contains("{{"), "all-blank alternatives must render empty, not as raw braces");
        assertFalse(fo.contains("NameLocal"), "no operand path may leak into the output");
    }

    private static final String FALLBACK_TEMPLATE = """
            <document id="sales-invoice">
                <page>
                    <section>
                        <field label="Customer">{{document.NameLocal|document.Name}}</field>
                        <field label="Note">{{document.Spaces|document.Note}}</field>
                    </section>
                </page>
            </document>
            """;

    /**
     * An image whose source the host resolves is embedded; one it declines (a missing logo, an
     * oversized file, a document that is not an image) leaves NO graphic behind - a printed document
     * without a logo is correct output, a broken-image box is not.
     */
    @Test
    void anImageIsEmbeddedThroughTheHostResolver() {
        String fo = PrintRenderer.renderFo(IMAGE_TEMPLATE, Map.of("document", Map.of("Logo", "/Templates/Print/logo.png")),
                source -> "data:image/png;base64,AAAA");

        assertTrue(fo.contains("<fo:external-graphic src=\"data:image/png;base64,AAAA\""), "the resolved source should be emitted");
        assertTrue(fo.contains("content-width=\"120pt\""), "the width hint should size the image");
    }

    @Test
    void anUnresolvableImageRendersNothing() {
        String fo = PrintRenderer.renderFo(IMAGE_TEMPLATE, Map.of("document", Map.of("Logo", "/Templates/Print/logo.png")), source -> null);

        assertFalse(fo.contains("external-graphic"), "an image the host cannot read must not be rendered at all");
    }

    @Test
    void anImageWithNoBoundSourceIsNeverHandedToTheResolver() {
        String fo = PrintRenderer.renderFo(IMAGE_TEMPLATE, Map.of(), source -> {
            throw new AssertionError("a blank source must not reach the resolver: [" + source + "]");
        });

        assertFalse(fo.contains("external-graphic"), "an unbound image source must not be rendered");
    }

    private static final String IMAGE_TEMPLATE = """
            <document id="sales-invoice">
                <page>
                    <image src="{{document.Logo}}" width="120"/>
                </page>
            </document>
            """;

    private static String renderFo(String template, Map<String, Object> data) {
        return PrintRenderer.renderFo(template, data, ImageResolver.PASS_THROUGH);
    }

    private static Map<String, Object> data() {
        return Map.of("document", Map.of("number", "INV-001", "customer", "ACME Ltd.", "total", "123.45"), "items",
                List.of(Map.of("name", "Widget", "amount", "100.00"), Map.of("name", "Gadget", "amount", "23.45")));
    }
}
