/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.config;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class DataSourceConfig.
 */
@Configuration
public class DataSourceConfig {

    /**
     * Gets the default data source name.
     *
     * @return the default data source name
     */
    @Bean
    @DefaultDataSourceName
    String getDefaultDataSourceName() {
        return DirigibleConfig.DEFAULT_DATA_SOURCE_NAME.getStringValue();

    }

    /**
     * Gets the system data source name.
     *
     * @return the system data source name
     */
    @Bean
    @SystemDataSourceName
    String getSystemDataSourceName() {
        return DirigibleConfig.SYSTEM_DATA_SOURCE_NAME.getStringValue();
    }
}
