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

import org.eclipse.dirigible.parsers.document.Attributes;
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
import org.eclipse.dirigible.parsers.document.LinkTarget;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.PageNode;
import org.eclipse.dirigible.parsers.document.QrCodeNode;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.SectionNode;
import org.eclipse.dirigible.parsers.document.SpaceNode;
import org.eclipse.dirigible.parsers.document.StackNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.TextNode;
import org.eclipse.dirigible.parsers.document.TotalNode;
import org.eclipse.dirigible.parsers.document.layout.Alignment;
import org.eclipse.dirigible.parsers.document.layout.LayoutEngine;
import org.eclipse.dirigible.parsers.document.layout.LayoutException;
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
 *
 * <p>
 * An {@code href} on a {@code text}, an {@code image}, a {@code qrcode} or a table {@code column}
 * makes the element a link ({@code fo:basic-link}, a URI link annotation in the PDF). Its bound
 * value is validated here, after the data was merged, for every {@code href} - literal or built
 * from placeholders - so a value read from the data cannot choose a scheme the DSL refuses
 * ({@link LinkTarget}); a refused one is a {@link LayoutException} at the attribute.
 */
public final class XslFoRenderer implements DocumentRenderer<String> {

    private static final double DEFAULT_PAGE_WIDTH = 595;
    private static final double DEFAULT_PAGE_HEIGHT = 842;
    private static final double DEFAULT_PAGE_PADDING = 40;
    private static final String FONT_SIZE = "fontSize";
    private static final String SIZE = "size";
    private static final String LEVEL = "level";

    /** The side of a {@code <qrcode>} that sets no {@code size}, in points - about 32 mm. */
    private static final double DEFAULT_QR_CODE_SIZE = 90;

    /** Resolves an {@code <image>} source into something FOP can read. */
    private final ImageResolver imageResolver;

    /** Encodes a {@code <qrcode>} payload into an image FOP can read. */
    private final QrCodeEncoder qrCodeEncoder;

    /**
     * Creates a renderer that emits every {@code <image>} source unchanged; instances are stateless and
     * reusable.
     */
    public XslFoRenderer() {
        this(ImageResolver.PASS_THROUGH);
    }

    /**
     * Creates a renderer that resolves {@code <image>} sources through the given resolver — how a host
     * with a content store inlines the bytes of a logo or of a record's file. Instances are stateless
     * and reusable.
     *
     * @param imageResolver the image source resolver, never {@code null}
     */
    public XslFoRenderer(ImageResolver imageResolver) {
        this(imageResolver, QrCodeEncoder.NONE);
    }

