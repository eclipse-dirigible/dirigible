/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nodejs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(65)
@Component
class TestingNodejsProjectsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestingNodejsProjectsInitializer.class);

    private final NodejsProjectsRegistry projectsRegistry;

    TestingNodejsProjectsInitializer(NodejsProjectsRegistry projectsRegistry) {
        this.projectsRegistry = projectsRegistry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // TODO this class is used for testing only
        LOGGER.info("Registering testing nodejs projects...");

        NodejsProject project1 = new NodejsProject("project1", "http://localhost:3000");
        projectsRegistry.register(project1);

        NodejsProject project2 = new NodejsProject("project2", "http://localhost:3001");
        projectsRegistry.register(project2);

        NodejsProject project3 = new NodejsProject("project3", "https://httpbin.org");
        projectsRegistry.register(project3);
    }

}
