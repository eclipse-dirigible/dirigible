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

import java.util.List;

import org.eclipse.dirigible.parsers.document.layout.Measurement;
import org.junit.Test;

public class TableFontSizerTest {

    private static final double DELTA = 0.0001;

    @Test
    public void shortContentKeepsTheBodySize() {
        assertEquals(10, TableFontSizer.fit(515, List.of(Measurement.fraction(1), Measurement.fraction(1)), List.of("Qty", "Total"),
                List.of(List.of("12", "1 254.00"))), DELTA);
    }

    @Test
    public void longTextWrapsAtItsSpacesButAFigureDoesNot() {
        // the description wraps word by word; the figure of the same length has to fit as a whole
        List<Measurement> widths = List.of(Measurement.px(60), Measurement.px(60));
        assertEquals(10, TableFontSizer.fit(120, widths, List.of("", ""), List.of(List.of("one two three four five six", "1"))), DELTA);
        assertEquals(8, TableFontSizer.fit(120, widths, List.of("", ""), List.of(List.of("x", "1 234 567 890.00"))), DELTA);
    }

    @Test
    public void percentAndPixelWidthsAreTakenBeforeFractionsShareTheRest() {
        // 200pt table: 100pt fixed, 50% = 100pt, so the fraction column is left 0pt and the floor applies
        List<Measurement> widths = List.of(Measurement.px(100), Measurement.percent(50), Measurement.fraction(1));
        assertEquals(8, TableFontSizer.fit(200, widths, List.of("", "", ""), List.of(List.of("1", "1", "12"))), DELTA);
        // the same figures in the fixed columns fit
        assertEquals(10, TableFontSizer.fit(200, widths, List.of("", "", ""), List.of(List.of("12 345", "12 345", ""))), DELTA);
    }

    @Test
    public void aLongHeaderWordCountsBold() {
        // "Quantity" in bold needs ~5.6em: 52pt of text room fits it at 9pt, not at 10pt
        assertEquals(9, TableFontSizer.fit(56, List.of(Measurement.px(56)), List.of("Quantity"), List.of(List.of("1"))), DELTA);
    }
}
