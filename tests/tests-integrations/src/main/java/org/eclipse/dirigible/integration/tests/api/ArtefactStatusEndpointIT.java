/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for {@code GET /services/core/artefacts}: publishes an artefact into the
 * registry, synchronizes, and asserts the endpoint reports it with the lifecycle the synchronizer
 * left behind.
 */
class ArtefactStatusEndpointIT extends IntegrationTest {

    private static final String ENDPOINT = "/services/core/artefacts";

    private static final String LOCATION = "/artefact-status-it/monitoring.extensionpoint";

    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION;

    private static final String CONTENT = """
            {
              "name": "artefact-status-it-point",
              "description": "Artefact status endpoint test"
            }
            """;

    /** The synchronization is forced synchronously, but the artefact row can lag it slightly. */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void reports_a_synchronized_artefact_with_its_lifecycle() {
        repository.createResource(REGISTRY_PATH, CONTENT.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(byLocation() + ".type", contains("extensionpoint"))
                                                 .body(byLocation() + ".name", contains("artefact-status-it-point"))
                                                 .body(byLocation() + ".status", contains("CREATED")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void drops_an_artefact_removed_from_the_registry() {
        repository.createResource(REGISTRY_PATH, CONTENT.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();

        repository.removeResource(REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(byLocation(), empty()),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void removeArtefactFromRegistry() {
        if (repository.hasResource(REGISTRY_PATH)) {
            repository.removeResource(REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    /** A JsonPath filter selecting the artefacts this test published, whatever else is deployed. */
    private static String byLocation() {
        return "findAll { it.location == '" + LOCATION + "' }";
    }
}
