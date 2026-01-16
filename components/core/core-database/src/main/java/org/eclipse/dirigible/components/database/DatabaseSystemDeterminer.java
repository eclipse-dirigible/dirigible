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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DatabaseSystemDeterminer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSystemDeterminer.class);

    private static final Map<DatabaseSystem, String> DB_URL_PREFIXES = Map.of(//
            DatabaseSystem.H2, "jdbc:h2", //
            DatabaseSystem.POSTGRESQL, "jdbc:postgresql", //
            DatabaseSystem.HANA, "jdbc:sap", //
            DatabaseSystem.SNOWFLAKE, "jdbc:snowflake", //
            DatabaseSystem.MARIADB, "jdbc:mariadb", //
            DatabaseSystem.MYSQL, "jdbc:mysql", //
            DatabaseSystem.MONGODB, "jdbc:mongodb", //
            DatabaseSystem.DERBY, "jdbc:derby", //
            DatabaseSystem.MSSQL, "jdbc:sqlserver"//
    );

    private static final Map<DatabaseSystem, List<String>> DB_DRIVERS = Map.of(//
            DatabaseSystem.H2, List.of("org.h2.Driver"), //
            DatabaseSystem.POSTGRESQL, List.of("org.postgresql.Driver"), //
            DatabaseSystem.HANA, List.of("com.sap.db.jdbc.Driver"), //
            DatabaseSystem.SNOWFLAKE, List.of("net.snowflake.client.jdbc.SnowflakeDriver"), //
            DatabaseSystem.MARIADB, List.of("org.mariadb.jdbc.Driver"), //
            DatabaseSystem.MYSQL, List.of("com.mysql.cj.jdbc.Driver"), //
            DatabaseSystem.MONGODB, List.of("com.mongodb.jdbc.MongoDriver"), //
            DatabaseSystem.MSSQL, List.of("com.microsoft.sqlserver.jdbc.SQLServerDriver"), //
            DatabaseSystem.DERBY, List.of("org.apache.derby.jdbc.ClientDriver", "org.apache.derby.jdbc.EmbeddedDriver")//
    );

    public static DatabaseSystem determine(javax.sql.DataSource dataSource) throws SQLException {
        if (dataSource instanceof DirigibleDataSource ddc) {
            return ddc.getDatabaseSystem();
        }

        try (Connection connection = dataSource.getConnection()) {
            return determine(connection);
        }
    }

    public static DatabaseSystem determine(Connection connection) throws SQLException {
        if (connection instanceof DirigibleConnection dc) {
            return dc.getDatabaseSystem();
        }

        DatabaseMetaData metaData = connection.getMetaData();
        String jdbcUrl = metaData.getURL();
        String driverClass = metaData.getDriverName();

        return determine(jdbcUrl, driverClass);
    }

    public static DatabaseSystem determine(String jdbcUrl, String driverClass) {
        DatabaseSystem databaseSystem = determineSystemByJdbcUrl(jdbcUrl)//
                                                                         .orElseGet(() -> determineSystemByDriverClass(driverClass));
        if (databaseSystem.isUnknown()) {
            LOGGER.warn("JDBC url [{}] and driver [{}] are determined as [{}]. Most probably something is misconfigured", jdbcUrl,
                    driverClass, databaseSystem);

        }
        LOGGER.debug("JDBC url [{}] and driver [{}] are determined as [{}]", jdbcUrl, driverClass, databaseSystem);
        return databaseSystem;
    }

    private static Optional<DatabaseSystem> determineSystemByJdbcUrl(String jdbcUrl) {
        return DB_URL_PREFIXES.entrySet()
                              .stream()
                              .filter(entry -> isJdbcUrlStartWithString(jdbcUrl, entry.getValue()))
                              .findFirst()
                              .map(Map.Entry::getKey);
    }

    private static boolean isJdbcUrlStartWithString(String jdbcUrl, String prefix) {
        if (StringUtils.isBlank(jdbcUrl)) {
            LOGGER.warn("Received blank JDBC URL [{}]", jdbcUrl, jdbcUrl);
            return false;
        }
        return jdbcUrl.trim()
                      .toLowerCase()
                      .startsWith(prefix);
    }

    private static DatabaseSystem determineSystemByDriverClass(String driverClass) {
        return DB_DRIVERS.entrySet()
                         .stream()
                         .filter(entry -> usedOneOfDrivers(driverClass, entry.getValue()))
                         .findFirst()
                         .map(Map.Entry::getKey)
                         .orElse(DatabaseSystem.UNKNOWN);
    }

    private static boolean usedOneOfDrivers(String driverClass, List<String> drivers) {
        String trimmedDriverClassName = safelyTrim(driverClass);
        return drivers.stream()
                      .anyMatch(driver -> Objects.equals(trimmedDriverClassName, safelyTrim(driver)));
    }

    private static String safelyTrim(String string) {
        return null != string ? string.trim() : null;
    }

}
