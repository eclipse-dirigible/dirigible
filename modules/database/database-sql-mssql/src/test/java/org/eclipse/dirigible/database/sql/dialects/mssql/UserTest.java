/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class UserTest.
 */
public class UserTest {

    /**
     * A user is a server login plus a database user mapped to it.
     */
    @Test
    public void createUser() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .create()
                               .user("MY_USER", "s3cret")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE LOGIN \"MY_USER\" WITH PASSWORD ='s3cret'; CREATE USER \"MY_USER\" FOR LOGIN \"MY_USER\"", sql);
    }

    /**
     * The password belongs to the login, so rotating it is ALTER LOGIN and not ALTER USER.
     */
    @Test
    public void alterUser() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .alter()
                               .user("MY_USER", "rotated")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER LOGIN \"MY_USER\" WITH PASSWORD ='rotated'", sql);
    }

    /**
     * Both objects go, otherwise the login outlives the user and blocks the next create.
     */
    @Test
    public void dropUser() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .drop()
                               .user("MY_USER")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP USER \"MY_USER\"; DROP LOGIN \"MY_USER\"", sql);
    }

    /**
     * The escaping applies on this dialect too.
     */
    @Test
    public void createUserWithQuoteInPassword() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .create()
                               .user("MY_USER", "pa'ss")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE LOGIN \"MY_USER\" WITH PASSWORD ='pa''ss'; CREATE USER \"MY_USER\" FOR LOGIN \"MY_USER\"", sql);
    }

}
