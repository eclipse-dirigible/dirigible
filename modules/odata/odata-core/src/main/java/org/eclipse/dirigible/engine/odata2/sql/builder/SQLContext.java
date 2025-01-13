/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import org.apache.olingo.odata2.api.processor.ODataContext;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DatabaseSystemDeterminer;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.SERVICE_UNAVAILABLE;

/**
 * The Class SQLContext.
 */
public class SQLContext {

    /** The database product. */
    private final DatabaseSystem databaseSystem;

    /** The odata context. */
    private ODataContext odataContext;

    /**
     * Instantiates a new SQL context.
     */
    public SQLContext() {
        databaseSystem = DatabaseSystem.DERBY;
    }

    /**
     * Instantiates a new SQL context.
     *
     * @param connection the connection
     * @param odataContext the odata context
     */
    public SQLContext(final Connection connection, final ODataContext odataContext) throws SQLException {
        this.odataContext = odataContext;
        this.databaseSystem = DatabaseSystemDeterminer.determine(connection);
    }

    /**
     * Instantiates a new SQL context.
     *
     * @param databaseSystem the database system
     */
    public SQLContext(final DatabaseSystem databaseSystem) {
        this.databaseSystem = databaseSystem;
    }

    /**
     * Gets the database product.
     *
     * @return the database product
     */
    public DatabaseSystem getDatabaseSystem() {
        return databaseSystem;
    }

    /**
     * Gets the odata context.
     *
     * @return the odata context
     */
    public ODataContext getOdataContext() {
        return odataContext;
    }

    /**
     * Gets the database name.
     *
     * @param metadata the metadata
     * @return the database name
     */
    private String getDatabaseName(final DatabaseMetaData metadata) {
        try {
            return metadata.getDatabaseProductName();
        } catch (SQLException e) {
            throw new OData2Exception("Unable to get the database product name", SERVICE_UNAVAILABLE, e);
        }
    }

}
