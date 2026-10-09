/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.schema;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The connection one schema pass walks its tables and views on, re-acquired from the data source
 * once a refused statement has left it unusable (#7766).
 * <p>
 * Some refusals are fatal for the connection itself - PostgreSQL's SQLSTATE {@code 0A000}, after
 * which the pool marks the connection broken - and every later statement on it fails with
 * {@code Connection is closed}. Without the re-acquisition one bad table cost the schema every
 * table declared after it.
 */
public final class SchemaPassConnection implements AutoCloseable {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemaPassConnection.class);

    /** How long a validity check after a refusal may take. */
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    /** The data source the connection is taken from. */
    private final DataSource dataSource;

    /** The current connection, null until first asked for. */
    private Connection connection;

    /**
     * Instantiates a schema pass connection; nothing is acquired until {@link #get()}.
     *
     * @param dataSource the data source of the schema
     */
    public SchemaPassConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * The connection to run the next statement on.
     *
     * @return the connection
     * @throws SQLException if no connection can be acquired
     */
    public Connection get() throws SQLException {
        if (connection == null) {
            connection = dataSource.getConnection();
        }
        return connection;
    }

    /**
     * Replaces the connection when the refusal that just happened left it closed or invalid, so the
     * next table starts on a usable one. A connection that is still valid is kept.
     *
     * @throws SQLException if a replacement cannot be acquired
     */
    void recoverAfterRefusal() throws SQLException {
        if (connection == null || isUsable(connection)) {
            return;
        }
        logger.warn("The schema pass connection is no longer usable after a refused statement - acquiring a new one");
        try {
            close();
        } catch (SQLException e) {
            logger.warn("Failed to close the unusable schema pass connection", e);
        }
        connection = dataSource.getConnection();
    }

    private static boolean isUsable(Connection connection) {
        try {
            return !connection.isClosed() && connection.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            logger.debug("Validity check of the schema pass connection failed", e);
            return false;
        }
    }

    /**
     * Returns the current connection to the data source.
     *
     * @throws SQLException if closing the connection fails
     */
    @Override
    public void close() throws SQLException {
        Connection current = connection;
        connection = null;
        if (current != null) {
            current.close();
        }
    }
}
