/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.init;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * The Class JobsInitializer.
 */
@Order(ApplicationReadyEventListeners.PROCESSES_INITIALIZER)
@Component
class FlowableProcessesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableProcessesInitializer.class);

    private final Set<SystemBpmProcess> systemBpmProcesses;
    private final BpmService bpmService;
    private final TenantContext tenantContext;

    FlowableProcessesInitializer(Set<SystemBpmProcess> systemBpmProcesses, BpmService bpmService, TenantContext tenantContext) {
        this.systemBpmProcesses = systemBpmProcesses;
        this.bpmService = bpmService;
        this.tenantContext = tenantContext;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Deploying [{}] system processes: {}", systemBpmProcesses.size(), systemBpmProcesses);

        systemBpmProcesses.forEach(this::deployProcess);

        LOGGER.info("Completed.");
    }

    private void deployProcess(SystemBpmProcess systemBpmProcess) {
        LOGGER.info("Deploying system BPM process {}", systemBpmProcess);
        String deploymentKey = systemBpmProcess.getDeploymentKey();
        String resourceName = systemBpmProcess.getResourcePath();
        byte[] content = getResourceFileContent(systemBpmProcess.getResourcePath());

        // deploy for each tenant?
        tenantContext.executeForEachTenant(() -> {
            bpmService.deployProcess(deploymentKey, resourceName, content);
            return null;
        });
    }

    private byte[] getResourceFileContent(String resourcePath) {
        try (InputStream inputStream = FlowableProcessesInitializer.class.getResourceAsStream(resourcePath)) {
            if (null == inputStream) {
                throw new IllegalStateException("Missing process with resource path " + resourcePath);
            }

            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to process with path " + resourcePath, ex);
        }
    }

}
