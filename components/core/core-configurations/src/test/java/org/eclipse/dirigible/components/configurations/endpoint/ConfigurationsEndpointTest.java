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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.configurations.service.ConfigurationsService;
import org.eclipse.dirigible.components.configurations.service.SensitiveConfigurations;
import org.junit.jupiter.api.AfterEach;
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

    private static final String SENSITIVE_KEY = "DIRIGIBLE_INTENT_AI_API_KEY";

    private static final String PLAIN_KEY = "DIRIGIBLE_BRANDING_NAME";

    /** The configurations endpoint. */
    private final ConfigurationsEndpoint configurationsEndpoint = new ConfigurationsEndpoint(new ConfigurationsService());

    @AfterEach
    public void tearDown() {
        Configuration.remove(SENSITIVE_KEY);
        Configuration.remove(PLAIN_KEY);
    }

    /**
     * Find all.
     */
    @Test
    public void findAll() {
        assertNotNull(configurationsEndpoint.findAll()
                                            .getBody());
    }

    /**
     * A sensitive value set at runtime never appears in clear in the body; a plain one is unchanged.
     */
    @Test
    public void findAllMasksSensitiveValues() {
        Configuration.set(SENSITIVE_KEY, "sk-live-value");
        Configuration.set(PLAIN_KEY, "Acme");

        List<List<String>> body = configurationsEndpoint.findAll()
                                                        .getBody();

        assertNotNull(body);
        assertFalse(body.toString()
                        .contains("sk-live-value"));
        assertEquals(SensitiveConfigurations.MASK, row(body, SENSITIVE_KEY).get(1));
        assertEquals("Acme", row(body, PLAIN_KEY).get(1));
    }

    private static List<String> row(List<List<String>> body, String key) {
        return body.stream()
                   .filter(r -> key.equals(r.get(0)))
                   .findFirst()
                   .orElseThrow(() -> new AssertionError("No row for " + key));
    }
}
