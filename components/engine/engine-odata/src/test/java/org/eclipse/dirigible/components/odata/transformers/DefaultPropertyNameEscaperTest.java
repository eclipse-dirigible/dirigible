/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Class DefaultPropertyNameEscaperTest.
 */
@ExtendWith(MockitoExtension.class)
public class DefaultPropertyNameEscaperTest {

    /** The escaper. */
    private DefaultPropertyNameEscaper escaper;

    /**
     * Sets the up.
     */
    @BeforeEach
    public void setUp() {
        this.escaper = new DefaultPropertyNameEscaper();
    }

    /**
     * Test escape dots.
     */
    @Test
    public void testEscapeDots() {
        assertEquals("Property_Name_With_Dots", escaper.escape("Property.Name.With.Dots"), "Unexpected escaped property name");
    }

    /**
     * Test escape dot.
     */
    @Test
    public void testEscapeDot() {
        assertEquals("Property_Name", escaper.escape("Property.Name"), "Unexpected escaped property name");
    }

    /**
     * Test escape valid name.
     */
    @Test
    public void testEscapeValidName() {
        assertEquals("PropertyName", escaper.escape("PropertyName"), "Unexpected escaped property name");
    }

}
