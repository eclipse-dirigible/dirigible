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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class MeasurementTest {

    private static void assertMeasurement(Measurement.Type type, double value, String raw) {
        Measurement measurement = Measurement.parse(raw);
        assertEquals(type, measurement.type());
        assertEquals(value, measurement.value(), 0.000001);
    }

    @Test
    public void parsesAbsolutePixels() {
        assertMeasurement(Measurement.Type.ABSOLUTE_PX, 100, "100");
        assertMeasurement(Measurement.Type.ABSOLUTE_PX, 100, "100px");
        assertMeasurement(Measurement.Type.ABSOLUTE_PX, 12.5, "12.5");
        assertMeasurement(Measurement.Type.ABSOLUTE_PX, 0, "0");
    }

    @Test
    public void parsesPercent() {
        assertMeasurement(Measurement.Type.PERCENT, 50, "50%");
        assertMeasurement(Measurement.Type.PERCENT, 33.3, "33.3%");
    }

    @Test
    public void parsesFractions() {
        assertMeasurement(Measurement.Type.FRACTION, 1, "*");
        assertMeasurement(Measurement.Type.FRACTION, 2, "2*");
        assertMeasurement(Measurement.Type.FRACTION, 3, "3*");
        assertMeasurement(Measurement.Type.FRACTION, 1.5, "1.5*");
    }

    @Test
    public void parsesAuto() {
        assertSame(Measurement.AUTO, Measurement.parse(null));
        assertSame(Measurement.AUTO, Measurement.parse(""));
        assertSame(Measurement.AUTO, Measurement.parse("  "));
        assertSame(Measurement.AUTO, Measurement.parse("auto"));
        assertSame(Measurement.AUTO, Measurement.parse("AUTO"));
    }

    @Test
    public void toleratesSurroundingWhitespace() {
        assertMeasurement(Measurement.Type.PERCENT, 50, " 50% ");
    }

    @Test
    public void rejectsInvalidValues() {
        for (String invalid : new String[] {"10em", "px", "-5", "*2", "5%%", "abc", "1e3", "1d", ".", "1.2.3", "%"}) {
            try {
                Measurement.parse(invalid);
                fail("Expected IllegalArgumentException for '" + invalid + "'");
            } catch (IllegalArgumentException e) {
                assertTrue("Message should name the raw input: " + e.getMessage(), e.getMessage()
                                                                                    .contains(invalid.trim()));
            }
        }
    }

    @Test
    public void rejectsNegativeConstruction() {
        try {
            Measurement.px(-1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
