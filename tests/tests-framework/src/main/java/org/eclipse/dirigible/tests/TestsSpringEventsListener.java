/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// @Order(Ordered.HIGHEST_PRECEDENCE) // Ensures it runs first
@Order(Ordered.LOWEST_PRECEDENCE) // Ensures it runs last
@Component
class TestsSpringEventsListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestsSpringEventsListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent || event instanceof ContextStoppedEvent) {
            LOGGER.info("Event [{}] will be handled.", event);
            DirigibleCleaner.deleteDirigibleFolder();
            return;
        }

        LOGGER.info("Event [{}] WILL NOT be handled.", event);
    }

}

