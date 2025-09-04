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
import org.eclipse.dirigible.database.persistence.utils.DatabaseMetadataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component("BuildExportTopologyTask_ExportSchemaProcess") // used in the bpmn process
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
        Set<String> includedTables = context.getIncludedTables();
        Set<String> excludedTables = context.getExcludedTables();
        String schema = context.getSchema();
        String dataSourceName = context.getDataSource();

        DirigibleDataSource dataSource = datasourceManager.getDataSource(dataSourceName);
        try {
            Set<String> targetTables = determineInitialTargetTables(dataSource, schema, includedTables, excludedTables);

            Set<String> tablesDependencies = DatabaseMetadataUtil.getTableDependencies(targetTables, schema, dataSource);
            targetTables.addAll(tablesDependencies);

            Set<String> tablesMismatch = new HashSet<>(tablesDependencies);
            tablesMismatch.retainAll(excludedTables);
            if (!tablesMismatch.isEmpty()) {
                String errorMessage = "Exclude tables [" + excludedTables
                        + "] cannot be removed from the export because they are dependencies for some of the target tables [" + targetTables
                        + "]. Conflicting tables: " + tablesMismatch;
                throw new SchemaExportException(errorMessage);
            }
            LOGGER.debug("Determined tables for export: {} ", targetTables);

            List<String> exportTopology = schemaTopologyService.sortTopologically(dataSource, schema, targetTables);
            LOGGER.debug("Determined export topology {}", exportTopology);
            context.setExportTopology(exportTopology);

        } catch (SQLException | RuntimeException ex) {
            throw new SchemaExportException("Failed to export topology of schema [" + schema + "] in datasource [" + dataSource + "]", ex);
        }
    }

    private Set<String> determineInitialTargetTables(DirigibleDataSource dataSource, String schema, Set<String> includedTables,
            Set<String> excludedTables) throws SQLException {
        Set<String> targetTables;
        if (includedTables.isEmpty()) {
            List<String> schemaTables = DatabaseMetadataUtil.getTablesInSchema(dataSource, schema);
            targetTables = null == schemaTables ? Collections.emptySet() : new HashSet<>(schemaTables);
        } else {
            targetTables = includedTables;
        }

        targetTables.removeAll(excludedTables);

        return targetTables;
    }

}
