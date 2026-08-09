/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the mapping from a model property's SQL type onto the type names the generated artefacts
 * need.
 */
class ModelDataTypesTest {

    @Test
    void mapsIntegerFamilies() {
        assertEquals(new ModelDataTypes.DataType("short", "number", "Short"), ModelDataTypes.parse("SMALLINT"));
        assertEquals(new ModelDataTypes.DataType("int", "number", "Integer"), ModelDataTypes.parse("INTEGER"));
        assertEquals(new ModelDataTypes.DataType("long", "number", "Long"), ModelDataTypes.parse("BIGINT"));
    }

    @Test
    void mapsDecimalToBigDecimalAndDoubleToDouble() {
        assertEquals("java.math.BigDecimal", ModelDataTypes.parse("DECIMAL")
                                                           .javaClass());
        assertEquals("Double", ModelDataTypes.parse("DOUBLE")
                                             .javaClass());
        assertEquals("Float", ModelDataTypes.parse("FLOAT")
                                            .javaClass());
    }

    @Test
    void mapsTextFamiliesToString() {
        assertEquals("String", ModelDataTypes.parse("VARCHAR")
                                             .javaClass());
        assertEquals("String", ModelDataTypes.parse("CLOB")
                                             .javaClass());
        assertEquals("String", ModelDataTypes.parse("character varying")
                                             .javaClass());
    }

    @Test
    void mapsTemporalFamilies() {
        assertEquals(new ModelDataTypes.DataType("date", "Date", "java.time.LocalDate"), ModelDataTypes.parse("DATE"));
        assertEquals(new ModelDataTypes.DataType("time", "string", "java.time.LocalTime"), ModelDataTypes.parse("TIME"));
        assertEquals(new ModelDataTypes.DataType("timestamp", "Date", "java.time.Instant"), ModelDataTypes.parse("TIMESTAMP"));
    }

    @Test
    void fallsBackToAnUnknownObjectForAnUnrecognisedOrAbsentType() {
        assertEquals(new ModelDataTypes.DataType("", "unknown", "Object"), ModelDataTypes.parse("GEOGRAPHY"));
        assertEquals(new ModelDataTypes.DataType("", "unknown", "Object"), ModelDataTypes.parse(null));
    }

    @Test
    void anAuditTimestampIsAlwaysAnInstantWhateverTheColumnDeclares() {
        assertEquals("java.time.Instant", ModelDataTypes.resolveJavaClass("String", "CREATED_AT"));
        assertEquals("java.time.Instant", ModelDataTypes.resolveJavaClass("String", "UPDATED_AT"));
    }

    @Test
    void anAuditUserIsAlwaysAString() {
        assertEquals("String", ModelDataTypes.resolveJavaClass("Integer", "CREATED_BY"));
        assertEquals("String", ModelDataTypes.resolveJavaClass("Integer", "UPDATED_BY"));
    }

    @Test
    void aNonAuditColumnKeepsItsOwnClass() {
        assertEquals("Integer", ModelDataTypes.resolveJavaClass("Integer", "NONE"));
        assertEquals("Integer", ModelDataTypes.resolveJavaClass("Integer", null));
        assertEquals("Object", ModelDataTypes.resolveJavaClass(null, null));
        assertEquals("Object", ModelDataTypes.resolveJavaClass("", null));
    }

}
