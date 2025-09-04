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

import org.eclipse.dirigible.components.data.export.service.DataImportService;
import org.eclipse.dirigible.components.data.export.service.ImportConfig;
import org.eclipse.dirigible.components.data.export.service.ImportConfigBuilder;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;

@Component("ImportTableDataTask_ImportSchemaProcess") // used in the bpmn process
class ImportTableDataTask extends BaseImportTask {

    private final DataImportService dataImportService;

    ImportTableDataTask(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @Override
    protected void execute(ImportProcessContext context) {
        String table = context.getTable();
        String dataSource = context.getDataSource();
        String schema = context.getTableSchema();

        String tableDataFilePath = context.getExportPath() + "/" + ExportFilesHelper.createTableDataFilename(table);
        try (InputStream inputStream = new BufferedInputStream(loadDocumentContentAsStream(tableDataFilePath))) {
            ImportConfig importConfig = new ImportConfigBuilder().setDistinguishEmptyFromNull(true)
                                                                 .build();
            dataImportService.importData(dataSource, schema, table, importConfig, inputStream);
        } catch (Exception ex) {
            throw new SchemaImportException(
                    "Failed to import data into table [" + table + "] from document with path [" + tableDataFilePath + "]", ex);
        }
    }

}
