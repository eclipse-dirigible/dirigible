/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class DatabaseMetadataCacheConfig.
 */
@Configuration
public class DatabaseMetadataCacheConfig {

    /**
     * Gets the database metadata cache.
     *
     * @return the database metadata cache
     */
    @Bean
    public DatabaseMetadataCache getDatabaseMetadataCache() {
        return new DatabaseMetadataCache();
    }

}
