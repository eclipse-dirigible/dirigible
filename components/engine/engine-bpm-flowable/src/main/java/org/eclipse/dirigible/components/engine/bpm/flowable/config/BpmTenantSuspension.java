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
import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Suspension: suspends every active process instance of the tenant, and activates them again.
 * Flowable moves a suspended instance's timer and async jobs to its suspended-job tables, so
 * nothing of it runs - no timer fires, no async step executes - and its user tasks cannot be
 * completed. No other platform path suspends a process instance (a retired deployment suspends its
 * definitions, never their instances), so every suspended instance of a suspended tenant is one
 * this participant suspended, and resuming activates them all. The engine's tenant id is the
 * platform's ({@link BpmProviderFlowable} deploys under it).
 */
@Component
class BpmTenantSuspension implements TenantSuspensionParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BpmTenantSuspension.class);

    /** The order of this participant: what runs for the tenant. */
    static final int ORDER = 100;

    /** The process engine. */
    private final ProcessEngine processEngine;

    /**
     * Instantiates a new BPM suspension.
     *
     * @param processEngine the process engine
     */
    BpmTenantSuspension(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void suspend(String tenantId) {
        RuntimeService runtime = processEngine.getRuntimeService();
        List<ProcessInstance> active = runtime.createProcessInstanceQuery()
                                              .processInstanceTenantId(tenantId)
                                              .active()
                                              .list();
        for (ProcessInstance instance : active) {
            runtime.suspendProcessInstanceById(instance.getId());
        }
        LOGGER.info("Suspended [{}] process instance(s) of tenant [{}].", active.size(), LogSanitizer.sanitize(tenantId));
    }

    @Override
    public void resume(String tenantId) {
        RuntimeService runtime = processEngine.getRuntimeService();
        List<ProcessInstance> suspended = runtime.createProcessInstanceQuery()
                                                 .processInstanceTenantId(tenantId)
                                                 .suspended()
                                                 .list();
        for (ProcessInstance instance : suspended) {
            runtime.activateProcessInstanceById(instance.getId());
        }
        LOGGER.info("Activated [{}] process instance(s) of tenant [{}].", suspended.size(), LogSanitizer.sanitize(tenantId));
    }
}
