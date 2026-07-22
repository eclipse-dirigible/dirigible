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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * End-to-end test for the first-class numbering SDK ({@code sdk.numbering.DocumentNumbers}). Drops
 * a client-Java {@code @Controller} that allocates the next number for a series, force-syncs it,
 * and asserts over HTTP (in the caller's tenant scope) that successive calls yield a gap-free,
 * formatted sequence - exercising the SDK bridge → the platform counter store (engine-numbering).
 */
class NumberingSdkIT extends IntegrationTest {

    private static final String PROJECT = "numbering-it";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/api/NumberingTestController.java";
    private static final String CONTROLLER_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION;
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/api/NumberingTestController/next";
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void allocatesAGapFreeFormattedSequence() {
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();

        // Both allocations run inside one executor pass (which sets up auth); the assertion is
        // RELATIVE (b == a + 1) so a compile-readiness retry that re-runs the whole lambda still holds
        // - each pass draws two consecutive numbers rather than depending on an absolute start value.
        restAssuredExecutor.execute(() -> {
            String a = given().when()
                              .get(ENDPOINT)
                              .then()
                              .statusCode(200)
                              .extract()
                              .asString();
            String b = given().when()
                              .get(ENDPOINT)
                              .then()
                              .statusCode(200)
                              .extract()
                              .asString();
            assertTrue(a.matches("T-\\d{4}"), "formatted: " + a);
            assertTrue(b.matches("T-\\d{4}"), "formatted: " + b);
            assertEquals(Integer.parseInt(a.substring(2)) + 1, Integer.parseInt(b.substring(2)), "gap-free: " + a + " then " + b);
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasResource(CONTROLLER_PATH)) {
            repository.removeResource(CONTROLLER_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private static String controllerSource() {
        return """
                package api;

                import java.util.Map;

                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.numbering.DocumentNumbers;

                @Controller
                public class NumberingTestController {

                    @Get("/next")
                    public String next() {
                        return DocumentNumbers.next("NumberingIT", "T-{seq:04}", Map.of());
                    }
                }
                """;
    }

}
