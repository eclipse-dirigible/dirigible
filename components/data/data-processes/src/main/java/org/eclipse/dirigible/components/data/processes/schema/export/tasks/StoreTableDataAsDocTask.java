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

@Component("StoreTableDataAsDocTask_ExportSchemaProcess") // used in the bpmn process
class StoreTableDataAsDocTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreTableDataAsDocTask.class);

    private static final String CSV_MEDIA_TYPE = "text/csv";

    private final DatabaseExportService databaseExportService;

    StoreTableDataAsDocTask(DatabaseExportService databaseExportService) {
        this.databaseExportService = databaseExportService;
    }

    @Override
    protected void execute(ExportProcessContext context) {
        String tableDataFilePath = context.getTableDataFilePath();

        LOGGER.debug("Saving file {} as document...", tableDataFilePath);

        File file = new File(tableDataFilePath);
        saveFileAsDocument(context, file);

        boolean fileDeleted = file.delete();
        LOGGER.debug("File {} deleted successfully: {}", tableDataFilePath, fileDeleted);
    }

    private void saveFileAsDocument(ExportProcessContext context, File file) {
        String table = context.getCurrentTable();
        String exportFolder = context.getExportPath();
        String fileName = ExportFilesHelper.createTableDataFilename(table);

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            saveDocument(in, fileName, CSV_MEDIA_TYPE, exportFolder);

        } catch (IOException | RuntimeException ex) {
            throw new SchemaExportException(
                    "Failed to save file [" + file + "] to document [" + fileName + "] in export folder [" + exportFolder + "]", ex);
        }
    }

}
