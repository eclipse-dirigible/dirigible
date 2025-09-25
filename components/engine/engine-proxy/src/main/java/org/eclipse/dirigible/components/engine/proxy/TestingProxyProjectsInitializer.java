/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(65)
@Component
class TestingProxyProjectsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestingProxyProjectsInitializer.class);

    private final ProxyProjectsRegistry projectsRegistry;

    TestingProxyProjectsInitializer(ProxyProjectsRegistry projectsRegistry) {
        this.projectsRegistry = projectsRegistry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // TODO this class is used for testing only
        LOGGER.info("Registering testing proxy projects...");

        ProxyProject project1 = new ProxyProject("project1", "http://localhost:3000");
        projectsRegistry.register(project1);

        ProxyProject project2 = new ProxyProject("project2", "http://localhost:3001");
        projectsRegistry.register(project2);

        ProxyProject project3 = new ProxyProject("project3", "https://httpbin.org");
        projectsRegistry.register(project3);
    }

}
