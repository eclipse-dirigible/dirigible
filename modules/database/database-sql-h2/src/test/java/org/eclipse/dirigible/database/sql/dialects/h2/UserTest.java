/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.h2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class UserTest.
 */
public class UserTest {

    /**
     * Creates the user.
     */
    @Test
    public void createUser() {
        String sql = SqlFactory.getNative(new H2SqlDialect())
                               .create()
                               .user("MY_USER", "s3cret")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE USER \"MY_USER\" PASSWORD 's3cret'", sql);
    }

    /**
     * H2 spells the assignment out, where PostgreSQL takes the bare PASSWORD keyword.
     */
    @Test
    public void alterUser() {
        String sql = SqlFactory.getNative(new H2SqlDialect())
                               .alter()
                               .user("MY_USER", "rotated")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER USER \"MY_USER\" SET PASSWORD 'rotated'", sql);
    }

    /**
     * Drop user.
     */
    @Test
    public void dropUser() {
        String sql = SqlFactory.getNative(new H2SqlDialect())
                               .drop()
                               .user("MY_USER")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP USER \"MY_USER\"", sql);
    }

}
