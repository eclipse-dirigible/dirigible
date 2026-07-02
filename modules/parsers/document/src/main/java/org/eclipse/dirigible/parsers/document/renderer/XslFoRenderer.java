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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.parsers.document.ColumnNode;
import org.eclipse.dirigible.parsers.document.CustomNode;
import org.eclipse.dirigible.parsers.document.DocumentNode;
import org.eclipse.dirigible.parsers.document.FieldNode;
import org.eclipse.dirigible.parsers.document.FooterNode;
import org.eclipse.dirigible.parsers.document.ForNode;
import org.eclipse.dirigible.parsers.document.HeaderNode;
import org.eclipse.dirigible.parsers.document.IfNode;
import org.eclipse.dirigible.parsers.document.ImageNode;
import org.eclipse.dirigible.parsers.document.LineNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.PageNode;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.SectionNode;
import org.eclipse.dirigible.parsers.document.SpaceNode;
import org.eclipse.dirigible.parsers.document.StackNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.TextNode;
import org.eclipse.dirigible.parsers.document.TotalNode;
import org.eclipse.dirigible.parsers.document.layout.Alignment;
import org.eclipse.dirigible.parsers.document.layout.LayoutEngine;
import org.eclipse.dirigible.parsers.document.layout.LayoutNode;
import org.eclipse.dirigible.parsers.document.layout.Measurement;

/**
 * Renders a data-bound layout tree into a self-contained XSLT stylesheet with literal XSL-FO — the
 * exact input the platform's FOP-based PDF facade transforms into a PDF (the stylesheet matches any
 * data document, e.g. {@code <data/>}, because all data was already merged by the
 * {@code DataBinder}).
 *
 * <p>
 * Deliberate v1 simplifications: {@code header}/{@code footer} render once in the flow (not as
 * repeated page regions), {@code repeatHeader} and {@code pageBreak} are ignored, a data-less
 * {@code table} is skipped entirely (FOP rejects an empty table body), and measurements map
 * 1&nbsp;px&nbsp;=&nbsp;1&nbsp;pt.
 */
public final class XslFoRenderer implements DocumentRenderer<String> {

    private static final double DEFAULT_PAGE_WIDTH = 595;
    private static final double DEFAULT_PAGE_HEIGHT = 842;
    private static final double DEFAULT_PAGE_PADDING = 40;

    /**
     * Creates a renderer; instances are stateless and reusable.
     */
    public XslFoRenderer() {}

