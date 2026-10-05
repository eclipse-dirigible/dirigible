/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * The column statements of the table alter path (#7635) on MySQL.
 */
public class ColumnChangeStatementTest {

    private final MySQLSqlDialect dialect = new MySQLSqlDialect();

    @Test
    public void renameColumnQuotesWithBackticks() {
        assertEquals("ALTER TABLE `T_INVOICE` RENAME COLUMN `INVOICE_DATE` TO `ISSUE_DATE`",
                dialect.renameColumn("T_INVOICE", "INVOICE_DATE", "ISSUE_DATE"));
    }

    @Test
    public void dropNotNullNeedsTheWholeColumnType() {
        assertNull(dialect.dropNotNull("T_INVOICE", "NOTE"));
    }
}
