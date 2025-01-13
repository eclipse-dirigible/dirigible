/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.LeakedConnectionsDoctor;
import org.eclipse.dirigible.components.database.ConnectionEnhancer;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The WrappedDataSource of the standard JDBC {@link DataSource} object with added some additional
 * capabilities..
 */
class DirigibleDataSourceImpl implements DirigibleDataSource {

    /** The Constant LOGGER. */
    private static final Logger logger = LoggerFactory.getLogger(DirigibleDataSourceImpl.class);

    private final List<ConnectionEnhancer> connectionEnhancers;
    private final HikariDataSource originalDataSource;
    private final DatabaseSystem databaseSystem;

    /**
     * Wrapper of the default datasource provided by the underlying platform It has some fault tolerance
     * features, which are not available by default in the popular JDBC drivers.
     *
     * @param originalDataSource the original data source
     * @param databaseSystem database type
     */
    DirigibleDataSourceImpl(List<ConnectionEnhancer> allConnectionEnhancers, HikariDataSource originalDataSource,
            DatabaseSystem databaseSystem) {
        this.connectionEnhancers = allConnectionEnhancers.stream()
                                                         .filter(e -> e.isApplicable(databaseSystem))
                                                         .collect(Collectors.toList());
        logger.info("Filtered [{}] connection enhancers out of [{}] for system [{}]: {}", connectionEnhancers.size(),
                allConnectionEnhancers.size(), databaseSystem, connectionEnhancers);
        this.originalDataSource = originalDataSource;
        this.databaseSystem = databaseSystem;
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public DirigibleConnection getConnection() throws SQLException {
        Connection connection = originalDataSource.getConnection();

        enhanceConnection(connection);
        LeakedConnectionsDoctor.registerConnection(connection);

        return new DirigibleConnectionImpl(connection, databaseSystem);
    }

    /**
     * Gets the connection.
     *
     * @param username the username
     * @param password the password
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public DirigibleConnection getConnection(String username, String password) throws SQLException {
        Connection connection = originalDataSource.getConnection(username, password);

        enhanceConnection(connection);
        LeakedConnectionsDoctor.registerConnection(connection);

        return new DirigibleConnectionImpl(connection, databaseSystem);
    }

    private void enhanceConnection(Connection connection) throws SQLException {
        for (ConnectionEnhancer enhancer : connectionEnhancers) {
            enhancer.apply(connection);
        }
    }

    /**
     * Gets the log writer.
     *
     * @return the log writer
     * @throws SQLException the SQL exception
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return originalDataSource.getLogWriter();
    }

    /**
     * Sets the log writer.
     *
     * @param arg0 the new log writer
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLogWriter(PrintWriter arg0) throws SQLException {
        originalDataSource.setLogWriter(arg0);
    }

    /**
     * Gets the login timeout.
     *
     * @return the login timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return originalDataSource.getLoginTimeout();
    }

    /**
     * Sets the login timeout.
     *
     * @param arg0 the new login timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLoginTimeout(int arg0) throws SQLException {
        originalDataSource.setLoginTimeout(arg0);
    }

    /**
     * Checks if is wrapper for.
     *
     * @param arg0 the arg 0
     * @return true, if is wrapper for
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return originalDataSource.isWrapperFor(arg0);
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param arg0 the arg 0
     * @return the t
     * @throws SQLException the SQL exception
     */
    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return originalDataSource.unwrap(arg0);
    }

    /**
     * Gets the parent logger.
     *
     * @return the parent logger
     * @throws SQLFeatureNotSupportedException the SQL feature not supported exception
     */
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public DatabaseSystem getDatabaseSystem() {
        return databaseSystem;
    }

    @Override
    public boolean isOfType(DatabaseSystem databaseSystem) {
        return this.databaseSystem.isOfType(databaseSystem);
    }

    @Override
    public void close() {
        originalDataSource.close();
    }
}