    /**
     * Creates a renderer that also prints {@code <qrcode>} elements, encoded by the given encoder - the
     * library itself carries no QR implementation. Instances are stateless and reusable.
     *
     * @param imageResolver the image source resolver; {@code null} emits every source unchanged
     * @param qrCodeEncoder the QR code encoder; {@code null} renders no QR code
     */
    public XslFoRenderer(ImageResolver imageResolver, QrCodeEncoder qrCodeEncoder) {
        this.imageResolver = imageResolver == null ? ImageResolver.PASS_THROUGH : imageResolver;
        this.qrCodeEncoder = qrCodeEncoder == null ? QrCodeEncoder.NONE : qrCodeEncoder;
    }

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
                <fo:root font-family="DejaVu Sans, Helvetica" font-size="10pt">
                """);
        fo.append("<fo:layout-master-set><fo:simple-page-master master-name=\"page\" page-width=\"")
          .append(points(width))
          .append("\" page-height=\"")
          .append(points(height))
          .append("\" margin=\"")
          .append(points(padding))
          .append("\"><fo:region-body/></fo:simple-page-master></fo:layout-master-set>\n");
        fo.append("<fo:page-sequence master-reference=\"page\"><fo:flow flow-name=\"xsl-region-body\">\n");
        appendChildren(fo, page == null ? root : page, width - 2 * padding);
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

    /**
     * Emits the children of a node.
     *
     * @param width the width the children span, in points; {@link Double#NaN} where it is not known
     *        (inside a row), which leaves a table there to the column-count rule
     */
    private void appendChildren(StringBuilder fo, LayoutNode node, double width) {
        for (LayoutNode child : node.children()) {
            appendNode(fo, child, width);
        }
    }

    private void appendNode(StringBuilder fo, LayoutNode node, double width) {
        switch (node.source()) {
            case HeaderNode ignored -> appendBlockContainer(fo, node, width, "space-after=\"10pt\"");
            case FooterNode ignored -> appendBlockContainer(fo, node, width, "space-before=\"14pt\" font-size=\"8pt\" color=\"#666666\"");
            case SectionNode ignored -> appendBlockContainer(fo, node, width, "space-after=\"8pt\"");
            case StackNode ignored -> appendBlockContainer(fo, node, width, "");
            case ColumnNode ignored -> appendBlockContainer(fo, node, width, "");
            case TextNode text -> appendText(fo, node, text);
            case FieldNode field -> appendField(fo, node, field);
            case TotalNode total -> appendTotal(fo, node, total);
            case RowNode ignored -> appendRow(fo, node);
            case TableNode ignored -> appendTable(fo, node, width);
            case ImageNode image -> appendImage(fo, node, image);
            case QrCodeNode qrCode -> appendQrCode(fo, node, qrCode);
            case LineNode ignored -> fo.append(
                    "<fo:block space-before=\"4pt\" space-after=\"4pt\"><fo:leader leader-pattern=\"rule\" leader-length=\"100%\" rule-thickness=\"0.5pt\" color=\"#999999\"/></fo:block>\n");
            case SpaceNode ignored -> fo.append("<fo:block space-after=\"")
                                        .append(points(heightOr(node, 10)))
                                        .append("\"><fo:leader/></fo:block>\n");
            case DocumentNode ignored -> appendChildren(fo, node, width);
            case PageNode ignored -> appendChildren(fo, node, width);
            case IfNode ignored -> appendChildren(fo, node, width); // pre-expanded by the DataBinder
            case ForNode ignored -> appendChildren(fo, node, width); // pre-expanded by the DataBinder
            case CustomNode custom -> {
                appendTextBlock(fo, node, custom.text(), "", "");
                appendChildren(fo, node, width);
            }
        }
    }

    private void appendBlockContainer(StringBuilder fo, LayoutNode node, double width, String extraAttributes) {
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
        appendChildren(fo, node, width);
        fo.append("</fo:block>\n");
    }

    private void appendText(StringBuilder fo, LayoutNode node, TextNode text) {
        String style = text.attributes()
                           .getOrDefault("style", "");
        String styleAttributes = switch (style) {
            case "title" -> " font-size=\"18pt\" font-weight=\"bold\"";
            case "subtitle" -> " font-size=\"12pt\" color=\"#444444\"";
            case "caption" -> " font-size=\"9pt\" font-weight=\"bold\" color=\"#666666\"";
            case "link" -> " color=\"#0563C1\" text-decoration=\"underline\"";
            default -> "";
        };
        appendTextBlock(fo, node, text.text(), styleAttributes, linkTarget(text.attributes()));
    }

    private void appendTextBlock(StringBuilder fo, LayoutNode node, String text, String styleAttributes, String linkTarget) {
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(styleAttributes)
          .append(">")
          .append(linked(linkTarget, escape(text)))
          .append("</fo:block>\n");
        appendChildren(fo, node, Double.NaN);
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
            appendNode(fo, child, Double.NaN);
            fo.append("</fo:table-cell>\n");
        }
        fo.append("</fo:table-row></fo:table-body></fo:table>\n");
    }

    /**
     * A data table: column definitions carry widths and optional {@code label}s (a header row is
     * emitted when any label is present); the {@code row} children are the data rows.
     */
    private void appendTable(StringBuilder fo, LayoutNode node, double width) {
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
        List<Measurement> widths = new ArrayList<>();
        for (LayoutNode column : columns) {
            widths.add(column.width()
                             .type() == Measurement.Type.AUTO ? Measurement.fraction(1) : column.width());
        }
        fo.append("<fo:table table-layout=\"fixed\" width=\"100%\" space-after=\"8pt\" font-size=\"")
          .append(points(tableFontSize(node, width, widths, columns, rows)))
          .append("\">\n");
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
                  .append(linked(linkTarget(cell.source()
                                                .attributes()),
                          escape(cell.source()
                                     .text())))
                  .append("</fo:block></fo:table-cell>\n");
            }
            fo.append("</fo:table-row>\n");
        }
        fo.append("</fo:table-body></fo:table>\n");
    }

    /**
     * The table's {@code fontSize} in points when the template sets one, else the automatic size: the
     * body size when the columns' content fits, else the largest size at which it does, never below 8pt
     * ({@link TableFontSizer}, #8321). Where the table's width is not known (inside a row) the size
     * drops a point for every column beyond three instead, to the same floor.
     */
    private static double tableFontSize(LayoutNode table, double width, List<Measurement> widths, List<LayoutNode> columns,
            List<LayoutNode> rows) {
        Attributes attributes = table.source()
                                     .attributes();
        if (!attributes.has(FONT_SIZE)) {
            if (Double.isNaN(width)) {
                return Math.max(TableFontSizer.MIN_SIZE, TableFontSizer.MAX_SIZE - Math.max(0, columns.size() - 3));
            }
            List<String> labels = columns.stream()
                                         .map(column -> column.source()
                                                              .attributes()
                                                              .getOrDefault("label", ""))
                                         .toList();
            List<List<String>> cells = rows.stream()
                                           .map(row -> row.children()
                                                          .stream()
                                                          .map(cell -> cell.source()
                                                                           .text())
                                                          .toList())
                                           .toList();
            return TableFontSizer.fit(width, widths, labels, cells);
        }
        return positivePoints(attributes, FONT_SIZE);
    }

    /**
     * An attribute holding a positive number of points ({@code 9}, {@code 9pt} and {@code 9px} are the
     * same); anything else is a layout error at the attribute's position.
     */
    private static double positivePoints(Attributes attributes, String name) {
        String raw = attributes.get(name);
        String amount = raw.trim();
        if (amount.endsWith("pt")) {
            amount = amount.substring(0, amount.length() - 2);
        }
        Measurement size;
        try {
            size = Measurement.parse(amount);
        } catch (IllegalArgumentException e) {
            throw invalidPoints(name, raw, attributes);
        }
        if (size.type() != Measurement.Type.ABSOLUTE_PX || size.value() <= 0) {
            throw invalidPoints(name, raw, attributes);
        }
        return size.value();
    }

    private static LayoutException invalidPoints(String name, String raw, Attributes attributes) {
        return new LayoutException("Invalid " + name + ": '" + raw + "' (expected a positive number of points)",
                attributes.positionOf(name));
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

    /**
     * Renders one image. An absent, blank or unresolvable source renders <b>nothing</b> — no block, no
     * placeholder: a printed business document with no logo is correct output, a broken-image box is
     * not. {@code width}/{@code height} are the resize hints; each is emitted only when it is an
     * absolute measurement, so a single one scales the image proportionally.
     */
    private void appendImage(StringBuilder fo, LayoutNode node, ImageNode image) {
        String linkTarget = linkTarget(image.attributes());
        String src = image.attributes()
                          .getOrDefault("src", "");
        if (src.isBlank()) {
            return;
        }
        String resolved = imageResolver.resolve(src);
        if (resolved == null || resolved.isBlank()) {
            return;
        }
        StringBuilder graphic = new StringBuilder("<fo:external-graphic src=\"").append(escape(resolved))
                                                                                .append("\"");
        appendContentDimension(graphic, "content-width", node.width());
        appendContentDimension(graphic, "content-height", node.height());
        graphic.append("/>");
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(">")
          .append(linked(linkTarget, graphic.toString()))
          .append("</fo:block>\n");
    }

    /**
     * Renders one QR code as a square of {@code size} points. A blank payload - a {@code value} or text
     * whose placeholders all resolved empty - and a payload the encoder declines render <b>nothing</b>,
     * exactly as an unreadable image does. {@code size} and {@code level} are the template author's own
     * words, so a wrong one is a layout error rather than a silently missing code.
     */
    private void appendQrCode(StringBuilder fo, LayoutNode node, QrCodeNode qrCode) {
        String side = points(qrCodeSize(qrCode.attributes()));
        QrCodeLevel level = qrCodeLevel(qrCode.attributes());
        String linkTarget = linkTarget(qrCode.attributes());
        String payload = qrCode.payload();
        if (payload.isBlank()) {
            return;
        }
        String encoded = qrCodeEncoder.encode(payload, level);
        if (encoded == null || encoded.isBlank()) {
            return;
        }
        fo.append("<fo:block")
          .append(alignAttribute(node))
          .append(">")
          .append(linked(linkTarget,
                  "<fo:external-graphic src=\"" + escape(encoded) + "\" content-width=\"" + side + "\" content-height=\"" + side + "\"/>"))
          .append("</fo:block>\n");
    }

    private static double qrCodeSize(Attributes attributes) {
        return attributes.getOrDefault(SIZE, "")
                         .isBlank() ? DEFAULT_QR_CODE_SIZE : positivePoints(attributes, SIZE);
    }

    private static QrCodeLevel qrCodeLevel(Attributes attributes) {
        try {
            return QrCodeLevel.parse(attributes.get(LEVEL));
        } catch (IllegalArgumentException e) {
            throw new LayoutException(e.getMessage(), attributes.positionOf(LEVEL));
        }
    }

    /**
     * The link an element's {@code href} opens, as the ASCII URI to emit; empty when the element is no
     * link - no {@code href}, or one that is blank or only its scheme after binding. A target of any
     * other kind than {@code http://}, {@code https://}, {@code mailto:} and {@code tel:} is a layout
     * error at the attribute, wherever its value came from.
     */
    private static String linkTarget(Attributes attributes) {
        try {
            return LinkTarget.parse(attributes.get(LinkTarget.ATTRIBUTE));
        } catch (IllegalArgumentException e) {
            throw new LayoutException(e.getMessage(), attributes.positionOf(LinkTarget.ATTRIBUTE));
        }
    }

    /**
     * The content wrapped in the link to the target, or as it is when there is no target or nothing to
     * click. The target is already a plain ASCII URI ({@link LinkTarget}) - no quote, parenthesis or
     * brace is left in it to end the {@code url('...')} notation or to open an XSLT attribute value
     * template - so only its {@code &} needs escaping.
     */
    private static String linked(String linkTarget, String content) {
        if (linkTarget.isEmpty() || content.isEmpty()) {
            return content;
        }
        return "<fo:basic-link external-destination=\"url('" + escape(linkTarget) + "')\">" + content + "</fo:basic-link>";
    }

    private static void appendContentDimension(StringBuilder fo, String attribute, Measurement measurement) {
        if (measurement.type() != Measurement.Type.ABSOLUTE_PX) {
            return;
        }
        fo.append(" ")
          .append(attribute)
          .append("=\"")
          .append(points(measurement.value()))
          .append("\"");
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
