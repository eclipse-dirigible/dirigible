/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(ApplicationListenersOrder.APP_LYFECYCLE_LOGGING_LISTENER)
@Component
class AppLifecycleLoggingListener implements ApplicationListener<ApplicationEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppLifecycleLoggingListener.class);

    private long createdContextAt;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationStartedEvent) {
            createdContextAt = System.currentTimeMillis();
            log("Application is initializing");
        }

        if (event instanceof ApplicationReadyEvent) {
            log("Application has started");
            long currentTime = System.currentTimeMillis();
            log("Start time: [{}] milliseconds. Init time: [{}] milliseconds", (currentTime - DirigibleApplication.getStartedAt()),
                    (currentTime - createdContextAt));
        }
    }

    private void log(String messageFormat, Object... argument) {
        LOGGER.info("------- " + messageFormat + " -------", argument);
    }

    @Override
    public void destroy() {
        log("Application stopped");
        log("Application started at [{}] and has been running for [{}] milliseconds", DirigibleApplication.getStartedAt(),
                (System.currentTimeMillis() - DirigibleApplication.getStartedAt()));
    }
}
