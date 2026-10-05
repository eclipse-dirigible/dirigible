/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The column statements of the table alter path (#7635) in their SQL standard form.
 */
public class ColumnChangeStatementTest {

    private final DefaultSqlDialect dialect = new DefaultSqlDialect();

    @Test
    public void renameColumn() {
        assertEquals("ALTER TABLE \"T_INVOICE\" RENAME COLUMN \"INVOICE_DATE\" TO \"ISSUE_DATE\"",
                dialect.renameColumn("T_INVOICE", "INVOICE_DATE", "ISSUE_DATE"));
    }

    @Test
    public void dropNotNull() {
        assertEquals("ALTER TABLE \"T_INVOICE\" ALTER COLUMN \"NOTE\" DROP NOT NULL", dialect.dropNotNull("T_INVOICE", "NOTE"));
    }

    @Test
    public void anEmbeddedEscapeSymbolIsDoubled() {
        assertEquals("ALTER TABLE \"T\" RENAME COLUMN \"A\"\"B\" TO \"C\"", dialect.renameColumn("T", "A\"B", "C"));
    }
}
