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

import org.eclipse.dirigible.components.data.management.load.DataSourceMetadataLoader;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component("ExportTableDefinitionTask_ExportSchemaProcess") // used in the bpmn process
class ExportTableDefinitionTask extends BaseExportTask {

    private final DataSourcesManager datasourceManager;
    private final DataSourceMetadataLoader dataSourceMetadataLoader;

    ExportTableDefinitionTask(DataSourcesManager datasourceManager, DataSourceMetadataLoader dataSourceMetadataLoader) {
        this.datasourceManager = datasourceManager;
        this.dataSourceMetadataLoader = dataSourceMetadataLoader;
    }

    @Override
    protected void execute(ExportProcessContext context) {
        String table = context.getCurrentTable();
        String exportPath = context.getExportPath();

        Table tableDefinition = loadTableDefinition(context, table);
        String fileName = ExportFilesHelper.createTableDefinitionFilename(table);
        saveObjectAsJsonDocument(tableDefinition, fileName, exportPath);
    }

    private Table loadTableDefinition(ExportProcessContext context, String tableName) {
        String schema = context.getSchema();
        String dataSourceName = context.getDataSource();

        DirigibleDataSource dataSource = datasourceManager.getDataSource(dataSourceName);

        try {
            return dataSourceMetadataLoader.loadTableMetadata(schema, tableName, dataSource);
        } catch (SQLException ex) {
            throw new SchemaExportException(
                    "Failed to export table definition for " + tableName + " from schema " + schema + " using data source " + dataSource,
                    ex);
        }
    }

}
