/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deprovisioning: cancels the tenant's running process instances, then deletes the tenant's process
 * history - finished instances with their variables, activities and tasks. Process variables carry
 * business data, and a tenant registered again under the same id must not see them. The engine's
 * tenant id is the platform's ({@link BpmProviderFlowable} deploys under it). The process
 * definitions stay deployed: they are the application's, not the tenant's. The instances of a
 * suspended tenant are suspended ({@link BpmTenantSuspension}); Flowable deletes a suspended
 * instance, with its suspended jobs, like any other.
 */
@Component
class BpmTenantTeardown implements TenantTeardownParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BpmTenantTeardown.class);

    /** The order of this participant: what runs for the tenant goes first. */
    static final int ORDER = 100;

    /** The reason recorded on a cancelled instance. */
    static final String REASON = "tenant deprovisioned";

    /** The process engine. */
    private final ProcessEngine processEngine;

    /**
     * Instantiates a new BPM teardown.
     *
     * @param processEngine the process engine
     */
    BpmTenantTeardown(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void teardown(String tenantId) {
        RuntimeService runtime = processEngine.getRuntimeService();
        List<ProcessInstance> running = runtime.createProcessInstanceQuery()
                                               .processInstanceTenantId(tenantId)
                                               .list();
        for (ProcessInstance instance : running) {
            runtime.deleteProcessInstance(instance.getId(), REASON);
        }
        HistoryService history = processEngine.getHistoryService();
        List<HistoricProcessInstance> finished = history.createHistoricProcessInstanceQuery()
                                                        .processInstanceTenantId(tenantId)
                                                        .list();
        for (HistoricProcessInstance instance : finished) {
            history.deleteHistoricProcessInstance(instance.getId());
        }
        LOGGER.info("Cancelled [{}] running process instance(s) and deleted [{}] historic one(s) of tenant [{}].", running.size(),
                finished.size(), LogSanitizer.sanitize(tenantId));
    }
}
