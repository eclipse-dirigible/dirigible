/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.sql.SQLException;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The driver delegate follows the database the job store runs on (#7049) - the standard delegate
 * fails every trigger read on PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class QuartzDriverDelegateResolverTest {

    @Mock
    private DirigibleDataSource dataSource;

    @AfterEach
    void clearOverride() {
        Configuration.remove(QuartzDriverDelegateResolver.DELEGATE_OVERRIDE_KEY);
    }

    @Test
    void testPostgreSqlGetsItsOwnDelegate() throws SQLException {
        when(dataSource.getDatabaseSystem()).thenReturn(DatabaseSystem.POSTGRESQL);

        assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate", QuartzDriverDelegateResolver.resolve(dataSource));
    }

    @Test
    void testMssqlGetsItsOwnDelegate() throws SQLException {
        when(dataSource.getDatabaseSystem()).thenReturn(DatabaseSystem.MSSQL);

        assertEquals("org.quartz.impl.jdbcjobstore.MSSQLDelegate", QuartzDriverDelegateResolver.resolve(dataSource));
    }

    @Test
    void testH2KeepsTheStandardDelegate() throws SQLException {
        when(dataSource.getDatabaseSystem()).thenReturn(DatabaseSystem.H2);

        assertEquals(QuartzDriverDelegateResolver.STANDARD_DELEGATE, QuartzDriverDelegateResolver.resolve(dataSource));
    }

    @Test
    void testUnmappedDatabaseKeepsTheStandardDelegate() {
        assertEquals(QuartzDriverDelegateResolver.STANDARD_DELEGATE, QuartzDriverDelegateResolver.delegateFor(DatabaseSystem.MARIADB));
        assertEquals(QuartzDriverDelegateResolver.STANDARD_DELEGATE, QuartzDriverDelegateResolver.delegateFor(DatabaseSystem.UNKNOWN));
    }

    @Test
    void testConfiguredDelegateOverridesTheDerivedOne() {
        Configuration.set(QuartzDriverDelegateResolver.DELEGATE_OVERRIDE_KEY, "org.quartz.impl.jdbcjobstore.HSQLDBDelegate");

        assertEquals("org.quartz.impl.jdbcjobstore.HSQLDBDelegate", QuartzDriverDelegateResolver.resolve(dataSource));
    }

    @Test
    void testBlankConfiguredDelegateIsIgnored() throws SQLException {
        Configuration.set(QuartzDriverDelegateResolver.DELEGATE_OVERRIDE_KEY, "  ");
        when(dataSource.getDatabaseSystem()).thenReturn(DatabaseSystem.POSTGRESQL);

        assertEquals("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate", QuartzDriverDelegateResolver.resolve(dataSource));
    }
}
