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
import static org.junit.Assert.fail;

import org.junit.Test;

public class AlignmentTest {

    @Test
    public void parsesAllValuesCaseInsensitively() {
        assertEquals(Alignment.LEFT, Alignment.parse("left"));
        assertEquals(Alignment.CENTER, Alignment.parse("Center"));
        assertEquals(Alignment.RIGHT, Alignment.parse("RIGHT"));
        assertEquals(Alignment.JUSTIFY, Alignment.parse("justify"));
    }

    @Test
    public void missingValueDefaultsToLeft() {
        assertEquals(Alignment.LEFT, Alignment.parse(null));
        assertEquals(Alignment.LEFT, Alignment.parse(""));
        assertEquals(Alignment.LEFT, Alignment.parse("   "));
    }

    @Test
    public void rejectsUnknownValues() {
        try {
            Alignment.parse("middle");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid alignment: 'middle'", e.getMessage());
        }
    }
}
