/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.open.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class OpenTelemetryProvider implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryProvider.class);

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext argApplicationContext) throws BeansException {
        context = argApplicationContext;
    }

    public static OpenTelemetry get() {
        assertSpringInitialized();
        return context.getBean(OpenTelemetry.class);
    }

    private static void assertSpringInitialized() {
        if (!isInitialzed()) {
            throw new IllegalStateException("Spring is not initialized yet.");
        }
    }

    private static boolean isInitialzed() {
        return context != null;
    }
}
