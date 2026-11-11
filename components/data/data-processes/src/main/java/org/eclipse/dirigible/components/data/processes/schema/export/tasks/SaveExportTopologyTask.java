/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.engine.cms.CmisObject;
import org.eclipse.dirigible.components.engine.cms.CmisSession;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
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

    /**
     * Creates the export folder. The path may name a folder several levels deep: the levels above the
     * last one are created when missing and reused when present, the last one must be new - an export
     * never writes into a folder that already holds something.
     */
    private CmisFolder createExportFolder(CmisSession cmisSession, String exportPath) throws IOException {
        List<String> segments = Arrays.stream(exportPath.split("/"))
                                      .filter(segment -> !segment.isBlank())
                                      .toList();
        if (segments.isEmpty()) {
            throw new SchemaExportException("The export path [" + exportPath + "] names no folder");
        }
        CmisFolder folder = cmisSession.getRootFolder();
        for (String segment : segments.subList(0, segments.size() - 1)) {
            CmisFolder existing = childFolder(folder, segment);
            folder = existing != null ? existing : createFolder(folder, segment);
        }
        return createFolder(folder, segments.get(segments.size() - 1));
    }

    private static CmisFolder childFolder(CmisFolder parent, String name) throws IOException {
        for (CmisObject child : parent.getChildren()) {
            if (child instanceof CmisFolder folder && name.equals(child.getName())) {
                return folder;
            }
        }
        return null;
    }

    private static CmisFolder createFolder(CmisFolder parent, String name) throws IOException {
        Map<String, String> properties = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_FOLDER, //
                CmisConstants.NAME, name);
        return parent.createFolder(properties);
    }
}
