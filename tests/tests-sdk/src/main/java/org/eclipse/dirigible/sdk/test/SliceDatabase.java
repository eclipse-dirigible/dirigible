/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.h2.jdbcx.JdbcDataSource;

/**
 * The slice's own in-memory H2 database, presented as the platform's default
 * {@link DirigibleDataSource}. Nothing else uses it, so wiping it between tests is safe.
 */
final class SliceDatabase implements AutoCloseable {

    private static final String DATA_SOURCE_NAME = "DefaultDB";
    private static final AtomicInteger INSTANCES = new AtomicInteger();

    private final JdbcDataSource h2 = new JdbcDataSource();
    private final DirigibleDataSource dataSource;

    SliceDatabase() {
        // DB_CLOSE_DELAY keeps the database alive between connections; close() shuts it down.
        h2.setURL("jdbc:h2:mem:intent-slice-" + INSTANCES.incrementAndGet() + ";DB_CLOSE_DELAY=-1");
        dataSource = proxy(DirigibleDataSource.class, new DataSourceHandler());
    }

    /** @return the database as the platform's default datasource */
    DirigibleDataSource dataSource() {
        return dataSource;
    }

    /**
     * Empties every table and restarts every identity, keeping the schema - each test starts from the
     * state a freshly provisioned application has.
     *
     * @throws SQLException if a statement fails
     */
    void clear() throws SQLException {
        try (Connection connection = h2.getConnection(); Statement statement = connection.createStatement()) {
            List<String> tables = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (String table : tables) {
                    statement.execute("TRUNCATE TABLE \"" + table.replace("\"", "\"\"") + "\" RESTART IDENTITY");
                }
            } finally {
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        }
    }

    @Override
    public void close() throws SQLException {
        try (Connection connection = h2.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(SliceDatabase.class.getClassLoader(), new Class<?>[] {type}, handler));
    }

    /**
     * Invokes the same method on the plain JDBC object, rethrowing what it threw rather than a wrapper.
     * The method is looked up on the target: the platform interfaces redeclare some JDBC methods
     * covariantly ({@code DirigibleDataSource.getConnection()}), which the target does not implement.
     */
    private static Object delegate(Object target, Method method, Object[] args) throws Throwable {
        try {
            return target.getClass()
                         .getMethod(method.getName(), method.getParameterTypes())
                         .invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    /**
     * What the platform asks a datasource beyond JDBC: its name, its database system, its lifecycle.
     */
    private final class DataSourceHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "getConnection" -> proxy(DirigibleConnection.class, new ConnectionHandler((Connection) delegate(h2, method, args)));
                case "getName" -> DATA_SOURCE_NAME;
                case "getDatabaseSystem" -> DatabaseSystem.H2;
                case "isOfType" -> args[0] == DatabaseSystem.H2;
                case "getTransactionManager" -> Optional.empty();
                case "isInUse" -> Boolean.FALSE;
                // The slice owns the database's lifecycle; nothing it runs may close it or swap its
                // transaction manager.
                case "close", "setTransactionManager" -> null;
                case "toString" -> "IntentSlice " + DATA_SOURCE_NAME + " [" + h2.getURL() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> delegate(h2, method, args);
            };
        }
    }

    /** A JDBC connection that also answers which database system it talks to. */
    private static final class ConnectionHandler implements InvocationHandler {

        private final Connection connection;

        private ConnectionHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "getDatabaseSystem" -> DatabaseSystem.H2;
                case "isOfType" -> args[0] == DatabaseSystem.H2;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> delegate(connection, method, args);
            };
        }
    }
}
