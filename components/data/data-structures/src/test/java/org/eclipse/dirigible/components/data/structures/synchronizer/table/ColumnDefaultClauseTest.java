/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.dirigible.database.sql.DataType;
import org.junit.jupiter.api.Test;

/**
 * The DEFAULT clause a column definition gets from the default its model carries (dirigible #7765).
 */
class ColumnDefaultClauseTest {

    @Test
    void aBareTextOnACharacterColumnIsAStringLiteral() {
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.VARCHAR, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.CHAR, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.NVARCHAR, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.CHARACTER_VARYING, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.CHARACTER, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.TEXT, "web"));
        assertEquals(" DEFAULT 'web' ", ColumnDefaultClause.of(DataType.CLOB, "web"));
    }

    @Test
    void anApostropheInsideTheTextIsEscaped() {
        assertEquals(" DEFAULT 'Owner''s copy' ", ColumnDefaultClause.of(DataType.VARCHAR, "Owner's copy"));
    }

    @Test
    void aTextThatIsAlreadyALiteralIsKeptAsWritten() {
        assertEquals(" DEFAULT 'draft' ", ColumnDefaultClause.of(DataType.VARCHAR, "'draft'"));
        assertEquals(" DEFAULT '' ", ColumnDefaultClause.of(DataType.VARCHAR, "''"));
    }

    @Test
    void aSqlExpressionOnACharacterColumnIsKeptAsWritten() {
        assertEquals(" DEFAULT NULL ", ColumnDefaultClause.of(DataType.VARCHAR, "NULL"));
        assertEquals(" DEFAULT null ", ColumnDefaultClause.of(DataType.VARCHAR, "null"));
        assertEquals(" DEFAULT CURRENT_USER ", ColumnDefaultClause.of(DataType.VARCHAR, "CURRENT_USER"));
        assertEquals(" DEFAULT gen_random_uuid() ", ColumnDefaultClause.of(DataType.VARCHAR, "gen_random_uuid()"));
        assertEquals(" DEFAULT RANDOM_UUID() ", ColumnDefaultClause.of(DataType.VARCHAR, "RANDOM_UUID()"));
        assertEquals(" DEFAULT UPPER('x') ", ColumnDefaultClause.of(DataType.VARCHAR, "UPPER('x')"));
    }

    @Test
    void aWordThatOnlyLooksLikeAFunctionNameIsStillText() {
        // a user named "user()" is a call, the word "user" alone is a session function - but "users" is
        // text
        assertEquals(" DEFAULT 'users' ", ColumnDefaultClause.of(DataType.VARCHAR, "users"));
        assertEquals(" DEFAULT 'now' ", ColumnDefaultClause.of(DataType.VARCHAR, "now"));
        assertEquals(" DEFAULT 'DRAFT' ", ColumnDefaultClause.of(DataType.VARCHAR, "DRAFT"));
        assertEquals(" DEFAULT 'a (b)' ", ColumnDefaultClause.of(DataType.VARCHAR, "a (b)"));
    }

    @Test
    void anEmptyDefaultIsTheEmptyStringOnACharacterColumnAndNothingElsewhere() {
        assertEquals(" DEFAULT '' ", ColumnDefaultClause.of(DataType.VARCHAR, ""));
        assertEquals("", ColumnDefaultClause.of(DataType.INTEGER, ""));
        assertEquals("", ColumnDefaultClause.of(DataType.TIMESTAMP, ""));
    }

    @Test
    void aNonCharacterColumnKeepsItsExpressionVerbatim() {
        assertEquals(" DEFAULT 0 ", ColumnDefaultClause.of(DataType.INTEGER, "0"));
        assertEquals(" DEFAULT 2 ", ColumnDefaultClause.of(DataType.INTEGER, "2"));
        assertEquals(" DEFAULT true ", ColumnDefaultClause.of(DataType.BOOLEAN, "true"));
        assertEquals(" DEFAULT CURRENT_DATE ", ColumnDefaultClause.of(DataType.DATE, "CURRENT_DATE"));
        assertEquals(" DEFAULT now() ", ColumnDefaultClause.of(DataType.TIMESTAMP, "now()"));
        assertEquals(" DEFAULT 1.5 ", ColumnDefaultClause.of(DataType.DECIMAL, "1.5"));
    }

    @Test
    void noDefaultIsNoClause() {
        assertEquals("", ColumnDefaultClause.of(DataType.VARCHAR, null));
        assertEquals("", ColumnDefaultClause.of(DataType.INTEGER, null));
    }
}
