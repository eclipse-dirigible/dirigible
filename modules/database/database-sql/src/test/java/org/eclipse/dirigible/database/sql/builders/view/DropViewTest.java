/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class DropViewTest.
 */
public class DropViewTest {

    /**
     * Drop table case sensitive.
     */
    @Test
    public void dropTableCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .drop()
                               .view("CUSTOMERS_VIEW")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP VIEW \"CUSTOMERS_VIEW\"", sql);
    }
}
