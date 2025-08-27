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

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.management.load.DataSourceMetadataLoader;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.engine.cms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

@Component("ExportTableDefinitionTask") // used in the bpmn process
class ExportTableDefinitionTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTableDefinitionTask.class);

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
        LOGGER.info("Exporting table definition [{}]", table);

        Table tableDefinition = loadTableDefinition(context, table);
        String fileName = table + ".json";
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

    private void saveObjectAsJsonDocument(Object object, String fileName, String exportPath) {
        String exportTopologyJson = JsonHelper.toJson(object);
        saveDocument(exportTopologyJson, fileName, MediaType.APPLICATION_JSON_VALUE, exportPath);
    }

    private void saveDocument(String content, String fileName, String mediaType, String exportPath) {
        CmisFolder exportFolder = getFolder(exportPath);
        saveDocument(content, fileName, mediaType, exportFolder);
    }

    private void saveDocument(String content, String fileName, String mediaType, CmisFolder folder) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        Map<String, String> fileProps = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, //
                CmisConstants.NAME, fileName);

        // this implementation is fine for small files
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inStream = new ByteArrayInputStream(bytes)) {

            long length = bytes.length; // not needed?
            CmisContentStream contentStream = cmisSession.getObjectFactory()
                                                         .createContentStream(fileName, length, mediaType, inStream);

            folder.createDocument(fileProps, contentStream);
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to create document with name [" + fileName + "] in folder [" + folder + "]", ex);
        }
    }

    private CmisFolder getFolder(String folderPath) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        try {
            CmisObject exportFolderAsObject = cmisSession.getObjectByPath(folderPath);
            if (exportFolderAsObject instanceof CmisFolder exportFolder) {
                return exportFolder;
            }
            throw new SchemaExportException("Returned cmis object " + exportFolderAsObject + " is not a folder");
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to get folder from path " + folderPath, ex);
        }
    }

}
