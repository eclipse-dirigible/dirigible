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

import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.eclipse.dirigible.components.engine.cms.CmisConstants;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component("SaveExportTopologyTask_ExportSchemaProcess") // used in the bpmn process
class SaveExportTopologyTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveExportTopologyTask.class);

    @Override
    protected void execute(ExportProcessContext context) {
        List<String> exportTopology = context.getExportTopology();
        LOGGER.debug("Saving export topology {}", exportTopology);

        CmisSession cmisSession = CmisSessionFactory.getSession();
        String exportPath = context.getExportPath();
        try {
            CmisFolder exportFolder = createExportFolder(cmisSession, exportPath);
            String topologyFilename = ExportFilesHelper.getExportTopologyFilename();
            saveObjectAsJsonDocument(exportTopology, topologyFilename, exportFolder);
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
}
