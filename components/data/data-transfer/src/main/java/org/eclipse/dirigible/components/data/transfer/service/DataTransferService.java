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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.topology.TopologicalSorter;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.transfer.callback.DataTransferCallbackHandler;
import org.eclipse.dirigible.components.data.transfer.callback.DummyDataTransferCallbackHandler;
import org.eclipse.dirigible.components.data.transfer.domain.DataTransfer;
import org.eclipse.dirigible.components.data.transfer.domain.DataTransferConfiguration;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.processors.table.PersistenceCreateTableProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class DataTransferManager.
 */
@Component
public class DataTransferService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataTransferService.class);

    /** The Constant DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE. */
    private static final String DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE = "DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE";

    /** The Constant DEFAULT_BATCH_SIZE. */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /** The batch size. */
    private static int BATCH_SIZE = 1000;


    /** The data sources manager. */
    private final DataSourcesManager dataSourcesManager;

    /**
     * Instantiates a new data transfer service.
     *
     * @param dataSourcesManager the data sources manager
     */
    @Autowired
    public DataTransferService(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;

    }

    /**
     * Gets the data sources manager.
     *
     * @return the data sources manager
     */
    public DataSourcesManager getDataSourcesManager() {
        return dataSourcesManager;
    }

    /**
     * Transfer.
     *
     * @param definition the definition
     * @param handler the handler
     * @throws Exception the exception
     */
    public final void transfer(DataTransfer definition, DataTransferCallbackHandler handler) throws Exception {
        DataSource source = getDataSourcesManager().getDataSource(definition.getSource());
        DataSource target = getDataSourcesManager().getDataSource(definition.getTarget());
        transfer(source, target, definition.getConfiguration(), handler);
    }

    /**
     * Transfer.
     *
     * @param source the source
     * @param target the target
     * @param configuration the configuration
     * @param handler the handler
     * @throws Exception the exception
     */
    public final void transfer(DataSource source, DataSource target, DataTransferConfiguration configuration,
            DataTransferCallbackHandler handler) throws Exception {

        if (handler == null) {
            handler = new DummyDataTransferCallbackHandler();
        }

        BATCH_SIZE = Configuration.getAsInt(DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE, DEFAULT_BATCH_SIZE);

        handler.transferStarted(configuration);

        try (Connection sourceConnection = source.getConnection()) {

            try (Connection targetConnection = target.getConnection()) {

                List<PersistenceTableModel> tables =
                        DataTransferReverseTableProcessor.reverseTables(source, configuration.getSourceSchema(), handler);
                if (handler.isStopped()) {
                    return;
                }
                tables = sortTables(tables, handler);
                sourceConnection.setSchema(configuration.getSourceSchema());
                targetConnection.setSchema(configuration.getTargetSchema());
                transferDataTables(tables, sourceConnection, targetConnection, handler);

                handler.transferFinished(tables.size());

            } catch (SQLException e) {
                String error = "Error occured when trying to connect to the target database";
                if (logger.isErrorEnabled()) {
                    logger.error(error, e);
                }
                handler.transferFailed(error);
                throw new Exception(e);
            }

        } catch (SQLException e) {
            String error = "Error occured when trying to connect to the source database";
            if (logger.isErrorEnabled()) {
                logger.error(error, e);
            }
            handler.transferFailed(error);
            throw new Exception(e);
        }

    }

    /**
     * Sort tables.
     *
     * @param tables the tables
     * @param handler the handler
     * @return the list
     */
    private List<PersistenceTableModel> sortTables(List<PersistenceTableModel> tables, DataTransferCallbackHandler handler) {

        handler.sortingStarted(tables);

        // Prepare for sorting
        List<DataTransferSortableTableWrapper> list = new ArrayList<DataTransferSortableTableWrapper>();
        Map<String, DataTransferSortableTableWrapper> wrappers = new HashMap<String, DataTransferSortableTableWrapper>();
        for (PersistenceTableModel tableModel : tables) {
            DataTransferSortableTableWrapper wrapper = new DataTransferSortableTableWrapper(tableModel, wrappers);
            list.add(wrapper);
        }

        // Topological sorting by dependencies
        TopologicalSorter<DataTransferSortableTableWrapper> sorter = new TopologicalSorter<>();
        list = sorter.sort(list);

        // Prepare result
        List<PersistenceTableModel> result = new ArrayList<PersistenceTableModel>();
        for (DataTransferSortableTableWrapper wrapper : list) {
            PersistenceTableModel tableModel = wrapper.getTableModel();
            result.add(tableModel);
        }

        handler.sortingFinished(result);

        return result;
    }

    /**
     * Transfer data.
     *
     * @param tables the tables
     * @param sourceConnection the source connection
     * @param targetConnection the target connection
     * @param handler the handler
     */
    private void transferDataTables(List<PersistenceTableModel> tables, Connection sourceConnection, Connection targetConnection,
            DataTransferCallbackHandler handler) {

        handler.dataTransferStarted();

        for (PersistenceTableModel tableModel : tables) {
            if (handler.isStopped()) {
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Data transfer of table %s has been started...", tableModel.getTableName()));
            }
            handler.tableTransferStarted(tableModel.getTableName());
            try {

                if (!SqlFactory.getNative(sourceConnection)
                               .existsTable(targetConnection, tableModel.getTableName())) {
                    PersistenceCreateTableProcessor createTableProcessor = new PersistenceCreateTableProcessor(null);
                    createTableProcessor.create(targetConnection, tableModel);
                } else {
                    String countSQL = SqlFactory.getNative(sourceConnection)
                                                .select()
                                                .column("count(*)")
                                                .from(tableModel.getTableName())
                                                .build();
                    try (PreparedStatement pstmtTarget = targetConnection.prepareStatement(countSQL)) {
                        ResultSet rs = pstmtTarget.executeQuery();
                        while (rs.next()) {
                            int count = rs.getInt(1);
                            if (count > 0) {
                                handler.tableSkipped(tableModel.getTableName(), "table exists and it is not empty");
                                continue;
                            }
                        }
                    }
                }

                String selectSQL = SqlFactory.getNative(sourceConnection)
                                             .select()
                                             .column("*")
                                             .from(tableModel.getTableName())
                                             .build();

                handler.tableSelectSQL(selectSQL);

                int transferedRecords = 0;

                try (PreparedStatement pstmtSource = sourceConnection.prepareStatement(selectSQL)) {
                    try (ResultSet rs = pstmtSource.executeQuery()) {
                        ResultSetMetaData resultSetMetaData = rs.getMetaData();

                        InsertBuilder insertBuilder = SqlFactory.getNative(targetConnection)
                                                                .insert()
                                                                .into(tableModel.getTableName());
                        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                            String columnName = resultSetMetaData.getColumnName(i);
                            insertBuilder.column(columnName);
                        }

                        String insertSQL = insertBuilder.build();
                        handler.tableInsertSQL(insertSQL);

                        try (PreparedStatement pstmtTarget = targetConnection.prepareStatement(insertSQL)) {
                            while (rs.next()) {
                                if (handler.isStopped()) {
                                    return;
                                }
                                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                                    int type = resultSetMetaData.getColumnType(i);
                                    switch (type) {
                                        case java.sql.Types.ARRAY:
                                            pstmtTarget.setArray(i, rs.getArray(i));
                                            break;
                                        case java.sql.Types.BIGINT:
                                            pstmtTarget.setLong(i, rs.getLong(i));
                                            break;
                                        case java.sql.Types.BINARY:
                                            pstmtTarget.setBinaryStream(i, rs.getBinaryStream(i));
                                            break;
                                        case java.sql.Types.BIT:
                                            pstmtTarget.setBoolean(i, rs.getBoolean(i));
                                            break;
                                        case java.sql.Types.BLOB:
                                            pstmtTarget.setBlob(i, rs.getBlob(i));
                                            break;
                                        case java.sql.Types.BOOLEAN:
                                            pstmtTarget.setBoolean(i, rs.getBoolean(i));
                                            break;
                                        case java.sql.Types.CHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.CLOB:
                                            pstmtTarget.setClob(i, rs.getClob(i));
                                            break;
                                        case java.sql.Types.DATE:
                                            pstmtTarget.setDate(i, rs.getDate(i));
                                            break;
                                        case java.sql.Types.DECIMAL:
                                            pstmtTarget.setBigDecimal(i, rs.getBigDecimal(i));
                                            break;
                                        case java.sql.Types.DOUBLE:
                                            pstmtTarget.setDouble(i, rs.getDouble(i));
                                            break;
                                        case java.sql.Types.FLOAT:
                                            pstmtTarget.setFloat(i, rs.getFloat(i));
                                            break;
                                        case java.sql.Types.INTEGER:
                                            pstmtTarget.setInt(i, rs.getInt(i));
                                            break;
                                        case java.sql.Types.LONGNVARCHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.LONGVARBINARY:
                                            pstmtTarget.setBinaryStream(i, rs.getBinaryStream(i));
                                            break;
                                        case java.sql.Types.LONGVARCHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.NCHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.NCLOB:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.NUMERIC:
                                            pstmtTarget.setDouble(i, rs.getDouble(i));
                                            break;
                                        case java.sql.Types.NVARCHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.REAL:
                                            pstmtTarget.setFloat(i, rs.getFloat(i));
                                            break;
                                        case java.sql.Types.SMALLINT:
                                            pstmtTarget.setShort(i, rs.getShort(i));
                                            break;
                                        case java.sql.Types.TIME:
                                            pstmtTarget.setTime(i, rs.getTime(i));
                                            break;
                                        case java.sql.Types.TIME_WITH_TIMEZONE:
                                            pstmtTarget.setTime(i, rs.getTime(i));
                                            break;
                                        case java.sql.Types.TIMESTAMP:
                                            pstmtTarget.setTimestamp(i, rs.getTimestamp(i));
                                            break;
                                        case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
                                            pstmtTarget.setTimestamp(i, rs.getTimestamp(i));
                                            break;
                                        case java.sql.Types.TINYINT:
                                            pstmtTarget.setByte(i, rs.getByte(i));
                                            break;
                                        case java.sql.Types.VARBINARY:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        case java.sql.Types.VARCHAR:
                                            pstmtTarget.setString(i, rs.getString(i));
                                            break;
                                        default:
                                            pstmtTarget.setObject(i, rs.getObject(i));
                                            break;
                                    }

                                }
                                handler.recordTransferFinished(tableModel.getTableName(), ++transferedRecords);
                                pstmtTarget.addBatch();
                                if (transferedRecords % BATCH_SIZE == 0) {
                                    pstmtTarget.executeBatch();
                                }
                            }
                            if (transferedRecords % BATCH_SIZE != 0) {
                                pstmtTarget.executeBatch();
                            }
                        }
                    }
                }

                String message = String.format("Data of table %s has been transferred successfully.", tableModel.getTableName());
                if (logger.isInfoEnabled()) {
                    logger.info(message);
                }

                handler.tableTransferFinished(tableModel.getTableName(), transferedRecords);

            } catch (Exception e) {
                String error = "Error occured while transferring the data for table: " + tableModel.getTableName();
                if (logger.isErrorEnabled()) {
                    logger.error(error, e);
                }
                handler.tableTransferFailed(tableModel.getTableName(), error + " -> " + e.getMessage());
            }
        }

        handler.dataTransferFinished();
    }


}
