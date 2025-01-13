/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.open.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The Class OpenTelemetryConfiguration.
 */
@Profile("!open-telemetry")
@Configuration
public class OpenTelemetryConfiguration {

    /**
     * Provide open telemetry.
     *
     * @return the open telemetry
     */
    @Bean
    OpenTelemetry provideOpenTelemetry() {
        return GlobalOpenTelemetry.get();
    }

}
