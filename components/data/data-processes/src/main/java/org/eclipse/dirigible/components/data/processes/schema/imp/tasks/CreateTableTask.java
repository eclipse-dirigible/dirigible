/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableCreateProcessor;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

@Component("CreateTableTask_ImportSchemaProcess") // used in the bpmn process
class CreateTableTask extends BaseImportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTableTask.class);

    private final DataSourcesManager datasourceManager;

    CreateTableTask(DataSourcesManager datasourceManager) {
        this.datasourceManager = datasourceManager;
    }

    @Override
    protected void execute(ImportProcessContext context) {
        Table table = loadTableDefinition(context);

        table.setIndexes(Collections.emptyList());

        String dataSourceName = context.getDataSource();
        createTable(dataSourceName, table);

        context.setTableSchema(table.getSchema());
    }

    private Table loadTableDefinition(ImportProcessContext context) {
        String tableName = context.getTable();
        String tableDefinitionFilePath = context.getExportPath() + "/" + ExportFilesHelper.createTableDefinitionFilename(tableName);
        String tableDefinition = loadDocumentContent(tableDefinitionFilePath);

        return JsonHelper.fromJson(tableDefinition, Table.class);
    }

    private void createTable(String dataSourceName, Table table) {
        DirigibleDataSource dataSource = datasourceManager.getDataSource(dataSourceName);
        try (Connection connection = dataSource.getConnection()) {
            TableCreateProcessor.execute(connection, table, false);
            LOGGER.info("Created table {} ", table.getName());
        } catch (SQLException | RuntimeException ex) {
            throw new SchemaImportException("Failed to create table [" + table + "] in data source " + dataSource, ex);
        }
    }

}
