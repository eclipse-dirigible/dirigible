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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiTenantConnectionProviderImpl extends AbstractMultiTenantConnectionProvider<String> {

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    private final DataSource datasource;

    @Autowired
    public MultiTenantConnectionProviderImpl(DataSourcesManager datasourcesManager, DataSource datasource) {
        this.datasourcesManager = datasourcesManager;
        this.datasource = datasource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = this.datasourcesManager.getDefaultDataSource()
                                                       .getConnection();
        return connection;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        Connection connection = this.datasource.getConnection();
        return connection;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return null;
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return null;
    }

}
