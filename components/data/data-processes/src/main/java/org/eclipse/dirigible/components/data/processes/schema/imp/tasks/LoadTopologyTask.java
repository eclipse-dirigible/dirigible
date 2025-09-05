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

import com.google.gson.reflect.TypeToken;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.processes.schema.export.ExportFilesHelper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("LoadTopologyTask_ImportSchemaProcess") // used in the bpmn process
class LoadTopologyTask extends BaseImportTask {

    @Override
    protected void execute(ImportProcessContext context) {
        String exportPath = context.getExportPath();
        List<String> tables = loadImportTables(exportPath);

        context.setTables(tables);
    }

    private List<String> loadImportTables(String exportPath) {
        String topologyFilePath = ExportFilesHelper.createExportTopologyFilePath(exportPath);
        String fileContent = loadDocumentContent(topologyFilePath);

        TypeToken<List<String>> typeToken = new TypeToken<>() {};
        return JsonHelper.fromJson(fileContent, typeToken);
    }

}
