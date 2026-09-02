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
 * HANA has no INFORMATION_SCHEMA - the table alter path's unique-constraint reconciliation (#7019)
 * reads SYS.CONSTRAINTS instead, keeping the primary key out of the picture.
 */
public class UniqueConstraintsCatalogTest {

    @Test
    public void readsUniqueKeysFromSysConstraintsWithoutThePrimaryKey() {
        assertEquals(
                "SELECT CONSTRAINT_NAME, COLUMN_NAME FROM SYS.CONSTRAINTS WHERE TABLE_NAME = ? AND SCHEMA_NAME = ?"
                        + " AND IS_UNIQUE_KEY = 'TRUE' AND IS_PRIMARY_KEY = 'FALSE' ORDER BY CONSTRAINT_NAME, POSITION",
                HanaSqlDialect.uniqueConstraintsQuery(true));
    }

    @Test
    public void fallsBackToTheCurrentSchemaWhenTheDriverReportsNone() {
        assertEquals(
                "SELECT CONSTRAINT_NAME, COLUMN_NAME FROM SYS.CONSTRAINTS WHERE TABLE_NAME = ? AND SCHEMA_NAME = CURRENT_SCHEMA"
                        + " AND IS_UNIQUE_KEY = 'TRUE' AND IS_PRIMARY_KEY = 'FALSE' ORDER BY CONSTRAINT_NAME, POSITION",
                HanaSqlDialect.uniqueConstraintsQuery(false));
    }
}
