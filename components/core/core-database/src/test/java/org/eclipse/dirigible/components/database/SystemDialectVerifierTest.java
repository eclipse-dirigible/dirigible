/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SystemDialectVerifierTest {

    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

    private static final String POSTGRESQL_URL = "jdbc:postgresql://db:5432/system";

    @Test
    void refusesADialectOfAnotherDatabaseNamingBoth() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SystemDialectVerifier.verify("org.hibernate.dialect.H2Dialect", POSTGRESQL_DRIVER, POSTGRESQL_URL));

        assertEquals("DIRIGIBLE_DATABASE_SYSTEM_DIALECT=org.hibernate.dialect.H2Dialect does not match the SystemDB driver"
                + " org.postgresql.Driver (jdbc:postgresql://db:5432/system) - remove the variable (the dialect is detected)"
                + " or set the POSTGRESQL dialect org.hibernate.dialect.PostgreSQLDialect", ex.getMessage());
    }

    @Test
    void refusesAPostgreSQLDialectOnH2() {
        assertThrows(IllegalStateException.class,
                () -> SystemDialectVerifier.verify("org.hibernate.dialect.PostgreSQLDialect", "org.h2.Driver", "jdbc:h2:mem:system"));
    }

    @Test
    void acceptsTheDialectOfTheDriversDatabase() {
        assertDoesNotThrow(
                () -> SystemDialectVerifier.verify("org.hibernate.dialect.PostgreSQLDialect", POSTGRESQL_DRIVER, POSTGRESQL_URL));
        assertDoesNotThrow(() -> SystemDialectVerifier.verify(" org.hibernate.dialect.H2Dialect ", "org.h2.Driver",
                "jdbc:h2:file:./target/dirigible/h2/SystemDB;LOCK_TIMEOUT=10000"));
    }

    @Test
    void acceptsASubclassOfTheDatabasesDialect() {
        assertDoesNotThrow(
                () -> SystemDialectVerifier.verify("org.hibernate.dialect.PostgresPlusDialect", POSTGRESQL_DRIVER, POSTGRESQL_URL));
        assertDoesNotThrow(() -> SystemDialectVerifier.verify("org.hibernate.dialect.MariaDBDialect", "com.mysql.cj.jdbc.Driver",
                "jdbc:mysql://db/system"));
    }

    @Test
    void acceptsTheMySQLDialectOnMariaDB() {
        assertDoesNotThrow(() -> SystemDialectVerifier.verify("org.hibernate.dialect.MySQLDialect", "org.mariadb.jdbc.Driver",
                "jdbc:mariadb://db/system"));
    }

    @Test
    void leavesADatabaseWithoutACoreDialectToHibernate() {
        assertDoesNotThrow(() -> SystemDialectVerifier.verify("org.hibernate.dialect.H2Dialect", "org.apache.derby.jdbc.EmbeddedDriver",
                "jdbc:derby:memory:system"));
    }

    @Test
    void leavesAnUnloadableDialectToHibernate() {
        assertDoesNotThrow(() -> SystemDialectVerifier.verify("com.example.MissingDialect", POSTGRESQL_DRIVER, POSTGRESQL_URL));
    }

    @Test
    void cutsParametersThatCanCarryCredentialsOffTheUrl() {
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> SystemDialectVerifier.verify("org.hibernate.dialect.H2Dialect",
                        "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://db:1433;databaseName=system;password=secret"));

        assertFalse(ex.getMessage()
                      .contains("secret"));
    }
}
