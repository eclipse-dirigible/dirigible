/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.perf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The PostgreSQL driver, with every statement it executes counted against the HTTP request the
 * executing thread is serving ({@link StatementCounter}) - so a step of
 * {@link PerformanceBaselineIT} reports how many statements one request of it costs, and which one
 * repeats, instead of leaving an N+1 to be guessed from a latency.
 *
 * <p>
 * Configured as the data sources' driver class with their ordinary {@code jdbc:postgresql:} URL,
 * which keeps the platform's database detection (by URL prefix) seeing PostgreSQL. Hikari
 * instantiates it by class name, so it is public with a public no-argument constructor, and it is
 * deliberately NOT registered with {@link java.sql.DriverManager}: a lookup by URL elsewhere in the
 * JVM keeps reaching the real driver.
 */
public final class StatementCountingDriver implements Driver {

    private final Driver delegate = new org.postgresql.Driver();

    /** Instantiated by Hikari, by class name. */
    public StatementCountingDriver() {}

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Connection connection = delegate.connect(url, info);
        return connection == null ? null : proxy(Connection.class, new ConnectionHandler(connection));
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return delegate.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return delegate.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(StatementCountingDriver.class.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    /** Hands out statements that count their executions; everything else goes to the connection. */
    private static final class ConnectionHandler implements InvocationHandler {

        private final Connection connection;

        ConnectionHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = StatementCountingDriver.invoke(connection, method, args);
            return switch (method.getName()) {
                case "createStatement" -> proxy(Statement.class, new StatementHandler((Statement) result, null));
                case "prepareStatement" -> proxy(PreparedStatement.class, new StatementHandler((Statement) result, (String) args[0]));
                case "prepareCall" -> proxy(CallableStatement.class, new StatementHandler((Statement) result, (String) args[0]));
                default -> result;
            };
        }
    }

    /**
     * Counts every {@code execute*} call - a prepared statement under the SQL it was prepared with, a
     * plain one under the SQL it is handed.
     */
    private static final class StatementHandler implements InvocationHandler {

        private final Statement statement;
        private final String preparedSql;

        StatementHandler(Statement statement, String preparedSql) {
            this.statement = statement;
            this.preparedSql = preparedSql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName()
                      .startsWith("execute")) {
                StatementCounter.record(args != null && args.length > 0 && args[0] instanceof String sql ? sql : preparedSql);
            }
            return StatementCountingDriver.invoke(statement, method, args);
        }
    }
}
