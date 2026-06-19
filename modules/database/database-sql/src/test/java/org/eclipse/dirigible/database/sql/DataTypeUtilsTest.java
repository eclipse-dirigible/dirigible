/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Types;

import org.junit.Test;

/**
 * The Class DataTypeUtilsTest.
 */
public class DataTypeUtilsTest {

    /**
     * CLOB (java.sql.Types 2005) must resolve to a name - the ALTER TABLE path maps existing column
     * JDBC type codes back through getDatabaseTypeName, and a CLOB column (a {@code text} field)
     * previously threw "Type [2005] not supported".
     */
    @Test
    public void clobIsSupported() {
        assertTrue(DataTypeUtils.isDatabaseTypeSupported(Types.CLOB));
        assertEquals(DataType.CLOB.toString(), DataTypeUtils.getDatabaseTypeName(Types.CLOB));
    }

    /**
     * NCLOB (java.sql.Types 2011) was the same omission as CLOB.
     */
    @Test
    public void nclobIsSupported() {
        assertTrue(DataTypeUtils.isDatabaseTypeSupported(Types.NCLOB));
        assertEquals(DataType.NCLOB.toString(), DataTypeUtils.getDatabaseTypeName(Types.NCLOB));
    }

    /**
     * BLOB stays supported (guards against regressing the sibling large-object mapping).
     */
    @Test
    public void blobIsSupported() {
        assertTrue(DataTypeUtils.isDatabaseTypeSupported(Types.BLOB));
        assertEquals(DataType.BLOB.toString(), DataTypeUtils.getDatabaseTypeName(Types.BLOB));
    }
}
