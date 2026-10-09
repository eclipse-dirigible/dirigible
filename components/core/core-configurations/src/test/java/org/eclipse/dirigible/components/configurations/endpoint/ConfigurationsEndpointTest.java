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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.ConfigGroup;
import org.eclipse.dirigible.commons.config.ConfigGroups;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.SensitiveConfigs;
import org.eclipse.dirigible.components.configurations.service.ConfigurationsService;
import org.eclipse.dirigible.components.configurations.service.SensitiveConfigurations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class ConfigurationsEndpointTest.
 *
 * {@link ConfigurationsService} is dependency-free (it only reads the static
 * {@link org.eclipse.dirigible.commons.config.Configuration} sources), so it is exercised directly
 * here without booting a Spring context. The end-to-end wiring of the configuration endpoints
 * together with the tenant context and datasources is covered by the integration tests.
 */
public class ConfigurationsEndpointTest {

    private static final String SECRET_KEY = "DIRIGIBLE_MAIL_PASSWORD";

    private static final String SENSITIVE_KEY = "DIRIGIBLE_INTENT_AI_API_KEY";

    private static final String PLAIN_KEY = "DIRIGIBLE_BRANDING_NAME";

    /** The configurations service. */
    private final ConfigurationsService configurationsService = new ConfigurationsService();

    private final ConfigurationsEndpoint endpoint = new ConfigurationsEndpoint(configurationsService);

    @AfterEach
    void cleanUp() {
        Configuration.remove(SECRET_KEY);
        Configuration.remove(SENSITIVE_KEY);
        Configuration.remove(PLAIN_KEY);
    }

    /**
     * Find all.
     */
    @Test
    public void findAll() {
        assertNotNull(endpoint.findAll(null)
                              .getBody());
    }

    /**
     * A sensitive value set at runtime never appears in clear in the body; a plain one is unchanged.
     */
    @Test
    public void findAllMasksSensitiveValues() {
        Configuration.set(SENSITIVE_KEY, "sk-live-value");
        Configuration.set(PLAIN_KEY, "Acme");

        List<List<String>> body = endpoint.findAll(null)
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

    /** Without a group the legacy endpoint answers exactly what it did before groups existed. */
    @Test
    public void withoutAGroupTheLegacyShapeIsUnchanged() {
        List<List<String>> body = endpoint.findAll(null)
                                          .getBody();
        assertEquals(configurationsService.findAll(), body);
        assertEquals(Configuration.getConfigurationParameters().length, body.size());
        assertTrue(body.stream()
                       .allMatch(row -> row.size() == 5));
        assertEquals(body, endpoint.findAll(List.of())
                                   .getBody());
    }

    @Test
    public void filtersByOneGroup() {
        List<List<String>> body = endpoint.findAll(List.of("database"))
                                          .getBody();
        assertFalse(body.isEmpty());
        assertTrue(body.stream()
                       .allMatch(row -> ConfigGroups.resolve(row.get(0)) == ConfigGroup.DATABASE));
    }

    @Test
    public void filtersBySeveralGroups() {
        Set<ConfigGroup> expected = Set.of(ConfigGroup.MAIL, ConfigGroup.BRANDING);
        List<List<String>> repeated = endpoint.findAll(List.of("mail", "branding"))
                                              .getBody();
        List<List<String>> commaSeparated = endpoint.findAll(List.of("mail,branding"))
                                                    .getBody();
        assertEquals(repeated, commaSeparated);
        assertEquals(expected, repeated.stream()
                                       .map(row -> ConfigGroups.resolve(row.get(0)))
                                       .collect(Collectors.toSet()));
    }

    @Test
    public void anUnknownGroupIsABadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> endpoint.findAll(List.of("nope")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> endpoint.findDescriptors(List.of("nope"), null, false));
    }

    @Test
    public void groupsCountEveryKey() {
        List<ConfigDescriptors.GroupSummary> groups = endpoint.findGroups()
                                                              .getBody();
        assertEquals(Configuration.getConfigurationParameters().length, groups.stream()
                                                                              .mapToInt(ConfigDescriptors.GroupSummary::total)
                                                                              .sum());
    }

    @Test
    public void descriptorsNeverCarryASecretInClear() {
        Configuration.set(SECRET_KEY, "hunter2");
        List<ConfigDescriptors.Group> groups = endpoint.findDescriptors(List.of("mail"), null, true)
                                                       .getBody();
        assertFalse(groups.toString()
                          .contains("hunter2"));
        ConfigDescriptors.Entry entry = groups.get(0)
                                              .entries()
                                              .stream()
                                              .filter(candidate -> candidate.key()
                                                                            .equals(SECRET_KEY))
                                              .findFirst()
                                              .orElseThrow();
        assertEquals(SensitiveConfigs.MASK, entry.value());
        assertEquals(ConfigDescriptors.SOURCE_RUNTIME, entry.source());
    }
}
