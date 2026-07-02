/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.Test;

public class LayoutEngineTest {

    private final DocumentParser parser = new DocumentParser();
    private final LayoutEngine engine = new LayoutEngine();

    @Test
    public void layoutTreeMirrorsTheAst() {
        Node root = parser.parse("<document><section><text>Hi</text></section></document>");
        LayoutNode layout = engine.layout(root);
        assertSame(root, layout.source());
        assertEquals(1, layout.children()
                              .size());
        assertEquals(1, layout.children()
                              .get(0)
                              .children()
                              .size());
        assertEquals("text", layout.children()
                                   .get(0)
                                   .children()
                                   .get(0)
                                   .source()
                                   .tag());
    }

    @Test
    public void defaultsAreAutoAndLeft() {
        LayoutNode layout = engine.layout(parser.parse("<document/>"));
        assertSame(Measurement.AUTO, layout.width());
        assertSame(Measurement.AUTO, layout.height());
        assertEquals(Alignment.LEFT, layout.alignment());
    }

    @Test
    public void explicitValuesAreResolved() {
        LayoutNode layout = engine.layout(parser.parse("<document width=\"595\" height=\"842\" align=\"center\"/>"));
        assertEquals(Measurement.px(595), layout.width());
        assertEquals(Measurement.px(842), layout.height());
        assertEquals(Alignment.CENTER, layout.alignment());
    }

    @Test
    public void flexBecomesFractionInsideFlexContainers() {
        Node root = parser.parse("<document><row><stack flex=\"2\"/><stack flex=\"1\"/></row></document>");
        LayoutNode row = engine.layout(root)
                               .children()
                               .get(0);
        assertEquals(Measurement.fraction(2), row.children()
                                                 .get(0)
                                                 .width());
        assertEquals(Measurement.fraction(1), row.children()
                                                 .get(1)
                                                 .width());
    }

    @Test
    public void explicitWidthWinsOverFlex() {
        Node root = parser.parse("<document><row><stack flex=\"2\" width=\"100\"/></row></document>");
        LayoutNode stack = engine.layout(root)
                                 .children()
                                 .get(0)
                                 .children()
                                 .get(0);
        assertEquals(Measurement.px(100), stack.width());
    }

    @Test
    public void flexOutsideFlexContainersIsIgnored() {
        Node root = parser.parse("<document><section flex=\"2\"/></document>");
        LayoutNode section = engine.layout(root)
                                   .children()
                                   .get(0);
        assertSame(Measurement.AUTO, section.width());
    }

    @Test
    public void resolvesColumnWidthsWithFractionDefault() {
        Node root = parser.parse("""
                <document>
                    <table source="invoice.items">
                        <column width="4*">{{a}}</column>
                        <column>{{b}}</column>
                        <column width="2*">{{c}}</column>
                        <column width="2*">{{d}}</column>
                    </table>
                </document>
                """);
        TableNode table = (TableNode) root.children()
                                          .get(0);
        List<Measurement> widths = LayoutEngine.resolveColumnWidths(table);
        assertEquals(List.of(Measurement.fraction(4), Measurement.fraction(1), Measurement.fraction(2), Measurement.fraction(2)), widths);
    }

    @Test
    public void fractionSharesNormalizeWeights() {
        double[] shares = LayoutEngine.fractionShares(
                List.of(Measurement.fraction(4), Measurement.fraction(1), Measurement.fraction(2), Measurement.fraction(2)));
        assertEquals(4.0 / 9, shares[0], 0.000001);
        assertEquals(1.0 / 9, shares[1], 0.000001);
        assertEquals(2.0 / 9, shares[2], 0.000001);
        assertEquals(2.0 / 9, shares[3], 0.000001);
    }

    @Test
    public void fractionSharesSkipAbsoluteAndPercentEntries() {
        double[] shares = LayoutEngine.fractionShares(List.of(Measurement.px(100), Measurement.fraction(1), Measurement.fraction(2)));
        assertEquals(0, shares[0], 0.000001);
        assertEquals(1.0 / 3, shares[1], 0.000001);
        assertEquals(2.0 / 3, shares[2], 0.000001);
    }

    @Test
    public void fractionSharesWithoutFractionsAreAllZero() {
        double[] shares = LayoutEngine.fractionShares(List.of(Measurement.px(100), Measurement.percent(50)));
        assertEquals(0, shares[0], 0.000001);
        assertEquals(0, shares[1], 0.000001);
    }

    @Test
    public void invalidWidthFailsWithTheAttributePosition() {
        Node root = parser.parse("<document>\n  <text width=\"banana\">x</text>\n</document>");
        try {
            engine.layout(root);
            fail("Expected LayoutException");
        } catch (LayoutException e) {
            assertEquals(2, e.getPosition()
                             .line());
            assertEquals(9, e.getPosition()
                             .column());
        }
    }

    @Test
    public void invalidAlignFailsWithTheAttributePosition() {
        Node root = parser.parse("<document><text align=\"middle\">x</text></document>");
        try {
            engine.layout(root);
            fail("Expected LayoutException");
        } catch (LayoutException e) {
            assertEquals(17, e.getPosition()
                              .column());
        }
    }
}
