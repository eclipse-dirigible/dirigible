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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("BuildExportTopologyTask") // used in the bpmn process
public class BuildExportTopologyTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildExportTopologyTask.class);

    @Override
    protected void execute(ExportProcessContext context) {
        LOGGER.info("Executing {} with context {}", this, context);

        Set<String> includedTables = context.getIncludedTables();
        LOGGER.info("includedTables {}", includedTables);

        Set<String> excludedTables = context.getExcludedTables();
        LOGGER.info("excludedTables {}", excludedTables);

        String schema = context.getSchema();
        LOGGER.info("schema {}", schema);

        String dataSource = context.getDataSource();
        LOGGER.info("dataSource {}", dataSource);

        String exportPath = context.getExportPath();
        LOGGER.info("exportPath {}", exportPath);
    }

}
