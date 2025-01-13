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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.eclipse.dirigible.components.base.artefact.topology.TopologicalSorter;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The Class DataTransferSchemaTopologyService.
 */
@Service
public class DataTransferSchemaTopologyService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataTransferSchemaTopologyService.class);

    /**
     * Sort topologically.
     *
     * @param dataSource the data source
     * @param schemaName the schema name
     * @return the list
     * @throws SQLException the SQL exception
     */
    public List<String> sortTopologically(DataSource dataSource, String schemaName) throws SQLException {

        List<PersistenceTableModel> tables = new ArrayList<PersistenceTableModel>();

        List<String> tableNames = DatabaseMetadataUtil.getTablesInSchema(dataSource, schemaName);
        if (tableNames != null) {
            for (String tableName : tableNames) {
                PersistenceTableModel persistenceTableModel = DatabaseMetadataUtil.getTableMetadata(tableName, schemaName, dataSource);
                tables.add(persistenceTableModel);
            }
        } else {
            String error = schemaName + " does not exist in the selected database";
            if (logger.isErrorEnabled()) {
                logger.error(error);
            }
        }

        tables = sortTables(tables);

        return tables.stream()
                     .map(PersistenceTableModel::getTableName)
                     .collect(Collectors.toList());
    }

    /**
     * Sort tables.
     *
     * @param tables the tables
     * @return the list
     */
    private List<PersistenceTableModel> sortTables(List<PersistenceTableModel> tables) {

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

        return result;
    }

}
