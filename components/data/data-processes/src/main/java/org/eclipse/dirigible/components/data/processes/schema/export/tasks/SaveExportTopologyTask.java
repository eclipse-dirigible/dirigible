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
import org.eclipse.dirigible.components.engine.cms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component("SaveExportTopologyTask") // used in the bpmn process
class SaveExportTopologyTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveExportTopologyTask.class);

    private static final String EXPORT_TOPOLOGY_FILENAME = "export-topology.json";

    @Override
    protected void execute(ExportProcessContext context) {
        List<String> exportTopology = context.getExportTopology();
        LOGGER.debug("Saving export topology {}", exportTopology);

        CmisSession cmisSession = CmisSessionFactory.getSession();
        String exportPath = context.getExportPath();
        try {
            CmisFolder exportFolder = createExportFolder(cmisSession, exportPath);
            createTopolgyFile(exportTopology, cmisSession, exportFolder);
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to save export topology file", ex);
        }
    }

    private CmisFolder createExportFolder(CmisSession cmisSession, String exportPath) throws IOException {
        CmisFolder rootFolder = cmisSession.getRootFolder();
        Map<String, String> rootFolderProps = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_FOLDER, //
                CmisConstants.NAME, exportPath);

        return rootFolder.createFolder(rootFolderProps);
    }

    private void createTopolgyFile(List<String> exportTopology, CmisSession cmisSession, CmisFolder exportFolder) throws IOException {
        Map<String, String> topolgyFileProps = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, //
                CmisConstants.NAME, EXPORT_TOPOLOGY_FILENAME);

        // this implementation is fine for small files
        String exportTopologyJson = JsonHelper.toJson(exportTopology);
        byte[] bytes = exportTopologyJson.getBytes(StandardCharsets.UTF_8);
        try (InputStream inStream = new ByteArrayInputStream(bytes)) {

            long length = bytes.length; // not needed?
            CmisContentStream contentStream = cmisSession.getObjectFactory()
                                                         .createContentStream(EXPORT_TOPOLOGY_FILENAME, length,
                                                                 MediaType.APPLICATION_JSON_VALUE, inStream);

            exportFolder.createDocument(topolgyFileProps, contentStream);
        }
    }
}
