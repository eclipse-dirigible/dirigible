/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.config;

import org.quartz.utils.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The Class CustomConnectionProvider.
 */
class CustomConnectionProvider implements ConnectionProvider {

    /** The data source. */
    private final DataSource dataSource;

    /**
     * Instantiates a new custom connection provider.
     *
     * @param dataSource the data source
     */
    public CustomConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shutdown.
     */
    @Override
    public void shutdown() {
        // nothing to shutdown
    }


    /**
     * Initialize.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void initialize() throws SQLException {
        // nothing to init
    }
}
