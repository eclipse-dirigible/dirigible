/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Reading the JSON a process is started and a task is completed with (issue #7373).
 */
class ProcessVariablesTest {

    /**
     * The defect itself: the record's key reached the variable store as a Double, so every reader that
     * renders it as text said "33.0" - which the generated controller's integer path parameter refuses.
     */
    @Test
    void aWholeNumberStaysWhole() {
        Map<String, Object> variables = ProcessVariables.fromJson("{\"__entityId\": 33, \"Id\": 33}");

        assertEquals(33L, variables.get("__entityId"));
        assertEquals("33", String.valueOf(variables.get("Id")));
    }

    /** A genuine decimal keeps its fraction - only the widening of an integer was wrong. */
    @Test
    void aDecimalStaysADecimal() {
        Map<String, Object> variables = ProcessVariables.fromJson("{\"Total\": 12.5, \"Name\": \"ACME\", \"Draft\": true}");

        assertEquals(12.5d, variables.get("Total"));
        assertEquals("ACME", variables.get("Name"));
        assertEquals(Boolean.TRUE, variables.get("Draft"));
    }

    /** A start with no parameters at all is a start with no variables, as before. */
    @Test
    void noDocumentIsNoVariables() {
        assertNull(ProcessVariables.fromJson(null));
    }
}
