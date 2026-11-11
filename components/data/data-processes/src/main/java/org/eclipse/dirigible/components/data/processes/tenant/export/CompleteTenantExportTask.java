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

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * The last step of {@code export-tenant}: the export is completed, with its archive's place, size
 * and expiry.
 */
@Component("CompleteTask_ExportTenantProcess") // used in the bpmn process
@Conditional(TenantExportEnabledCondition.class)
class CompleteTenantExportTask extends BPMTask {

    private final TenantExportService service;

    CompleteTenantExportTask(TenantExportService service) {
        this.service = service;
    }

    @Override
    protected void execute(TaskExecution execution) {
        long exportId = execution.getMandatoryVariable(TenantExportProcess.EXPORT_ID, Long.class);
        String archivePath = execution.getMandatoryVariable(PackageTenantExportTask.ARCHIVE_PATH, String.class);
        long archiveSize = execution.getMandatoryVariable(PackageTenantExportTask.ARCHIVE_SIZE, Long.class);
        service.complete(exportId, archivePath, archiveSize);
    }
}
