/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The column rename of the table alter path (#7635) on HANA.
 */
public class ColumnChangeStatementTest {

    @Test
    public void renameColumnNamesTheColumnThroughItsTable() {
        assertEquals("RENAME COLUMN \"T_INVOICE\".\"INVOICE_DATE\" TO \"ISSUE_DATE\"",
                new HanaSqlDialect().renameColumn("T_INVOICE", "INVOICE_DATE", "ISSUE_DATE"));
    }
}
