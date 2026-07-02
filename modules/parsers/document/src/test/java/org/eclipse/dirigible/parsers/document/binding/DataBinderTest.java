/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.parsers.document.ColumnNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.Test;

public class DataBinderTest {

    private final DocumentParser parser = new DocumentParser();
    private final DataBinder binder = new DataBinder();

    @Test
    public void substitutesPlaceholdersInTextAndAttributes() {
        Node root = parser.parse("<document><field label=\"No\">{{invoice.number}}</field><image src=\"{{company.logo}}\"/></document>");
        Node bound = binder.bind(root, Map.of("invoice", Map.of("number", "SI-1"), "company", Map.of("logo", "/logo.png")));
        assertEquals("SI-1", bound.children()
                                  .get(0)
                                  .text());
        assertEquals("/logo.png", bound.children()
                                       .get(1)
                                       .attributes()
                                       .get("src"));
    }

    @Test
    public void unresolvedPlaceholdersRenderEmpty() {
        Node root = parser.parse("<document><text>Hi {{missing.path}}!</text></document>");
        Node bound = binder.bind(root, Map.of());
        assertEquals("Hi !", bound.children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void mixedTextAroundPlaceholdersIsPreserved() {
        Node root = parser.parse("<document><text>Page {{page}} of {{pages}}</text></document>");
        Node bound = binder.bind(root, Map.of("page", 1, "pages", 3));
        assertEquals("Page 1 of 3", bound.children()
                                         .get(0)
                                         .text());
    }

    @Test
    public void tableExpandsOneRowPerSourceElement() {
        Node root = parser.parse("""
                <document>
                    <table source="invoice.items">
                        <column width="3*">{{description}}</column>
                        <column width="*" align="right">{{amount}}</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("invoice", Map.of("items",
                List.of(Map.of("description", "Widget", "amount", "10.00"), Map.of("description", "Gadget", "amount", "20.00")))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        // 2 column definitions (templates dropped) + 2 expanded rows
        assertEquals(4, table.children()
                             .size());
        assertTrue(table.children()
                        .get(0) instanceof ColumnNode);
        assertEquals("", table.children()
                              .get(0)
                              .text());
        RowNode firstRow = (RowNode) table.children()
                                          .get(2);
        assertEquals("Widget", firstRow.children()
                                       .get(0)
                                       .text());
        assertEquals("10.00", firstRow.children()
                                      .get(1)
                                      .text());
        assertEquals("right", firstRow.children()
                                      .get(1)
                                      .attributes()
                                      .get("align"));
    }

    @Test
    public void rowScopeFallsBackToTheDocumentScope() {
        Node root = parser.parse("""
                <document>
                    <table source="items">
                        <column>{{name}} ({{currency}})</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("currency", "EUR", "items", List.of(Map.of("name", "Widget"))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        assertEquals("Widget (EUR)", table.children()
                                          .get(1)
                                          .children()
                                          .get(0)
                                          .text());
    }

    @Test
    public void emptyOrMissingTableSourceYieldsNoRows() {
        Node root = parser.parse("<document><table source=\"missing\"><column>{{x}}</column></table></document>");
        TableNode table = (TableNode) binder.bind(root, Map.of())
                                            .children()
                                            .get(0);
        assertEquals(1, table.children()
                             .size());
    }

    @Test
    public void forExpandsItsChildrenPerElement() {
        Node root = parser.parse("""
                <document>
                    <for source="transactions">
                        <text>{{date}}: {{amount}}</text>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("transactions", List.of(Map.of("date", "01-01", "amount", "5"), Map.of("date", "01-02", "amount", "7"))));
        assertEquals(2, bound.children()
                             .size());
        assertEquals("01-01: 5", bound.children()
                                      .get(0)
                                      .text());
        assertEquals("01-02: 7", bound.children()
                                      .get(1)
                                      .text());
    }

    @Test
    public void ifKeepsChildrenWhenTruthyAndDropsThemWhenFalsy() {
        String template = "<document><if source=\"invoice.hasDiscount\"><text>Discount!</text></if></document>";
        Node kept = binder.bind(parser.parse(template), Map.of("invoice", Map.of("hasDiscount", true)));
        assertEquals(1, kept.children()
                            .size());
        for (Object falsy : new Object[] {false, "", 0, List.of(), Map.of()}) {
            Node dropped = binder.bind(parser.parse(template), Map.of("invoice", Map.of("hasDiscount", falsy)));
            assertEquals("Expected no children for " + falsy, 0, dropped.children()
                                                                        .size());
        }
        Node missing = binder.bind(parser.parse(template), Map.of());
        assertEquals(0, missing.children()
                               .size());
    }

    @Test
    public void nestedStructuresBindRecursively() {
        Node root = parser.parse("<document><section><row><stack><text>{{a.b}}</text></stack></row></section></document>");
        Node bound = binder.bind(root, Map.of("a", Map.of("b", "deep")));
        assertEquals("deep", bound.children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void bindingIsRepeatableOnTheSameTemplate() {
        Node root = parser.parse("<document><text>{{n}}</text></document>");
        assertEquals("1", binder.bind(root, Map.of("n", 1))
                                .children()
                                .get(0)
                                .text());
        assertEquals("2", binder.bind(root, Map.of("n", 2))
                                .children()
                                .get(0)
                                .text());
    }
}
