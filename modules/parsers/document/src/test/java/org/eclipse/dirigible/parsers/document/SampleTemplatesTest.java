/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.Test;

/**
 * Parses the shipped example templates — one per supported business document — and deep-asserts the
 * canonical sales invoice.
 */
public class SampleTemplatesTest {

    private static final List<String> TEMPLATES =
            List.of("sales-invoice", "purchase-order", "quote", "delivery-note", "receipt", "statement", "credit-note");

    private final DocumentParser parser = new DocumentParser();

    private static String load(String name) throws IOException {
        try (InputStream in = SampleTemplatesTest.class.getResourceAsStream("/templates/" + name + ".print")) {
            assertNotNull("Missing sample template: " + name, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void allSampleTemplatesParse() throws IOException {
        for (String template : TEMPLATES) {
            DocumentNode document = parser.parseDocument(load(template));
            assertEquals(template, document.attributes()
                                           .get("id"));
        }
    }

    @Test
    public void salesInvoiceHasTheExpectedStructure() throws IOException {
        DocumentNode document = parser.parseDocument(load("sales-invoice"));

        assertEquals(1, document.children()
                                .size());
        Node page = document.children()
                            .get(0);
        assertTrue(page instanceof PageNode);
        assertEquals("595", page.attributes()
                                .get("width"));

        List<Node> parts = page.children();
        assertTrue(parts.get(0) instanceof HeaderNode);
        assertTrue(parts.get(1) instanceof SectionNode);
        assertTrue(parts.get(2) instanceof SectionNode);
        assertTrue(parts.get(3) instanceof SpaceNode);
        assertTrue(parts.get(4) instanceof TableNode);
        assertTrue(parts.get(5) instanceof SectionNode);
        assertTrue(parts.get(6) instanceof FooterNode);

        TableNode table = (TableNode) parts.get(4);
        assertEquals("invoice.items", table.attributes()
                                           .get("source"));
        assertEquals(4, table.children()
                             .size());
        ColumnNode description = (ColumnNode) table.children()
                                                   .get(0);
        assertEquals("45%", description.attributes()
                                       .get("width"));
        assertEquals("{{description}}", description.text());

        SectionNode totals = (SectionNode) parts.get(5);
        Node lastTotalsChild = totals.children()
                                     .get(totals.children()
                                                .size()
                                             - 1);
        assertTrue(lastTotalsChild instanceof TotalNode);
        assertEquals("{{invoice.total}}", lastTotalsChild.text());
        assertTrue(totals.children()
                         .get(1) instanceof IfNode);

        FooterNode footer = (FooterNode) parts.get(6);
        assertEquals("Terms & Conditions apply — Page {{page}} of {{pages}}", footer.children()
                                                                                    .get(0)
                                                                                    .text());
    }

    @Test
    public void receiptKeepsTheRawAmpersand() throws IOException {
        DocumentNode document = parser.parseDocument(load("receipt"));
        String all = flattenText(document);
        assertTrue(all.contains("Cash & Card"));
    }

    private static String flattenText(Node node) {
        StringBuilder text = new StringBuilder(node.text());
        for (Node child : node.children()) {
            text.append(' ')
                .append(flattenText(child));
        }
        return text.toString();
    }
}
