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

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * The step of {@code export-tenant} that packs the archive from the tables the schema export wrote
 * and the tenant's documents, and stores it.
 */
@Component("PackageArchiveTask_ExportTenantProcess") // used in the bpmn process
@Conditional(TenantExportEnabledCondition.class)
class PackageTenantExportTask extends BPMTask {

    /** The variable carrying where the archive is. */
    static final String ARCHIVE_PATH = "archivePath";

    /** The variable carrying the archive's size. */
    static final String ARCHIVE_SIZE = "archiveSize";

    private final TenantExportRepository exports;
    private final TenantExportArchives archives;

    PackageTenantExportTask(TenantExportRepository exports, TenantExportArchives archives) {
        this.exports = exports;
        this.archives = archives;
    }

    @Override
    protected void execute(TaskExecution execution) {
        long exportId = execution.getMandatoryVariable(TenantExportProcess.EXPORT_ID, Long.class);
        TenantExport export = exports.findById(exportId)
                                     .orElseThrow(() -> new IllegalStateException("There is no export [" + exportId + "]"));
        try {
            TenantExportArchives.Stored stored = archives.pack(exportId, export.getTenantId(), export.getRequestedAt());
            execution.setVariable(ARCHIVE_PATH, stored.path());
            execution.setVariable(ARCHIVE_SIZE, stored.sizeBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not pack the archive of export [" + exportId + "]", ex);
        }
    }
}
