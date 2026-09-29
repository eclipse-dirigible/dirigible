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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;

import io.restassured.path.json.JsonPath;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The boot numbers a release gate reads from {@code /actuator/health} instead of grepping the log
 * (#7533): the artefact census, the AOT registered-vs-expected class count and the job store's
 * liveness, shown to an authenticated caller and kept from an anonymous one.
 */
// One boot for the whole class: the one method that publishes removes its artefact again.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BootHealthIT extends IntegrationTest {

    private static final String HEALTH = "/actuator/health";

    private static final String VIEW_LOCATION = "/boot-health-it/broken.view";

    private static final String VIEW_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + VIEW_LOCATION;

    /** A view over a table that does not exist: its CREATE VIEW fails and it parks FAILED. */
    private static final String BROKEN_VIEW = """
            {
                "name": "BOOT_HEALTH_IT_BROKEN_VIEW",
                "type": "VIEW",
                "query": "SELECT * FROM BOOT_HEALTH_IT_NO_SUCH_TABLE",
                "dependencies": []
            }
            """;

    /** Keeps the pass carrying the broken view short: one in-pass retry a second later. */
    private static final String CROSS_RETRY_COUNT = "1";
    private static final String CROSS_RETRY_INTERVAL_MILLIS = "1000";

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    private static String previousCrossRetryCount;
    private static String previousCrossRetryInterval;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeAll
    static void shortenTheInPassRetry() {
        previousCrossRetryCount = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.getStringValue();
        previousCrossRetryInterval = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.getStringValue();
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.setStringValue(CROSS_RETRY_COUNT);
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.setStringValue(CROSS_RETRY_INTERVAL_MILLIS);
    }

    @AfterAll
    static void restoreTheInPassRetry() {
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.setStringValue(previousCrossRetryCount);
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.setStringValue(previousCrossRetryInterval);
    }

    @AfterEach
    void removeTheBrokenView() {
        if (repository.hasResource(VIEW_REGISTRY_PATH)) {
            repository.removeResource(VIEW_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void anAuthenticatedCallerReadsTheBootNumbers() {
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.status", equalTo("UP"))
                                                 .body("components.artefacts.details.total", greaterThan(0))
                                                 .body("components.artefacts.details.failed", notNullValue())
                                                 .body("components.artefacts.details.failedByType", notNullValue())
                                                 .body("components.aot.status", equalTo("UP"))
                                                 .body("components.aot.details.modules", notNullValue())
                                                 .body("components.scheduler.status", equalTo("UP"))
                                                 .body("components.scheduler.details.triggers", greaterThan(0))
                                                 .body("components.scheduler.details.errors", equalTo(0)),
                ASSERTION_TIMEOUT_SECONDS);

        restAssuredExecutor.execute(() -> {
            JsonPath health = given().when()
                                     .get(HEALTH)
                                     .then()
                                     .statusCode(200)
                                     .extract()
                                     .jsonPath();
            assertEquals(health.getInt("components.aot.details.expectedClasses"), health.getInt("components.aot.details.registeredClasses"),
                    "every class the AOT markers list must be registered");
        });
    }

    @Test
    void aFailedArtefactIsCountedByType() {
        repository.createResource(VIEW_REGISTRY_PATH, BROKEN_VIEW.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.status", equalTo("UP"))
                                                 .body("components.artefacts.details.failed", greaterThan(0))
                                                 .body("components.artefacts.details.failedByType.view", equalTo(1)),
                ASSERTION_TIMEOUT_SECONDS);

        repository.removeResource(VIEW_REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.details.failedByType", not(hasKey("view"))),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void anAnonymousProbeReadsTheStatusAlone() {
        restAssuredExecutor.execute(() -> given().auth()
                                                 .none()
                                                 .when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("UP"))
                                                 .body("$", not(hasKey("components"))),
                "localhost");
    }
}
