/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.data.transfer.callback.DataTransferCallbackHandler;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DataTransferReverseTableProcessor.
 */
public class DataTransferReverseTableProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataTransferReverseTableProcessor.class);

    /**
     * Reverse tables.
     *
     * @param dataSource the data source
     * @param schemaName the schema name
     * @param handler the handler
     * @return the list
     * @throws SQLException the SQL exception
     */
    public static List<PersistenceTableModel> reverseTables(DataSource dataSource, String schemaName, DataTransferCallbackHandler handler)
            throws SQLException {

        if (handler != null) {
            handler.metadataLoadingStarted();
        }

        List<PersistenceTableModel> tables = new ArrayList<PersistenceTableModel>();

        List<String> tableNames = DatabaseMetadataUtil.getTablesInSchema(dataSource, schemaName);
        if (tableNames != null) {
            for (String tableName : tableNames) {
                PersistenceTableModel persistenceTableModel = reverseTable(dataSource, schemaName, tableName);
                tables.add(persistenceTableModel);
            }
        } else {
            String error = schemaName + " does not exist in the target database";
            if (logger.isErrorEnabled()) {
                logger.error(error);
            }
            if (handler != null) {
                handler.metadataLoadingError(error);
            }
        }

        if (handler != null) {
            handler.metadataLoadingFinished(tables.size());
        }

        return tables;
    }

    /**
     * Reverse table.
     *
     * @param dataSource the data source
     * @param schemaName the schema name
     * @param tableName the table name
     * @return the persistence table model
     * @throws SQLException the SQL exception
     */
    public static PersistenceTableModel reverseTable(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        return DatabaseMetadataUtil.getTableMetadata(tableName, schemaName, dataSource);
    }

}
