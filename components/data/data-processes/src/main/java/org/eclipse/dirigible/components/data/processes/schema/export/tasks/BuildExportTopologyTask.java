/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.export.tasks;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.transfer.service.DataTransferSchemaTopologyService;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableRelationModel;
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component("BuildExportTopologyTask") // used in the bpmn process
public class BuildExportTopologyTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildExportTopologyTask.class);

    private final DataSourcesManager datasourceManager;
    private final DataTransferSchemaTopologyService schemaTopologyService;

    BuildExportTopologyTask(DataSourcesManager datasourceManager, DataTransferSchemaTopologyService schemaTopologyService) {
        this.datasourceManager = datasourceManager;
        this.schemaTopologyService = schemaTopologyService;
    }

    @Override
    protected void execute(ExportProcessContext context) {
        LOGGER.info("Executing {} with context {}", this, context);

        Set<String> includedTables = context.getIncludedTables();
        LOGGER.info("includedTables {}", includedTables);

        Set<String> excludedTables = context.getExcludedTables();
        LOGGER.info("excludedTables {}", excludedTables);

        String schema = context.getSchema();
        LOGGER.info("schema {}", schema);

        String dataSourceName = context.getDataSource();
        LOGGER.info("dataSource {}", dataSourceName);

        String exportPath = context.getExportPath();
        LOGGER.info("exportPath {}", exportPath);

        DirigibleDataSource dataSource = datasourceManager.getDataSource(dataSourceName);
        try {
            Set<String> targetTables = null;
            if (includedTables.isEmpty()) {
                targetTables = new HashSet<>(DatabaseMetadataUtil.getTablesInSchema(dataSource, schema));
            } else {
                targetTables = includedTables;
            }

            Set<String> tablesDependencies = getTableDependencies(targetTables, schema, dataSource);
            targetTables.addAll(tablesDependencies);

            Set<String> tablesMismatch = new HashSet<>(tablesDependencies);
            tablesMismatch.retainAll(excludedTables);
            if (!tablesMismatch.isEmpty()) {
                String errorMessage = "Exclude tables [" + excludedTables
                        + "] cannot be removed from the export because they are dependencies for the target tables [" + targetTables
                        + "]. Conflicting tables: " + tablesMismatch;
                throw new SchemaExportException(errorMessage);
            }

            targetTables.removeAll(excludedTables);

            LOGGER.info("All needed tables for export are {} ", targetTables);
            List<String> topology = schemaTopologyService.sortTopologically(dataSource, schema);
            LOGGER.info("Determined Topology {}", topology);
        } catch (SQLException ex) {
            throw new SchemaExportException("Failed to export topology of schema [" + schema + "] in datasource [" + dataSource + "]", ex);
        }
    }

    private Set<String> getTableDependencies(Set<String> tables, String schema, DirigibleDataSource dataSource) throws SQLException {
        Set<String> dependencies = new HashSet<>();
        for (String table : tables) {
            PersistenceTableModel tableMetadata = DatabaseMetadataUtil.getTableMetadata(table, schema, dataSource);
            List<PersistenceTableRelationModel> relations = tableMetadata.getRelations();
            if (null != relations) {
                Set<String> tableDependencies = relations.stream()
                                                         .map(m -> m.getToTableName())
                                                         .collect(Collectors.toSet());
                dependencies.addAll(tableDependencies);
            }
        }
        return dependencies;
    }

}
