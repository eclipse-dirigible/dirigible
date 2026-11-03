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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

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
 * A client Java source that fails to compile counts as a failed artefact (#8239): the batch compile
 * settles its lifecycle in the synchronizer's {@code finishing()}, after the pass used to take its
 * census and outside the errors the pass counts, so the instance reported READY with zero failed
 * artefacts while the source was listed FAILED. Fixing the source heals both numbers.
 */
class JavaCompileFailureReadinessIT extends IntegrationTest {

    private static final String SOURCE_LOCATION = "/java-compile-failure-readiness-it/qabroken/Broken.java";

    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + SOURCE_LOCATION;

    private static final String BROKEN = "package qabroken; public class Broken { Strin x; }";

    private static final String FIXED = "package qabroken; public class Broken { String x; }";

    private static final String HEALTH = "/actuator/health";

    private static final String READINESS = "/services/core/readiness";

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void removeTheSource() {
        if (repository.hasResource(REGISTRY_PATH)) {
            repository.removeResource(REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void aSourceThatFailsToCompileDegradesReadinessUntilItIsFixed() {
        publish(BROKEN);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.details.failedByType.java", equalTo(1)),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(READINESS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("READY_DEGRADED"))
                                                 .body("failedArtefacts", greaterThan(0)),
                ASSERTION_TIMEOUT_SECONDS);

        publish(FIXED);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(HEALTH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("components.artefacts.details.failedByType", not(hasKey("java"))),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(READINESS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("status", equalTo("READY"))
                                                 .body("failedArtefacts", equalTo(0)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void publish(String source) {
        repository.createResource(REGISTRY_PATH, source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
