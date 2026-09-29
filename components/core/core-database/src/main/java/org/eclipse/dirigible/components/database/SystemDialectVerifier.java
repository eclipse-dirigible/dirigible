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

import java.util.Map;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refuses a configured SystemDB dialect that contradicts the SystemDB driver.
 *
 * <p>
 * Hibernate applies a configured dialect verbatim. One of another database makes the schema update
 * pass miss the existing tables (identifier case differs) and try to create them again, so the boot
 * fails at a distance with {@code relation ... already exists} instead of naming the
 * misconfiguration. The database is determined from the configured JDBC URL and driver, the same
 * way {@link DatabaseSystemDeterminer} does for every data source, and the configured dialect must
 * be the dialect of that database or a subclass of it. A database without a Hibernate core dialect
 * (Derby, Snowflake, MongoDB, an unknown one) and a dialect class that cannot be loaded are left to
 * Hibernate.
 */
final class SystemDialectVerifier {

    /** The configuration key naming the SystemDB dialect. */
    static final String DIALECT_KEY = "DIRIGIBLE_DATABASE_SYSTEM_DIALECT";

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDialectVerifier.class);

    /** MariaDB is accepted with the MySQL dialect, which {@code MariaDBDialect} extends. */
    private static final Map<DatabaseSystem, Class<? extends Dialect>> DIALECTS_BY_DATABASE = Map.of(//
            DatabaseSystem.H2, H2Dialect.class, //
            DatabaseSystem.POSTGRESQL, PostgreSQLDialect.class, //
            DatabaseSystem.HANA, HANADialect.class, //
            DatabaseSystem.MARIADB, MySQLDialect.class, //
            DatabaseSystem.MYSQL, MySQLDialect.class, //
            DatabaseSystem.MSSQL, SQLServerDialect.class//
    );

    private SystemDialectVerifier() {}

    /**
     * Verifies that the configured dialect belongs to the database the SystemDB driver connects to.
     *
     * @param configuredDialect the fully qualified dialect class name, as configured
     * @param driverClass the SystemDB driver class name
     * @param jdbcUrl the SystemDB JDBC URL
     * @throws IllegalStateException when the dialect is of another database than the driver
     */
    static void verify(String configuredDialect, String driverClass, String jdbcUrl) {
        DatabaseSystem databaseSystem = DatabaseSystemDeterminer.determine(jdbcUrl, driverClass);
        Class<? extends Dialect> expectedDialect = DIALECTS_BY_DATABASE.get(databaseSystem);
        if (expectedDialect == null) {
            return;
        }
        Class<?> dialect;
        try {
            dialect = Class.forName(configuredDialect.trim(), false, SystemDialectVerifier.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            LOGGER.debug("Cannot load the configured dialect [{}] - leaving its resolution to Hibernate", configuredDialect, ex);
            return;
        }
        if (!expectedDialect.isAssignableFrom(dialect)) {
            throw new IllegalStateException(DIALECT_KEY + "=" + configuredDialect + " does not match the SystemDB driver " + driverClass
                    + " (" + withoutParameters(jdbcUrl) + ") - remove the variable (the dialect is detected) or set the " + databaseSystem
                    + " dialect " + expectedDialect.getName());
        }
    }

    /** Parameters can carry credentials (e.g. SQL Server's {@code ;password=}), so they are cut off. */
    private static String withoutParameters(String jdbcUrl) {
        int end = jdbcUrl.length();
        for (char separator : new char[] {'?', ';'}) {
            int index = jdbcUrl.indexOf(separator);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return jdbcUrl.substring(0, end);
    }
}
