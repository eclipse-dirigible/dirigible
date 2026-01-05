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

import org.eclipse.dirigible.components.data.export.service.DatabaseExportService;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;

@Component("ExportTableDataTask_ExportSchemaProcess") // used in the bpmn process
class ExportTableDataTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTableDataTask.class);

    private static final String CSV_MEDIA_TYPE = "text/csv";

    private final DatabaseExportService databaseExportService;

    ExportTableDataTask(DatabaseExportService databaseExportService) {
        this.databaseExportService = databaseExportService;
    }

    @Override
    protected void execute(ExportProcessContext context) {
        String table = context.getCurrentTable();

        // use temp file to prevent OOM
        File tempFile = createTempFile(table);
        LOGGER.debug("Created temp file [{}] for table [{}]", tempFile, table);
        try {
            exportTableDataToFile(tempFile, context);
            saveFileAsDocument(context, tempFile);
        } finally {
            tempFile.delete();
        }
    }

    private File createTempFile(String table) {
        try {
            File tempFile = File.createTempFile(table, ".csv");
            tempFile.deleteOnExit();

            return tempFile;
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to create temp file for table " + table, ex);
        }
    }

    private void exportTableDataToFile(File tempFile, ExportProcessContext context) {
        String table = context.getCurrentTable();

        String schema = context.getSchema();
        String dataSourceName = context.getDataSource();

        try {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                databaseExportService.exportStructure(dataSourceName, schema, table, out);
            }
        } catch (IOException | RuntimeException ex) {
            throw new SchemaExportException("Failed to export table [" + table + "] from schema [" + schema + "] from data source ["
                    + dataSourceName + "] into temp file", ex);
        }
    }

    private void saveFileAsDocument(ExportProcessContext context, File file) {
        String table = context.getCurrentTable();
        String exportFolder = context.getExportPath();
        String fileName = ExportFilesHelper.createTableDataFilename(table);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            long contentLength = file.length();
            saveDocument(in, contentLength, fileName, CSV_MEDIA_TYPE, exportFolder);

        } catch (IOException | RuntimeException ex) {
            throw new SchemaExportException(
                    "Failed to save file [" + file + "] to document [" + fileName + "] in to export folder [" + exportFolder + "]", ex);
        }
    }

}
