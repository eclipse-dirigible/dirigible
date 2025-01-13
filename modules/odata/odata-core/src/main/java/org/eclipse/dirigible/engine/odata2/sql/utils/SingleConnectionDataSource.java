/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.utils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * The operations in a OData changeset must be executed within one transaction (connection)
 * Therefore we create a single and non-closable connection datasource for simplicity.
 */
public class SingleConnectionDataSource implements DataSource {


    /** The non closable connection. */
    private final Connection nonClosableConnection;

    /**
     * Instantiates a new single connection data source.
     *
     * @param connection the connection
     */
    public SingleConnectionDataSource(Connection connection) {
        this.nonClosableConnection = getNonClosableConnection(connection);
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     */
    @Override
    public Connection getConnection() {
        return nonClosableConnection;
    }

    /**
     * Gets the connection.
     *
     * @param username the username
     * @param password the password
     * @return the connection
     */
    @Override
    public Connection getConnection(String username, String password) {
        return nonClosableConnection;
    }

    /**
     * Gets the log writer.
     *
     * @return the log writer
     */
    @Override
    public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Sets the log writer.
     *
     * @param printWriter the new log writer
     */
    @Override
    public void setLogWriter(PrintWriter printWriter) {
        throw new UnsupportedOperationException("Not supported");

    }

    /**
     * Sets the login timeout.
     *
     * @param i the new login timeout
     */
    @Override
    public void setLoginTimeout(int i) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Gets the login timeout.
     *
     * @return the login timeout
     */
    @Override
    public int getLoginTimeout() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Gets the parent logger.
     *
     * @return the parent logger
     */
    @Override
    public Logger getParentLogger() {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param aClass the a class
     * @return the t
     */
    @Override
    public <T> T unwrap(Class<T> aClass) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Checks if is wrapper for.
     *
     * @param aClass the a class
     * @return true, if is wrapper for
     */
    @Override
    public boolean isWrapperFor(Class<?> aClass) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Gets the non closable connection.
     *
     * @param connection the connection
     * @return the non closable connection
     */
    Connection getNonClosableConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(this.getClass()
                                                       .getClassLoader(),
                new Class<?>[] {Connection.class}, new NonClosableConnection(connection));
    }


    /**
     * The Class NonClosableConnection.
     */
    private static class NonClosableConnection implements InvocationHandler {

        /** The delegate. */
        private final Connection delegate;

        /**
         * Instantiates a new non closable connection.
         *
         * @param delegate the delegate
         */
        public NonClosableConnection(Connection delegate) {
            this.delegate = delegate;
        }

        /**
         * Invoke.
         *
         * @param proxy the proxy
         * @param method the method
         * @param args the args
         * @return the object
         * @throws Throwable the throwable
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            switch (method.getName()) {
                case "equals":
                    return (proxy == args[0]);
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "close":
                    return null;
                case "isClosed":
                    return delegate.isClosed();
            }
            try {
                return method.invoke(this.delegate, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }
}
