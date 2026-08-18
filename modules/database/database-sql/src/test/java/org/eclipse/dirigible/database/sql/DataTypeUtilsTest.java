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
import static org.junit.Assert.assertFalse;
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

    /**
     * TIMESTAMP WITH TIME ZONE (java.sql.Types 2014) is what H2 reports back for a column created from
     * a {@code java.time.Instant} mapping. The ALTER TABLE path resolves every existing column's JDBC
     * type code through getDatabaseTypeName, so the missing entry failed the whole schema artefact with
     * "Type [2014] not supported".
     */
    @Test
    public void timestampWithTimeZoneIsSupported() {
        assertTrue(DataTypeUtils.isDatabaseTypeSupported(Types.TIMESTAMP_WITH_TIMEZONE));
        assertEquals(DataType.TIMESTAMP.toString(), DataTypeUtils.getDatabaseTypeName(Types.TIMESTAMP_WITH_TIMEZONE));
    }

    /**
     * TIME WITH TIME ZONE (java.sql.Types 2013) is the sibling omission of TIMESTAMP WITH TIME ZONE.
     */
    @Test
    public void timeWithTimeZoneIsSupported() {
        assertTrue(DataTypeUtils.isDatabaseTypeSupported(Types.TIME_WITH_TIMEZONE));
        assertEquals(DataType.TIME.toString(), DataTypeUtils.getDatabaseTypeName(Types.TIME_WITH_TIMEZONE));
    }

    /**
     * A with-time-zone column is still a temporal one, so it must unify with the plain TIMESTAMP a
     * table definition declares.
     */
    @Test
    public void timestampWithTimeZoneUnifiesWithTimestamp() {
        assertEquals(DataTypeUtils.getUnifiedDatabaseType(DataType.TIMESTAMP.toString()),
                DataTypeUtils.getUnifiedDatabaseType(DataTypeUtils.getDatabaseTypeName(Types.TIMESTAMP_WITH_TIMEZONE)));
    }

    /**
     * TIMESTAMPTZ is what PostgreSQL's JDBC driver reports as a column's TYPE_NAME for a
     * {@code timestamptz} column (e.g. a {@code java.time.Instant} field). CSVIM's CsvProcessor
     * resolves every column of the target table through this string-keyed lookup - independently of
     * whether the CSV touches that column's value - so a missing entry here failed the import of any
     * table with such a column with "Type [TIMESTAMPTZ] not supported", regardless of the actual data.
     */
    @Test
    public void timestampTzStringResolvesToTimestamp() {
        assertEquals(Types.TIMESTAMP, (int) DataTypeUtils.getSqlTypeByDataType("TIMESTAMPTZ"));
        assertEquals(Types.TIMESTAMP, (int) DataTypeUtils.getSqlTypeByDataType("TIMESTAMP WITH TIME ZONE"));
    }

    /**
     * PostgreSQL JDBC metadata reports a boolean column as java.sql.Types.BIT, so BIT (and the BOOL
     * alias) must unify with the BOOLEAN a table definition declares - otherwise every alter over a
     * table with a boolean column fails as an incompatible change.
     */
    @Test
    public void bitUnifiesWithBoolean() {
        assertEquals("BOOLEAN", DataTypeUtils.getUnifiedDatabaseType("BIT"));
        assertEquals("BOOLEAN", DataTypeUtils.getUnifiedDatabaseType("BOOL"));
        assertEquals("BOOLEAN", DataTypeUtils.getUnifiedDatabaseType(DataTypeUtils.getDatabaseTypeName(Types.BIT)));
    }

    /**
     * DECIMAL is an alias of NUMERIC in PostgreSQL (and interchangeable per the SQL standard), so a
     * DECIMAL model column reported back by the metadata as NUMERIC is the same column, not an
     * incompatible change.
     */
    @Test
    public void numericUnifiesWithDecimal() {
        assertEquals(DataTypeUtils.getUnifiedDatabaseType("DECIMAL"), DataTypeUtils.getUnifiedDatabaseType("NUMERIC"));
        assertEquals(DataTypeUtils.getUnifiedDatabaseType("DECIMAL"),
                DataTypeUtils.getUnifiedDatabaseType(DataTypeUtils.getDatabaseTypeName(Types.NUMERIC)));
    }

    /**
     * The character types are one family, aliases included - a CLOB column reported back as VARCHAR (or
     * CHARACTER VARYING) holds the same kind of value.
     */
    @Test
    public void characterTypesAreOneFamily() {
        assertTrue(DataTypeUtils.isCharacterType("VARCHAR"));
        assertTrue(DataTypeUtils.isCharacterType("CHARACTER VARYING"));
        assertTrue(DataTypeUtils.isCharacterType("NVARCHAR"));
        assertTrue(DataTypeUtils.isCharacterType("CHAR"));
        assertTrue(DataTypeUtils.isCharacterType("TEXT"));
        assertTrue(DataTypeUtils.isCharacterType("CLOB"));
        assertTrue(DataTypeUtils.isCharacterType("CHARACTER LARGE OBJECT"));
        assertTrue(DataTypeUtils.isCharacterType("NCLOB"));
    }

    /**
     * Everything else is not - the family check must not swallow a real type change.
     */
    @Test
    public void nonCharacterTypesAreNotInTheFamily() {
        assertFalse(DataTypeUtils.isCharacterType("INTEGER"));
        assertFalse(DataTypeUtils.isCharacterType("DECIMAL"));
        assertFalse(DataTypeUtils.isCharacterType("TIMESTAMP"));
        assertFalse(DataTypeUtils.isCharacterType("BLOB"));
        assertFalse(DataTypeUtils.isCharacterType("BOOLEAN"));
    }
}
