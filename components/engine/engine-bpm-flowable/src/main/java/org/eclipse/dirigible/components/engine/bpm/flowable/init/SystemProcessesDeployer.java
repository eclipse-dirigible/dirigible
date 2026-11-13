/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.bpm.SystemBpmProcess;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deploys the platform's own processes - every {@link SystemBpmProcess} bean - for the current
 * tenant. A process definition belongs to one tenant, so the caller decides which tenants: all of
 * them when the instance starts, and the new one when a tenant is provisioned while it runs.
 */
@Component
class SystemProcessesDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemProcessesDeployer.class);

    private final Set<SystemBpmProcess> systemBpmProcesses;
    private final BpmService bpmService;

    SystemProcessesDeployer(Set<SystemBpmProcess> systemBpmProcesses, BpmService bpmService) {
        this.systemBpmProcesses = systemBpmProcesses;
        this.bpmService = bpmService;
    }

    /**
     * Deploys every system process for the current tenant.
     */
    void deployForCurrentTenant() {
        LOGGER.info("Deploying [{}] system processes: {}", systemBpmProcesses.size(), systemBpmProcesses);
        for (SystemBpmProcess process : systemBpmProcesses) {
            String content = new String(getResourceFileContent(process.getResourcePath()), StandardCharsets.UTF_8);
            bpmService.deployProcess(process.getDeploymentKey(), process.getResourcePath(), content);
        }
    }

    private byte[] getResourceFileContent(String resourcePath) {
        try (InputStream inputStream = SystemProcessesDeployer.class.getResourceAsStream(resourcePath)) {
            if (null == inputStream) {
                throw new IllegalStateException("Missing process with resource path " + resourcePath);
            }

            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to process with path " + resourcePath, ex);
        }
    }
}
