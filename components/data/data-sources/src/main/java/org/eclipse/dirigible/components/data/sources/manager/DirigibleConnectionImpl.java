/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleConnection;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;

class DirigibleConnectionImpl implements DirigibleConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleConnectionImpl.class);

    private final String dataSourceName;
    private final Connection connection;
    private final DatabaseSystem databaseSystem;

    DirigibleConnectionImpl(String dataSourceName, Connection connection, DatabaseSystem databaseSystem) {
        this.dataSourceName = dataSourceName;
        this.connection = connection;
        this.databaseSystem = databaseSystem;
    }

    @FunctionalInterface
    interface ExecuteWithConnection<R> {

        R execute(Connection connection) throws SQLException;
    }

    @Override
    public Statement createStatement() throws SQLException {
        return executeWithConnection(Connection::createStatement);
    }

    private <R> R executeWithConnection(ExecuteWithConnection<R> execution) throws SQLException {
        if (Objects.equals("SystemDB", dataSourceName)) {
            LOGGER.debug("Executing directly for [{}]", dataSourceName);
            return execution.execute(getConnection());
        }

        boolean activeSpringTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        if (activeSpringTransaction) {
            return execution.execute(getConnection());
        } else {
            return executeWithEnabledAutoCommit(execution);
        }

        // if (!activeSpringTransaction) {
        // LOGGER.debug("There IS NO active spring transaction for data source [{}]. Executing with enabled
        // auto commit. ",
        // dataSourceName);
        // return executeWithEnabledAutoCommit(execution);
        // }
        // if (isTransactionForCurrentDataSource()) {
        // LOGGER.debug(
        // "There IS active spring transaction for data source [{}] but it IS not for current data source.
        // Transaction name [{}]. Executing with enabled auto commit.",
        // dataSourceName, TransactionSynchronizationManager.getCurrentTransactionName());
        // return execution.execute(getConnection());
        // } else {
        // LOGGER.debug(
        // "There IS active spring transaction but IT IS NOT for data source [{}]. Transaction name [{}].
        // Executing with enabled auto commit. ",
        // dataSourceName, TransactionSynchronizationManager.getCurrentTransactionName());
        // return executeWithEnabledAutoCommit(execution);
        // }
    }

    private <R> R executeWithEnabledAutoCommit(ExecuteWithConnection<R> execution) throws SQLException {
        R result = execution.execute(connection);
        connection.commit();

        return result;
    }

    private Connection getConnection() {
        return connection;
    }

    private boolean isTransactionForCurrentDataSource() {
        Set<DirigibleDataSource> transactionDataSource = getTransactionDataSources();
        return transactionDataSource.stream()
                                    .anyMatch(ds -> Objects.equals(ds.getName(), dataSourceName));
    }

    private Set<DirigibleDataSource> getTransactionDataSources() {
        Set<Object> transactionDataSources = TransactionSynchronizationManager.getResourceMap()
                                                                              .keySet();
        Set<DirigibleDataSource> dataSources = new HashSet<>();
        for (Object dataSource : transactionDataSources) {
            if (dataSource instanceof DirigibleDataSource dirigibleDataSource) {
                dataSources.add(dirigibleDataSource);
            }
        }
        return dataSources;
    }

    private boolean getConnectionAutoCommit() {
        try {
            return getConnection().getAutoCommit();
        } catch (SQLException e) {
            LOGGER.warn("Failed to get auto commit for connection [{}]. Returning false.", getConnection());
            return false;
        }
    }

    private void setConnectionAutoCommit(boolean autoCommit) {
        try {
            getConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            LOGGER.warn("Failed to set auto commit to [{}] for [{}]", autoCommit, getConnection(), e);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql));
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return executeWithConnection(c -> c.prepareCall(sql));
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return executeWithConnection(c -> c.nativeSQL(sql));
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return getConnection().getAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        getConnection().setAutoCommit(autoCommit);
    }

    @Override
    public void commit() throws SQLException {
        getConnection().commit();
    }

    @Override
    public void rollback() throws SQLException {
        getConnection().rollback();
    }

    @Override
    public void close() throws SQLException {
        getConnection().close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return getConnection().isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return executeWithConnection(Connection::getMetaData);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return getConnection().isReadOnly();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        getConnection().setReadOnly(readOnly);
    }

    @Override
    public String getCatalog() throws SQLException {
        return getConnection().getCatalog();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        getConnection().setCatalog(catalog);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return getConnection().getTransactionIsolation();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        getConnection().setTransactionIsolation(level);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return getConnection().getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        getConnection().clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return executeWithConnection(c -> c.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return executeWithConnection(c -> c.prepareCall(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return getConnection().getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        getConnection().setTypeMap(map);
    }

    @Override
    public int getHoldability() throws SQLException {
        return getConnection().getHoldability();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        getConnection().setHoldability(holdability);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return getConnection().setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return getConnection().setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        getConnection().rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        getConnection().releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return executeWithConnection(c -> c.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return executeWithConnection(c -> c.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql, autoGeneratedKeys));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql, columnIndexes));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return executeWithConnection(c -> c.prepareStatement(sql, columnNames));
    }

    @Override
    public Clob createClob() throws SQLException {
        return getConnection().createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return getConnection().createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return getConnection().createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return getConnection().createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return getConnection().isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        getConnection().setClientInfo(name, value);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return getConnection().getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return getConnection().getClientInfo();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        getConnection().setClientInfo(properties);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return getConnection().createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return getConnection().createStruct(typeName, attributes);
    }

    @Override
    public String getSchema() throws SQLException {
        return getConnection().getSchema();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        getConnection().setSchema(schema);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        getConnection().abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        getConnection().setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return getConnection().getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getConnection().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getConnection().isWrapperFor(iface);
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
    public String toString() {
        return "DirigibleConnectionImpl{" + "connection=" + connection + ", databaseSystem=" + databaseSystem + '}';
    }
}