    @Override
    public String render(LayoutNode root) {
        LayoutNode page = findPage(root);
        double width = pageDimension(page, "width", DEFAULT_PAGE_WIDTH);
        double height = pageDimension(page, "height", DEFAULT_PAGE_HEIGHT);
        double padding = pageDimension(page, "padding", DEFAULT_PAGE_PADDING);

        StringBuilder fo = new StringBuilder(8192);
        fo.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
                <xsl:template match="/">
                <fo:root font-family="Helvetica" font-size="10pt">
                """);
        fo.append("<fo:layout-master-set><fo:simple-page-master master-name=\"page\" page-width=\"")
          .append(points(width))
          .append("\" page-height=\"")
          .append(points(height))
          .append("\" margin=\"")
          .append(points(padding))
          .append("\"><fo:region-body/></fo:simple-page-master></fo:layout-master-set>\n");
        fo.append("<fo:page-sequence master-reference=\"page\"><fo:flow flow-name=\"xsl-region-body\">\n");
        appendChildren(fo, page == null ? root : page);
        fo.append("</fo:flow></fo:page-sequence>\n");
        fo.append("</fo:root>\n</xsl:template>\n</xsl:stylesheet>\n");
        return fo.toString();
    }

    private static LayoutNode findPage(LayoutNode root) {
        if (root.source() instanceof PageNode) {
            return root;
        }
        for (LayoutNode child : root.children()) {
            if (child.source() instanceof PageNode) {
                return child;
            }
        }
        return null;
    }

    private static double pageDimension(LayoutNode page, String attribute, double fallback) {
        if (page == null) {
            return fallback;
        }
        String raw = page.source()
                         .attributes()
                         .get(attribute);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Measurement measurement = Measurement.parse(raw);
        return measurement.type() == Measurement.Type.ABSOLUTE_PX ? measurement.value() : fallback;
    }

    private void appendChildren(StringBuilder fo, LayoutNode node) {
        for (LayoutNode child : node.children()) {
            appendNode(fo, child);
        }
    }

    private void appendNode(StringBuilder fo, LayoutNode node) {
        switch (node.source()) {
            case HeaderNode ignored -> appendBlockContainer(fo, node, "space-after=\"10pt\"");
            case FooterNode ignored -> appendBlockContainer(fo, node, "space-before=\"14pt\" font-size=\"8pt\" color=\"#666666\"");
            case SectionNode ignored -> appendBlockContainer(fo, node, "space-after=\"8pt\"");
            case StackNode ignored -> appendBlockContainer(fo, node, "");
            case ColumnNode ignored -> appendBlockContainer(fo, node, "");
            case TextNode text -> appendText(fo, node, text);
            case FieldNode field -> appendField(fo, node, field);
            case TotalNode total -> appendTotal(fo, node, total);
            case RowNode ignored -> appendRow(fo, node);
            case TableNode ignored -> appendTable(fo, node);
            case ImageNode image -> appendImage(fo, node, image);
            case LineNode ignored -> fo.append(
                    "<fo:block space-before=\"4pt\" space-after=\"4pt\"><fo:leader leader-pattern=\"rule\" leader-length=\"100%\" rule-thickness=\"0.5pt\" color=\"#999999\"/></fo:block>\n");
            case SpaceNode ignored -> fo.append("<fo:block space-after=\"")
                                        .append(points(heightOr(node, 10)))
                                        .append("\"><fo:leader/></fo:block>\n");
            case DocumentNode ignored -> appendChildren(fo, node);
            case PageNode ignored -> appendChildren(fo, node);
            case IfNode ignored -> appendChildren(fo, node); // pre-expanded by the DataBinder
            case ForNode ignored -> appendChildren(fo, node); // pre-expanded by the DataBinder
            case CustomNode custom -> {
                appendTextBlock(fo, node, custom.text(), "");
                appendChildren(fo, node);
            }
        }
    }

    private void appendBlockContainer(StringBuilder fo, LayoutNode node, String extraAttributes) {
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(extraAttributes.isEmpty() ? "" : " " + extraAttributes)
          .append(">\n");
        if (!node.source()
                 .text()
                 .isEmpty()) {
            fo.append("<fo:block>")
              .append(escape(node.source()
                                 .text()))
              .append("</fo:block>\n");
        }
        appendChildren(fo, node);
        fo.append("</fo:block>\n");
    }

    private void appendText(StringBuilder fo, LayoutNode node, TextNode text) {
        String style = text.attributes()
                           .getOrDefault("style", "");
        String styleAttributes = switch (style) {
            case "title" -> " font-size=\"18pt\" font-weight=\"bold\"";
            case "subtitle" -> " font-size=\"12pt\" color=\"#444444\"";
            case "caption" -> " font-size=\"9pt\" font-weight=\"bold\" color=\"#666666\"";
            default -> "";
        };
        appendTextBlock(fo, node, text.text(), styleAttributes);
    }

    private void appendTextBlock(StringBuilder fo, LayoutNode node, String text, String styleAttributes) {
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(styleAttributes)
          .append(">")
          .append(escape(text))
          .append("</fo:block>\n");
        appendChildren(fo, node);
    }

    private void appendField(StringBuilder fo, LayoutNode node, FieldNode field) {
        String label = field.attributes()
                            .getOrDefault("label", "");
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(">");
        if (!label.isEmpty()) {
            fo.append("<fo:inline font-weight=\"bold\">")
              .append(escape(label))
              .append(": </fo:inline>");
        }
        fo.append(escape(field.text()))
          .append("</fo:block>\n");
    }

    private void appendTotal(StringBuilder fo, LayoutNode node, TotalNode total) {
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(" font-size=\"12pt\" font-weight=\"bold\" space-before=\"6pt\">")
          .append(escape(total.text()))
          .append("</fo:block>\n");
    }

    /** A free-standing row is a single-row table whose columns come from the children's widths. */
    private void appendRow(StringBuilder fo, LayoutNode node) {
        if (node.children()
                .isEmpty()) {
            return;
        }
        fo.append("<fo:table table-layout=\"fixed\" width=\"100%\">\n");
        List<Measurement> widths = new ArrayList<>();
        for (LayoutNode child : node.children()) {
            widths.add(child.width());
        }
        appendColumnDefinitions(fo, widths);
        fo.append("<fo:table-body><fo:table-row>\n");
        for (LayoutNode child : node.children()) {
            fo.append("<fo:table-cell>");
            appendNode(fo, child);
            fo.append("</fo:table-cell>\n");
        }
        fo.append("</fo:table-row></fo:table-body></fo:table>\n");
    }

    /**
     * A data table: column definitions carry widths and optional {@code label}s (a header row is
     * emitted when any label is present); the {@code row} children are the data rows.
     */
    private void appendTable(StringBuilder fo, LayoutNode node) {
        List<LayoutNode> columns = new ArrayList<>();
        List<LayoutNode> rows = new ArrayList<>();
        for (LayoutNode child : node.children()) {
            if (child.source() instanceof ColumnNode) {
                columns.add(child);
            } else if (child.source() instanceof RowNode) {
                rows.add(child);
            }
        }
        if (rows.isEmpty()) {
            return; // FOP rejects an empty fo:table-body
        }
        fo.append("<fo:table table-layout=\"fixed\" width=\"100%\" space-after=\"8pt\">\n");
        List<Measurement> widths = new ArrayList<>();
        for (LayoutNode column : columns) {
            widths.add(column.width()
                             .type() == Measurement.Type.AUTO ? Measurement.fraction(1) : column.width());
        }
        appendColumnDefinitions(fo, widths);
        boolean hasLabels = columns.stream()
                                   .anyMatch(column -> column.source()
                                                             .attributes()
                                                             .has("label"));
        if (hasLabels) {
            fo.append("<fo:table-header><fo:table-row border-bottom=\"0.5pt solid #999999\">\n");
            for (LayoutNode column : columns) {
                fo.append("<fo:table-cell padding=\"2pt\"><fo:block font-weight=\"bold\"")
                  .append(alignAttribute(column))
                  .append(">")
                  .append(escape(column.source()
                                       .attributes()
                                       .getOrDefault("label", "")))
                  .append("</fo:block></fo:table-cell>\n");
            }
            fo.append("</fo:table-row></fo:table-header>\n");
        }
        fo.append("<fo:table-body>\n");
        for (LayoutNode row : rows) {
            fo.append("<fo:table-row>\n");
            for (LayoutNode cell : row.children()) {
                fo.append("<fo:table-cell padding=\"2pt\"><fo:block")
                  .append(alignAttribute(cell))
                  .append(">")
                  .append(escape(cell.source()
                                     .text()))
                  .append("</fo:block></fo:table-cell>\n");
            }
            fo.append("</fo:table-row>\n");
        }
        fo.append("</fo:table-body></fo:table>\n");
    }

    private void appendColumnDefinitions(StringBuilder fo, List<Measurement> widths) {
        for (Measurement width : widths) {
            fo.append("<fo:table-column column-width=\"")
              .append(switch (width.type()) {
                  case ABSOLUTE_PX -> points(width.value());
                  case PERCENT -> trim(width.value()) + "%";
                  case FRACTION -> "proportional-column-width(" + trim(width.value()) + ")";
                  case AUTO -> "proportional-column-width(1)";
              })
              .append("\"/>\n");
        }
    }

    private void appendImage(StringBuilder fo, LayoutNode node, ImageNode image) {
        String src = image.attributes()
                          .getOrDefault("src", "");
        if (src.isEmpty()) {
            return;
        }
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append("><fo:external-graphic src=\"")
          .append(escape(src))
          .append("\"");
        if (node.width()
                .type() == Measurement.Type.ABSOLUTE_PX) {
            fo.append(" content-width=\"")
              .append(points(node.width()
                                 .value()))
              .append("\"");
        }
        fo.append("/></fo:block>\n");
    }

    private static String alignAttribute(LayoutNode node) {
        if (node.alignment() == Alignment.LEFT) {
            return "";
        }
        return " text-align=\"" + node.alignment()
                                      .name()
                                      .toLowerCase()
                + "\"";
    }

    private static double heightOr(LayoutNode node, double fallback) {
        return node.height()
                   .type() == Measurement.Type.ABSOLUTE_PX ? node.height()
                                                                 .value()
                           : fallback;
    }

    private static String points(double value) {
        return trim(value) + "pt";
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    /**
     * Convenience for the common pipeline: normalize the bound AST and render it.
     *
     * @param boundRoot the data-bound AST (output of the {@code DataBinder})
     * @return the XSL-FO stylesheet
     */
    public String renderBound(Node boundRoot) {
        return render(new LayoutEngine().layout(boundRoot));
    }
}
