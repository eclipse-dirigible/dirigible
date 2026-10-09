/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import java.util.Map;

/**
 * Checks that a data source's JDBC driver class can be loaded before HikariCP is asked to, so a
 * driver that is not bundled fails with a message naming the driver and how to add it instead of a
 * bare {@code ClassNotFoundException}.
 *
 * <p>
 * The default executable bundles only the H2 and PostgreSQL drivers (#7804). The dialects of the
 * other databases stay in the platform, their drivers do not.
 */
final class JdbcDriverAvailability {

    /** Driver class name to the database name and the Maven coordinate that provides it. */
    private static final Map<String, String[]> KNOWN_DRIVERS = Map.of(//
            "com.mysql.cj.jdbc.Driver", new String[] {"MySQL", "com.mysql:mysql-connector-j"}, //
            "org.mariadb.jdbc.Driver", new String[] {"MariaDB", "org.mariadb.jdbc:mariadb-java-client"}, //
            "com.microsoft.sqlserver.jdbc.SQLServerDriver", new String[] {"Microsoft SQL Server", "com.microsoft.sqlserver:mssql-jdbc"}, //
            "com.sap.db.jdbc.Driver", new String[] {"SAP HANA", "com.sap.cloud.db.jdbc:ngdbc"}, //
            "net.snowflake.client.jdbc.SnowflakeDriver", new String[] {"Snowflake", "net.snowflake:snowflake-jdbc"});

    private JdbcDriverAvailability() {}

    /**
     * Fails when the driver class cannot be loaded from the class loaders HikariCP uses: the thread
     * context class loader, then the platform's own. A blank driver is left to HikariCP, which resolves
     * the driver from the URL.
     *
     * @param dataSourceName the data source name
     * @param driverClassName the configured driver class name
     * @throws IllegalStateException if the driver class is not on the classpath
     */
    static void requireDriver(String dataSourceName, String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return;
        }
        String driver = driverClassName.trim();
        if (isLoadable(driver, Thread.currentThread()
                                     .getContextClassLoader())
                || isLoadable(driver, JdbcDriverAvailability.class.getClassLoader())) {
            return;
        }
        throw new IllegalStateException(missingDriverMessage(dataSourceName, driver));
    }

    /**
     * The message for a driver that is not on the classpath.
     *
     * @param dataSourceName the data source name
     * @param driver the driver class name
     * @return the message
     */
    static String missingDriverMessage(String dataSourceName, String driver) {
        String[] known = KNOWN_DRIVERS.get(driver);
        String what = known != null ? "the " + known[0] + " JDBC driver (" + known[1] + ")" : "the jar that provides it";
        return "The JDBC driver class [" + driver + "] of data source [" + dataSourceName + "] is not on the classpath."
                + " Dirigible bundles only the H2 and PostgreSQL drivers; add " + what
                + " to the application pom, declare it in a project.json dependency with \"scope\": \"platform\","
                + " or drop the jar into the /modules directory (loader.path).";
    }

    private static boolean isLoadable(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            return false;
        }
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
