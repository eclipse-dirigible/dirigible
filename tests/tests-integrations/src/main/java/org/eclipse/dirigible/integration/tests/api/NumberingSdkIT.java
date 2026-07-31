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
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.numbering.DocumentNumberService;
import org.eclipse.dirigible.components.engine.numbering.NumberSeriesDeclaration;
import org.eclipse.dirigible.components.engine.numbering.NumberSeriesDeclarationService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for first-class document numbering: the {@code .numbers} artefact synchronizer
 * (declare → provision per tenant; identical re-declaration skips; differing re-declaration fails
 * loudly; delete never touches a counter) and the {@code sdk.numbering.DocumentNumbers} allocation
 * (gap-free formatted sequences, per-partition independence, per-tenant independence). The series'
 * SHAPE lives in the {@code .numbers} declaration and the per-tenant settings - application code
 * only ever references the series by name.
 */
class NumberingSdkIT extends IntegrationTest {

    private static final String PROJECT = "numbering-it";
    private static final String SERIES = "NumberingIT";

    private static final String NUMBERS_LOCATION = "/" + PROJECT + "/" + PROJECT + ".numbers";
    private static final String NUMBERS_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + NUMBERS_LOCATION;
    private static final String RIVAL_NUMBERS_LOCATION = "/numbering-it-rival/rival.numbers";
    private static final String RIVAL_NUMBERS_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + RIVAL_NUMBERS_LOCATION;
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/api/NumberingTestController.java";
    private static final String CONTROLLER_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION;
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/api/NumberingTestController";

    /** Declared shape: prefix {@code T-} in a total width of 6 → {@code T-0001}. */
    private static final String NUMBERS_CONTENT = "{\"series\": [{\"name\": \"" + SERIES + "\", \"prefix\": \"T-\", \"size\": 6}]}";
    /** The same series declared DIFFERENTLY by another module - must fail that artefact. */
    private static final String RIVAL_NUMBERS_CONTENT = "{\"series\": [{\"name\": \"" + SERIES + "\", \"prefix\": \"X-\", \"size\": 8}]}";

    private static final String NUMBER_PATTERN = "T-\\d{4}";
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private NumberSeriesDeclarationService declarationService;

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private TenantContext tenantContext;

