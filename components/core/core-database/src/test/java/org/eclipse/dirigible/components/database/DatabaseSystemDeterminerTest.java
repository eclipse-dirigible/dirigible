/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DatabaseSystemDeterminerTest {

    @InjectMocks
    private DatabaseSystemDeterminer databaseSystemDeterminer;

    @Test
    void testDetermineMSSQL_withUrl() {
        testDetermine_withUrl("jdbc:sqlserver://localhost:1433;databaseName=master", DatabaseSystem.MSSQL);
    }

    private void testDetermine_withUrl(String jdbcUrl, DatabaseSystem expectedType) {
        DatabaseSystem result = DatabaseSystemDeterminer.determine(jdbcUrl, null);

        assertEquals(expectedType, result);
    }

    @Test
    void testDetermineMSSQL_withDriverUrl() {
        testDetermine_withDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver", DatabaseSystem.MSSQL);
    }

    private void testDetermine_withDriver(String driverClass, DatabaseSystem expectedType) {
        DatabaseSystem result = DatabaseSystemDeterminer.determine(null, driverClass);

        assertEquals(expectedType, result);
    }

    @Test
    void testDetermineDerby_withUrl() {
        testDetermine_withUrl("jdbc:derby://localhost:1527/databaseName;create=true", DatabaseSystem.DERBY);
    }

    @Test
    void testDetermineDerby_withDriverUrl() {
        testDetermine_withDriver("org.apache.derby.jdbc.EmbeddedDriver", DatabaseSystem.DERBY);
    }

    @Test
    void testDetermineDerby_withDriverUrl2() {
        testDetermine_withDriver("org.apache.derby.jdbc.ClientDriver", DatabaseSystem.DERBY);
    }

    @Test
    void testDetermineH2_withUrl() {
        testDetermine_withUrl("jdbc:h2:mem:test", DatabaseSystem.H2);
    }

    @Test
    void testDetermineH2_withDriverUrl() {
        testDetermine_withDriver("org.h2.Driver", DatabaseSystem.H2);
    }

    @Test
    void testDeterminePostgreSQL_withUrl() {
        testDetermine_withUrl("jdbc:postgresql://localhost:5432/testdb", DatabaseSystem.POSTGRESQL);
    }

    @Test
    void testDeterminePostgreSQL_withDriver() {
        testDetermine_withDriver("org.postgresql.Driver", DatabaseSystem.POSTGRESQL);
    }

    @Test
    void testDetermineHANA_withUrl() {
        testDetermine_withUrl("jdbc:sap://hana-db:30015", DatabaseSystem.HANA);
    }

    @Test
    void testDetermineHANA_withDriver() {
        testDetermine_withDriver("com.sap.db.jdbc.Driver", DatabaseSystem.HANA);
    }

    @Test
    void testDetermineSnowflake_withUrl() {
        testDetermine_withUrl("jdbc:snowflake://account.snowflakecomputing.com", DatabaseSystem.SNOWFLAKE);
    }

    @Test
    void testDetermineSnowflake_withDriver() {
        testDetermine_withDriver("net.snowflake.client.jdbc.SnowflakeDriver", DatabaseSystem.SNOWFLAKE);
    }

    @Test
    void testDetermineMariaDB_withUrl() {
        testDetermine_withUrl("jdbc:mariadb://localhost:3306/testdb", DatabaseSystem.MARIADB);
    }

    @Test
    void testDetermineMariaDB_withDriver() {
        testDetermine_withDriver("org.mariadb.jdbc.Driver", DatabaseSystem.MARIADB);
    }

    @Test
    void testDetermineMySQL_withUrl() {
        testDetermine_withUrl("jdbc:mysql://localhost:3306/testdb", DatabaseSystem.MYSQL);
    }

    @Test
    void testDetermineMySQL_withDriver() {
        testDetermine_withDriver("com.mysql.cj.jdbc.Driver", DatabaseSystem.MYSQL);
    }

    @Test
    void testDetermineMongoDB_withUrl() {
        testDetermine_withUrl("jdbc:mongodb://localhost:27017/mydatabase", DatabaseSystem.MONGODB);
    }

    @Test
    void testDetermineMongoDB_withDriver() {
        testDetermine_withDriver("com.mongodb.jdbc.MongoDriver", DatabaseSystem.MONGODB);
    }

    @Test
    void testDetermineUnknown_withUrl() {
        testDetermine_withUrl("jdbc:unknown://localhost:1234/testdb", DatabaseSystem.UNKNOWN);
    }

    @Test
    void testDetermineUnknown_withDriver() {
        testDetermine_withDriver("com.unknown.Driver", DatabaseSystem.UNKNOWN);
    }

}
