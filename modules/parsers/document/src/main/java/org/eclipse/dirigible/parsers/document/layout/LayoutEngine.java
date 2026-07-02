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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.parsers.document.ColumnNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.StackNode;
import org.eclipse.dirigible.parsers.document.TableNode;

/**
 * Normalizes a parsed template into a {@link LayoutNode} tree: raw {@code width}/{@code height}/
 * {@code align}/{@code flex} attribute strings become typed {@link Measurement}s and
 * {@link Alignment}s, and table column widths can be resolved including fraction distribution.
 *
 * <p>
 * The engine deliberately does NOT compute geometry: no coordinates, no pagination or
 * {@code pageBreak} handling, no text measurement or wrapping, no percent-of-what resolution (that
 * needs a concrete page width), no image loading, no Mustache evaluation and no
 * {@code if}/{@code for} expansion. The layout tree stays template-shaped; a concrete
 * {@code DocumentRenderer} adds geometry against a real page. Other attributes ({@code gap},
 * {@code padding}, {@code margin}, {@code style}, ...) stay raw and reachable via
 * {@code layoutNode.source().attributes()}.
 */
public final class LayoutEngine {

    /**
     * Creates an engine; instances are stateless and reusable.
     */
    public LayoutEngine() {}

    /**
     * Normalizes an AST into its layout tree.
     *
     * @param root the AST root
     * @return the layout tree root
     * @throws LayoutException when a {@code width}, {@code height}, {@code flex} or {@code align} value
     *         is invalid, pointing at the attribute's source position
     */
    public LayoutNode layout(Node root) {
        return layoutNode(root, false);
    }

    private LayoutNode layoutNode(Node node, boolean inFlexContainer) {
        Measurement width = measurement(node, "width");
        Measurement height = measurement(node, "height");
        if (inFlexContainer && width.type() == Measurement.Type.AUTO && node.attributes()
                                                                            .has("flex")) {
            // flex="2" on a row/column/stack child is shorthand for width="2*"; explicit width wins
            String flex = node.attributes()
                              .get("flex");
            try {
                width = Measurement.parse(flex.trim() + "*");
            } catch (IllegalArgumentException e) {
                throw new LayoutException("Invalid flex value: '" + flex + "'", node.attributes()
                                                                                    .positionOf("flex"));
            }
        }
        Alignment alignment;
        try {
            alignment = Alignment.parse(node.attributes()
                                            .get("align"));
        } catch (IllegalArgumentException e) {
            throw new LayoutException(e.getMessage(), node.attributes()
                                                          .positionOf("align"));
        }
        boolean childrenInFlexContainer = node instanceof RowNode || node instanceof ColumnNode || node instanceof StackNode;
        List<LayoutNode> children = new ArrayList<>(node.children()
                                                        .size());
        for (Node child : node.children()) {
            children.add(layoutNode(child, childrenInFlexContainer));
        }
        return new LayoutNode(node, width, height, alignment, children);
    }

    private Measurement measurement(Node node, String attribute) {
        try {
            return Measurement.parse(node.attributes()
                                         .get(attribute));
        } catch (IllegalArgumentException e) {
            throw new LayoutException(e.getMessage(), node.attributes()
                                                          .positionOf(attribute));
        }
    }

    /**
     * The widths of a table's column definitions in declaration order; a column without a {@code width}
     * defaults to {@code *} (fraction weight 1).
     *
     * @param table the table node
     * @return the column widths
     * @throws LayoutException when a column width is invalid
     */
    public static List<Measurement> resolveColumnWidths(TableNode table) {
        List<Measurement> widths = new ArrayList<>();
        for (Node child : table.children()) {
            if (!(child instanceof ColumnNode column)) {
                continue;
            }
            String raw = column.attributes()
                               .get("width");
            try {
                Measurement width = Measurement.parse(raw);
                widths.add(width.type() == Measurement.Type.AUTO ? Measurement.fraction(1) : width);
            } catch (IllegalArgumentException e) {
                throw new LayoutException(e.getMessage(), column.attributes()
                                                                .positionOf("width"));
            }
        }
        return widths;
    }

    /**
     * The normalized weight of every measurement in the list: fraction entries share proportionally
     * (e.g. {@code 4*, *, 2*, 2*} yields {@code 4/9, 1/9, 2/9, 2/9}); absolute, percent and auto
     * entries get weight {@code 0} — a renderer sizes them first and distributes the leftover space by
     * these weights.
     *
     * @param widths the column widths
     * @return the weights, same size and order as the input
     */
    public static double[] fractionShares(List<Measurement> widths) {
        double totalWeight = 0;
        for (Measurement width : widths) {
            if (width.type() == Measurement.Type.FRACTION) {
                totalWeight += width.value();
            }
        }
        double[] shares = new double[widths.size()];
        if (totalWeight == 0) {
            return shares;
        }
        for (int i = 0; i < widths.size(); i++) {
            Measurement width = widths.get(i);
            if (width.type() == Measurement.Type.FRACTION) {
                shares[i] = width.value() / totalWeight;
            }
        }
        return shares;
    }
}
