/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.binding.DataBinder;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.Test;

public class XslFoRendererTest {

    private final DocumentParser parser = new DocumentParser();
    private final DataBinder binder = new DataBinder();
    private final XslFoRenderer renderer = new XslFoRenderer();

    private String renderTemplate(String template, Map<String, Object> data) {
        Node bound = binder.bind(parser.parse(template), data);
        return renderer.renderBound(bound);
    }

    @Test
    public void emitsASelfContainedStylesheetWithPageGeometry() {
        String fo =
                renderTemplate("<document><page width=\"595\" height=\"842\" padding=\"40\"><text>Hi</text></page></document>", Map.of());
        assertTrue(fo.startsWith("<?xml version=\"1.0\""));
        assertTrue(fo.contains("<xsl:template match=\"/\">"));
        assertTrue(fo.contains("page-width=\"595pt\""));
        assertTrue(fo.contains("page-height=\"842pt\""));
        assertTrue(fo.contains("margin=\"40pt\""));
        assertTrue(fo.contains("<fo:block>Hi</fo:block>"));
    }

    @Test
    public void defaultsToA4WithoutAPageNode() {
        String fo = renderTemplate("<document><text>Hi</text></document>", Map.of());
        assertTrue(fo.contains("page-width=\"595pt\""));
        assertTrue(fo.contains("page-height=\"842pt\""));
    }

    @Test
    public void rendersTextStylesAndAlignment() {
        String fo = renderTemplate("<document><text align=\"right\" style=\"title\">Sales Invoice</text></document>", Map.of());
        assertTrue(fo.contains("text-align=\"right\""));
        assertTrue(fo.contains("font-size=\"18pt\" font-weight=\"bold\""));
        assertTrue(fo.contains(">Sales Invoice</fo:block>"));
    }

    @Test
    public void rendersFieldsAsLabelValue() {
        String fo = renderTemplate("<document><field label=\"Invoice No\">{{n}}</field></document>", Map.of("n", "SI-1"));
        assertTrue(fo.contains("<fo:inline font-weight=\"bold\">Invoice No: </fo:inline>SI-1"));
    }

    @Test
    public void rendersDataTableWithHeaderAndProportionalColumns() {
        String fo = renderTemplate("""
                <document>
                    <table source="items">
                        <column width="3*" label="Description">{{description}}</column>
                        <column width="*" align="right" label="Amount">{{amount}}</column>
                    </table>
                </document>
                """, Map.of("items", List.of(Map.of("description", "Widget", "amount", "10.00"))));
        assertTrue(fo.contains("proportional-column-width(3)"));
        assertTrue(fo.contains("proportional-column-width(1)"));
        assertTrue(fo.contains("<fo:table-header>"));
        assertTrue(fo.contains(">Description</fo:block>"));
        assertTrue(fo.contains(">Widget</fo:block>"));
        assertTrue(fo.contains("text-align=\"right\">10.00</fo:block>"));
    }

    @Test
    public void percentAndPixelColumnWidthsAreEmitted() {
        String fo = renderTemplate("""
                <document>
                    <table source="items">
                        <column width="45%">{{a}}</column>
                        <column width="100px">{{b}}</column>
                    </table>
                </document>
                """, Map.of("items", List.of(Map.of("a", "x", "b", "y"))));
        assertTrue(fo.contains("column-width=\"45%\""));
        assertTrue(fo.contains("column-width=\"100pt\""));
    }

    @Test
    public void tableWithoutRowsIsSkipped() {
        String fo = renderTemplate("<document><table source=\"missing\"><column label=\"X\">{{x}}</column></table></document>", Map.of());
        assertFalse(fo.contains("<fo:table "));
    }

    @Test
    public void freeStandingRowBecomesASingleRowTable() {
        String fo = renderTemplate(
                "<document><row><stack flex=\"2\"><text>L</text></stack><stack flex=\"1\"><text>R</text></stack></row></document>",
                Map.of());
        assertTrue(fo.contains("proportional-column-width(2)"));
        assertTrue(fo.contains("<fo:table-row>"));
        assertEquals(2, fo.split("<fo:table-cell>", -1).length - 1);
    }

    @Test
    public void escapesXmlSpecialCharacters() {
        String fo = renderTemplate("<document><text>Fruit & Veg &lt;fresh&gt;</text></document>", Map.of());
        assertTrue(fo.contains("Fruit &amp; Veg &lt;fresh&gt;"));
    }

    @Test
    public void rendersLineSpaceTotalAndImage() {
        String fo = renderTemplate("""
                <document>
                    <line/>
                    <space height="20"/>
                    <total align="right">{{t}}</total>
                    <image src="{{logo}}" width="120"/>
                </document>
                """, Map.of("t", "302.00", "logo", "/logo.png"));
        assertTrue(fo.contains("leader-pattern=\"rule\""));
        assertTrue(fo.contains("space-after=\"20pt\""));
        assertTrue(fo.contains("font-size=\"12pt\" font-weight=\"bold\" space-before=\"6pt\">302.00</fo:block>"));
        assertTrue(fo.contains("<fo:external-graphic src=\"/logo.png\" content-width=\"120pt\"/>"));
    }

    @Test
    public void salesInvoiceSampleRendersEndToEnd() throws Exception {
        String template = new String(getClass().getResourceAsStream("/templates/sales-invoice.print")
                                               .readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        Map<String, Object> data = Map.of("company", Map.of("name", "ACME", "logo", "/logo.png", "address", "Main St 1"), "invoice",
                Map.of("number", "SI00000001", "date", "2026-07-02", "dueDate", "2026-07-16", "subtotal", "260.00", "vat", "52.00", "total",
                        "302.00", "hasDiscount", false, "items",
                        List.of(Map.of("description", "Widget", "quantity", "2", "price", "100.00", "amount", "240.00"))),
                "customer", Map.of("name", "John Doe", "address", "Elm St 2", "vatNumber", "BG123"), "page", 1, "pages", 1);
        String fo = renderTemplate(template, data);
        assertTrue(fo.contains("SI00000001"));
        assertTrue(fo.contains(">Widget</fo:block>"));
        assertTrue(fo.contains("302.00"));
        assertFalse("discount line must be dropped", fo.contains("Discount"));
        assertFalse("no unresolved placeholders may leak", fo.contains("{{"));
    }
}
