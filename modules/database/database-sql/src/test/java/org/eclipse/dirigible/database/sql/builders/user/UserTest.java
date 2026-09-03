/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.user;

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
        String sql = SqlFactory.getDefault()
                               .create()
                               .user("MY_USER", "s3cret")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE USER \"MY_USER\" PASSWORD 's3cret'", sql);
    }

    /**
     * Alter user.
     */
    @Test
    public void alterUser() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .user("MY_USER", "rotated")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER USER \"MY_USER\" PASSWORD 'rotated'", sql);
    }

    /**
     * Drop user.
     */
    @Test
    public void dropUser() {
        String sql = SqlFactory.getDefault()
                               .drop()
                               .user("MY_USER")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP USER \"MY_USER\"", sql);
    }

    /**
     * A user name is quoted exactly once, whether or not the caller quoted it.
     */
    @Test
    public void createUserAlreadyQuoted() {
        String sql = SqlFactory.getDefault()
                               .create()
                               .user("\"MY_USER\"", "s3cret")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE USER \"MY_USER\" PASSWORD 's3cret'", sql);
    }

    /**
     * An escape symbol inside a user name is doubled rather than ending the identifier.
     */
    @Test
    public void createUserWithEscapeSymbolInName() {
        String sql = SqlFactory.getDefault()
                               .create()
                               .user("ev\"il", "s3cret")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE USER \"ev\"\"il\" PASSWORD 's3cret'", sql);
    }

    /**
     * A quote inside a password is doubled rather than ending the literal.
     */
    @Test
    public void createUserWithQuoteInPassword() {
        String sql = SqlFactory.getDefault()
                               .create()
                               .user("MY_USER", "pa'ss")
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE USER \"MY_USER\" PASSWORD 'pa''ss'", sql);
    }

    /**
     * The same protection on the alter path, which carries a password too.
     */
    @Test
    public void alterUserWithQuoteInPassword() {
        String sql = SqlFactory.getDefault()
                               .alter()
                               .user("MY_USER", "pa'ss")
                               .build();

        assertNotNull(sql);
        assertEquals("ALTER USER \"MY_USER\" PASSWORD 'pa''ss'", sql);
    }

    /**
     * And on the drop path, which carries only an identifier.
     */
    @Test
    public void dropUserWithEscapeSymbolInName() {
        String sql = SqlFactory.getDefault()
                               .drop()
                               .user("ev\"il")
                               .build();

        assertNotNull(sql);
        assertEquals("DROP USER \"ev\"\"il\"", sql);
    }

}
