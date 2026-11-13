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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.JsonProcessVariablesBuilder;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * The {@code export-tenant} system process: prepare, the {@code export-schema} process as a call
 * activity for the tables, pack the archive, complete. This class is what the rest of the export
 * knows of the process engine - how an export is started, whether it failed and how it is stopped.
 *
 * <p>
 * A step that throws is retried by the engine and then parked as a dead-letter job; nothing in the
 * process itself hears of that. So a failure is found from outside, by {@link #failure(String)}:
 * the process, or the schema export it called, has a dead-letter job.
 */
@Component
@Conditional(TenantExportEnabledCondition.class)
class TenantExportProcess implements SystemBpmProcess {

    /** The process definition key. */
    static final String KEY = "export-tenant";

    /** The variable carrying the export's id. */
    static final String EXPORT_ID = "tenantExportId";

    private final BpmService bpmService;
    private final ProcessEngine processEngine;

    TenantExportProcess(BpmService bpmService, ProcessEngine processEngine) {
        this.bpmService = bpmService;
        this.processEngine = processEngine;
    }

    @Override
    public String getIdentifier() {
        return KEY;
    }

    @Override
    public String getDeploymentKey() {
        return KEY;
    }

    @Override
    public String getResourcePath() {
        return "/system-processes/export-tenant.bpmn";
    }

    /**
     * Starts the process for an export, in the current tenant.
     *
     * @param exportId the export
     * @return the process instance id
     */
    String start(long exportId) {
        return bpmService.startProcess(KEY, String.valueOf(exportId), new JsonProcessVariablesBuilder().addVariable(EXPORT_ID, exportId)
                                                                                                       .build());
    }

    /**
     * Why an export's process will not complete, if it will not: one of its steps, or of the schema
     * export it called, ran out of retries, or the process is gone.
     *
     * @param processInstanceId the export's process instance
     * @return the reason, or empty while the process is running
     */
    Optional<String> failure(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        List<String> instanceIds = new ArrayList<>();
        instanceIds.add(processInstanceId);
        for (ProcessInstance called : runtimeService.createProcessInstanceQuery()
                                                    .superProcessInstanceId(processInstanceId)
                                                    .list()) {
            instanceIds.add(called.getId());
        }
        for (String instanceId : instanceIds) {
            List<Job> dead = processEngine.getManagementService()
                                          .createDeadLetterJobQuery()
                                          .processInstanceId(instanceId)
                                          .list();
            if (!dead.isEmpty()) {
                String message = dead.get(0)
                                     .getExceptionMessage();
                return Optional.of(message == null || message.isBlank() ? "A step of the export failed" : message);
            }
        }
        boolean running = runtimeService.createProcessInstanceQuery()
                                        .processInstanceId(processInstanceId)
                                        .count() > 0;
        return running ? Optional.empty() : Optional.of("The export's process ended before it completed the export");
    }

    /**
     * Stops an export's process, with the schema export it called. A process that is already gone is
     * success.
     *
     * @param processInstanceId the export's process instance
     */
    void cancel(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        boolean running = runtimeService.createProcessInstanceQuery()
                                        .processInstanceId(processInstanceId)
                                        .count() > 0;
        if (running) {
            runtimeService.deleteProcessInstance(processInstanceId, "The tenant export failed");
        }
    }
}
