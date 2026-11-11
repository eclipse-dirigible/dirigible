/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * The first step of {@code export-tenant}: the export is running, and the schema export is told
 * what to read - the tenant's own data source and schema, the application's tables in it, and a
 * work folder that belongs to this export alone.
 */
@Component("PrepareTask_ExportTenantProcess") // used in the bpmn process
@Conditional(TenantExportEnabledCondition.class)
class PrepareTenantExportTask extends BPMTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareTenantExportTask.class);

    // caution: these names are used in the BPMN process definition and by the export-schema process
    private static final String DATA_SOURCE = "dataSource";
    private static final String SCHEMA = "schema";
    private static final String EXPORT_PATH = "exportPath";
    private static final String INCLUDED_TABLES = "includedTables";
    private static final String EXCLUDED_TABLES = "excludedTables";
    private static final String EXPORT_DATABASE = "exportDatabase";

    private final TenantExportService service;
    private final TenantExportTables tables;
    private final TenantExportArchives archives;

    PrepareTenantExportTask(TenantExportService service, TenantExportTables tables, TenantExportArchives archives) {
        this.service = service;
        this.tables = tables;
        this.archives = archives;
    }

    @Override
    protected void execute(TaskExecution execution) {
        long exportId = execution.getMandatoryVariable(TenantExportProcess.EXPORT_ID, Long.class);
        service.markRunning(exportId);
        try {
            // a retried step starts from nothing: the schema export refuses a folder that exists
            archives.deleteExportFolder(exportId);
            TenantExportTables.Source source = tables.resolve();
            LOGGER.info("Export [{}] takes [{}] table(s) of schema [{}]: {}", exportId, source.tables()
                                                                                              .size(),
                    source.schema(), source.tables());
            execution.setVariable(DATA_SOURCE, source.dataSource());
            execution.setVariable(SCHEMA, source.schema());
            execution.setVariable(EXPORT_PATH, TenantExportArchives.workFolder(exportId));
            execution.setVariable(INCLUDED_TABLES, source.tables());
            execution.setVariable(EXCLUDED_TABLES, Set.of());
            // with no tables the schema export is skipped: it reads an empty list as "every table"
            execution.setVariable(EXPORT_DATABASE, !source.tables()
                                                          .isEmpty());
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Could not prepare export [" + exportId + "]", ex);
        }
    }
}
