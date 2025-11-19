/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.config;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The Class MultiTenantConnectionProviderImpl.
 */
@Component
public class MultiTenantConnectionProviderImpl extends AbstractMultiTenantConnectionProvider<String> {

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The datasource. */
    private final DataSource datasource;

    /**
     * Instantiates a new multi tenant connection provider impl.
     *
     * @param datasourcesManager the datasources manager
     * @param datasource the datasource
     */
    @Autowired
    public MultiTenantConnectionProviderImpl(DataSourcesManager datasourcesManager, DataSource datasource) {
        this.datasourcesManager = datasourcesManager;
        this.datasource = datasource;
    }

    /**
     * Gets the connection.
     *
     * @param tenantIdentifier the tenant identifier
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = this.datasourcesManager.getDefaultDataSource()
                                                       .getConnection();
        return connection;
    }

    /**
     * Gets the any connection.
     *
     * @return the any connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getAnyConnection() throws SQLException {
        return datasourcesManager.getDefaultDataSource()
                                 .getConnection();
    }

    /**
     * Release any connection.
     *
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Release connection.
     *
     * @param tenantIdentifier the tenant identifier
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Supports aggressive release.
     *
     * @return true, if successful
     */
    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * Gets the any connection provider.
     *
     * @return the any connection provider
     */
    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return null;
    }

    /**
     * Select connection provider.
     *
     * @param tenantIdentifier the tenant identifier
     * @return the connection provider
     */
    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return null;
    }

}