    @Test
    void allocatesAGapFreeFormattedSequenceFromADeclaredSeries() {
        publishDeclarationAndController();

        // Both allocations run inside one executor pass (which sets up auth); the assertion is
        // RELATIVE (b == a + 1) so a compile-readiness retry that re-runs the whole lambda still holds
        // - each pass draws two consecutive numbers rather than depending on an absolute start value.
        restAssuredExecutor.execute(() -> {
            String a = allocate("/next");
            String b = allocate("/next");
            assertTrue(a.matches(NUMBER_PATTERN), "formatted: " + a);
            assertTrue(b.matches(NUMBER_PATTERN), "formatted: " + b);
            assertEquals(value(a) + 1, value(b), "gap-free: " + a + " then " + b);
        }, ASSERTION_TIMEOUT_SECONDS);

        // A re-publish of the same declaration is a skip: the counter continues, it never resets.
        synchronizationProcessor.forceProcessSynchronizers();
        restAssuredExecutor.execute(() -> {
            String c = allocate("/next");
            String d = allocate("/next");
            assertEquals(value(c) + 1, value(d), "still gap-free after a re-sync: " + c + " then " + d);
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void allocatingAnUndeclaredSeriesFailsLoudly() {
        publishDeclarationAndController();

        restAssuredExecutor.execute(() -> {
            // The controller allocates fine for the declared series (proves it is compiled and live) ...
            allocate("/next");
            // ... and fails loudly for a series no .numbers artefact declares - never a silent default.
            given().when()
                   .get(ENDPOINT + "/undeclared")
                   .then()
                   .statusCode(500);
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void aDifferingRedeclarationFailsThatArtefactLoudlyNamingBothModules() {
        publishDeclarationAndController();

        repository.createResource(RIVAL_NUMBERS_PATH, RIVAL_NUMBERS_CONTENT.getBytes(StandardCharsets.UTF_8), false, "application/json",
                true);
        synchronizationProcessor.forceProcessSynchronizers();

        NumberSeriesDeclaration rival = declarationService.findAllByName(SERIES)
                                                          .stream()
                                                          .filter(d -> RIVAL_NUMBERS_LOCATION.equals(d.getLocation()))
                                                          .findFirst()
                                                          .orElseThrow(() -> new AssertionError("the rival declaration was not parsed"));
        assertEquals(ArtefactLifecycle.FAILED, rival.getLifecycle(), "a differing re-declaration must fail: " + rival);
        assertTrue(rival.getError()
                        .contains(NUMBERS_LOCATION)
                && rival.getError()
                        .contains(RIVAL_NUMBERS_LOCATION),
                "the failure must name both declaring modules: " + rival.getError());

        // The tenant's series is untouched by the conflict: it still allocates in the ORIGINAL shape.
        restAssuredExecutor.execute(() -> {
            String a = allocate("/next");
            assertTrue(a.matches(NUMBER_PATTERN), "the original shape must survive a conflicting declaration: " + a);
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void partitionsOfOneSeriesKeepIndependentSequences() {
        publishDeclarationAndController();

        restAssuredExecutor.execute(() -> {
            String a1 = allocate("/next/A");
            String b1 = allocate("/next/B");
            String a2 = allocate("/next/A");
            // Identical numbers across partitions are CORRECT - each partition owes its own sequential
            // range (two legal entities must not share a counter), in the shape inherited from the series.
            assertEquals(a1, b1, "each partition starts its own sequence: " + a1 + " vs " + b1);
            assertEquals(value(a1) + 1, value(a2), "another partition's allocation must not advance this one: " + a1 + " then " + a2);
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void aNewTenantGetsTheDeclaredSeriesWithItsOwnSequence() throws Exception {
        publishDeclarationAndController();

        // Advance the default tenant's sequence so "the new tenant starts at 1" is a real assertion.
        restAssuredExecutor.execute(() -> allocate("/next"), ASSERTION_TIMEOUT_SECONDS);

        DirigibleTestTenant tenant = new DirigibleTestTenant("numbering-it-tenant");
        createTenants(tenant);
        waitForTenantProvisioning(tenant);

        // The tenant status flips to PROVISIONED before the post-provisioning synchronizer retrigger
        // completes, so poll until the series is provisioned in the new tenant's schema - the first
        // allocation that succeeds IS the tenant's first number, so capture it.
        String[] first = new String[1];
        Awaitility.await()
                  .pollInterval(2, TimeUnit.SECONDS)
                  .atMost(60, TimeUnit.SECONDS)
                  .until(() -> {
                      try {
                          first[0] = tenantContext.execute(tenant.getId(), () -> documentNumberService.next(SERIES));
                          return true;
                      } catch (IllegalStateException notYetProvisioned) {
                          return false;
                      }
                  });

        assertEquals("T-0001", first[0], "a new tenant draws from its OWN provisioned sequence");
    }

    @AfterEach
    void cleanup() {
        boolean cleaned = false;
        for (String path : new String[] {CONTROLLER_PATH, NUMBERS_PATH, RIVAL_NUMBERS_PATH}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                cleaned = true;
            }
        }
        if (cleaned) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void publishDeclarationAndController() {
        repository.createResource(NUMBERS_PATH, NUMBERS_CONTENT.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String allocate(String path) {
        return given().when()
                      .get(ENDPOINT + path)
                      .then()
                      .statusCode(200)
                      .extract()
                      .asString();
    }

    /** The numeric sequence value of a rendered {@code T-NNNN} number. */
    private static int value(String number) {
        return Integer.parseInt(number.substring(2));
    }

    private static String controllerSource() {
        return """
                package api;

                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.http.PathParam;
                import org.eclipse.dirigible.sdk.numbering.DocumentNumbers;

                @Controller
                public class NumberingTestController {

                    @Get("/next")
                    public String next() {
                        return DocumentNumbers.next("NumberingIT");
                    }

                    @Get("/next/{partition}")
                    public String nextFor(@PathParam("partition") String partition) {
                        return DocumentNumbers.next("NumberingIT", partition);
                    }

                    @Get("/undeclared")
                    public String undeclared() {
                        return DocumentNumbers.next("NumberingUndeclaredIT");
                    }
                }
                """;
    }

}
