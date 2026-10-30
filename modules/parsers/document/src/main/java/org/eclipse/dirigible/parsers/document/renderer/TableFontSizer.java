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

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.dirigible.parsers.document.layout.Measurement;

/**
 * Picks the automatic font size of a data table from what its columns hold (#8321): the body size
 * when every column's content fits its column, else the largest size at which it does, never below
 * {@link #MIN_SIZE}. FOP bleeds an overflowing (right-aligned) line over its neighbour instead of
 * clipping it, so what has to fit is each cell's longest unbreakable run: a figure as a whole (its
 * grouping spaces must not wrap it), a word of any other text, a word of a bold header label.
 *
 * <p>
 * Text is measured with an estimate of the advance widths of DejaVu Sans, the font the stylesheet
 * names first, by character class - the parser library carries no font metrics. The estimate leans
 * wide, so a size it accepts fits.
 */
final class TableFontSizer {

    /** The document body size, and the largest a table is given automatically. */
    static final double MAX_SIZE = 10;

    /** The smallest size a table is given automatically - still readable on paper. */
    static final double MIN_SIZE = 8;

    /** The horizontal padding of a table cell, both sides together, in points. */
    private static final double CELL_PADDING = 4;

    /** Header labels are bold, which DejaVu Sans sets about a tenth wider. */
    private static final double BOLD_FACTOR = 1.1;

    /** The sizes are offered in half points. */
    private static final double STEP = 0.5;

    /** A figure: digits with grouping spaces, separators, a sign or a percent - printed unbroken. */
    private static final Pattern FIGURE = Pattern.compile("[-+]?[\\d\\s.,]*\\d[\\d\\s.,]*%?");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TableFontSizer() {}

    /**
     * The automatic font size of a table.
     *
     * @param tableWidth the width the table spans, in points
     * @param widths the column widths, one per column, none {@code AUTO}
     * @param labels the header labels, one per column, empty where a column has none
     * @param rows the cell texts of each data row
     * @return the size in points, between {@link #MIN_SIZE} and {@link #MAX_SIZE}
     */
    static double fit(double tableWidth, List<Measurement> widths, List<String> labels, List<List<String>> rows) {
        double[] columnWidths = resolve(tableWidth, widths);
        double size = MAX_SIZE;
        for (int column = 0; column < columnWidths.length; column++) {
            double ems = longestWordEms(labels.get(column)) * BOLD_FACTOR;
            for (List<String> row : rows) {
                if (column < row.size()) {
                    ems = Math.max(ems, unbreakableEms(row.get(column)));
                }
            }
            if (ems > 0) {
                size = Math.min(size, (columnWidths[column] - CELL_PADDING) / ems);
            }
        }
        return Math.max(MIN_SIZE, Math.floor(size / STEP) * STEP);
    }

    /** Column widths in points: absolute and percent widths first, fractions share what is left. */
    private static double[] resolve(double tableWidth, List<Measurement> widths) {
        double[] resolved = new double[widths.size()];
        double fixed = 0;
        double weights = 0;
        for (int i = 0; i < resolved.length; i++) {
            Measurement width = widths.get(i);
            switch (width.type()) {
                case ABSOLUTE_PX -> resolved[i] = width.value();
                case PERCENT -> resolved[i] = tableWidth * width.value() / 100;
                case FRACTION -> weights += width.value();
                case AUTO -> throw new IllegalArgumentException("An AUTO column width reached the table sizer");
            }
            fixed += resolved[i];
        }
        double remaining = Math.max(0, tableWidth - fixed);
        for (int i = 0; i < resolved.length; i++) {
            Measurement width = widths.get(i);
            if (width.type() == Measurement.Type.FRACTION && weights > 0) {
                resolved[i] = remaining * width.value() / weights;
            }
        }
        return resolved;
    }

    private static double unbreakableEms(String text) {
        String trimmed = text.strip();
        return FIGURE.matcher(trimmed)
                     .matches() ? ems(trimmed) : longestWordEms(trimmed);
    }

    private static double longestWordEms(String text) {
        double longest = 0;
        for (String word : WHITESPACE.split(text.strip())) {
            longest = Math.max(longest, ems(word));
        }
        return longest;
    }

    /** The estimated advance width of a text in ems of DejaVu Sans. */
    private static double ems(String text) {
        double ems = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || ".,:;'|!".indexOf(c) >= 0) {
                ems += 0.32;
            } else if (Character.isDigit(c)) {
                ems += 0.64;
            } else if (Character.isUpperCase(c) || c == '%' || c == '@' || c == '№') {
                ems += 0.75;
            } else {
                ems += 0.62;
            }
        }
        return ems;
    }
}
