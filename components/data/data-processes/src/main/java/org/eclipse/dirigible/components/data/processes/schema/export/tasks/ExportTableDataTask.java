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

@Component("ExportTableDataTask") // used in the bpmn process
class ExportTableDataTask extends BaseExportTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTableDataTask.class);

    @Override
    protected void execute(ExportProcessContext context) {
        String table = context.getCurrentTable();
        LOGGER.info("Exporting table data [{}]", table);
    }

}
