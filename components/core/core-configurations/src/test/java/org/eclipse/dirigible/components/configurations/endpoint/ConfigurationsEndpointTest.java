/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.endpoint;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.dirigible.components.configurations.service.ConfigurationsService;
import org.junit.jupiter.api.Test;

/**
 * The Class ConfigurationsEndpointTest.
 *
 * {@link ConfigurationsService} is dependency-free (it only reads the static
 * {@link org.eclipse.dirigible.commons.config.Configuration} sources), so it is exercised directly
 * here without booting a Spring context. The end-to-end wiring of the configuration endpoints
 * together with the tenant context and datasources is covered by the integration tests.
 */
public class ConfigurationsEndpointTest {

    /** The configurations service. */
    private final ConfigurationsService configurationsService = new ConfigurationsService();

    /**
     * Find all.
     */
    @Test
    public void findAll() {
        assertNotNull(configurationsService.findAll());
    }
}
