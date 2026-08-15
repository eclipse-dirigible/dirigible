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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.restassured.http.ContentType;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end test for first-class document numbering: the {@code .numbers} artefact synchronizer
 * (declare → provision per tenant; identical re-declaration skips; differing re-declaration fails
 * loudly; delete never touches a counter) and the {@code sdk.numbering.DocumentNumbers} allocation
 * (gap-free formatted sequences, per-partition independence, per-tenant independence). The series'
 * SHAPE lives in the {@code .numbers} declaration and the per-tenant settings - application code
 * only ever references the series by name.
 */
// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add ~10s of boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
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

    private static final String PARTITIONED_SERIES = "NumberingPartIT";
    private static final String PARTITION_TABLE = "NUMBERING_IT_COMPANY";

    /** Declared shape: prefix {@code T-} in a total width of 6 → {@code T-0001}. */
    private static final String NUMBERS_CONTENT = "{\"series\": [{\"name\": \"" + SERIES + "\", \"prefix\": \"T-\", \"size\": 6},"
            + " {\"name\": \"" + PARTITIONED_SERIES + "\", \"prefix\": \"P-\", \"size\": 6," + " \"partitions\": {\"table\": \""
            + PARTITION_TABLE + "\", \"key\": \"COMPANY_ID\", \"label\": \"COMPANY_NAME\"}}]}";
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
    private DataSourcesManager dataSourcesManager;

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
    void theSettingsSurfaceSeedsPartitionCountersAndTheBaseRowIsOnlyTheShapeTemplate() {
        publishDeclarationAndController();

        restAssuredExecutor.execute(() -> {
            // Materialize a partition: its first allocation copies the base shape and starts its own
            // counter (partition values are data - no artefact can pre-provision them).
            allocate("/next/SEED");

            // The management surface flags the series PARTITIONED - the base ("") row is only the
            // shape template new partitions inherit, so the settings page offers no counter on it.
            given().when()
                   .get("/services/core/numbering")
                   .then()
                   .statusCode(200)
                   .body("find { it.series == '" + SERIES + "' && it.partition == '' }.partitioned", equalTo(true))
                   .body("find { it.series == '" + SERIES + "' && it.partition == 'SEED' }.partitioned", equalTo(true));

            // Editing the BASE row's next must not affect partition allocations (the observed trap:
            // an operator edits the base Next expecting to seed the next invoice number)...
            given().contentType(ContentType.JSON)
                   .body("{\"series\": \"" + SERIES + "\", \"partition\": \"\", \"next\": 500}")
                   .when()
                   .put("/services/core/numbering")
                   .then()
                   .statusCode(204);

            // ...while a PARTITION row's next IS the seed: it round-trips and the next issued number
            // renders exactly it.
            given().contentType(ContentType.JSON)
                   .body("{\"series\": \"" + SERIES + "\", \"partition\": \"SEED\", \"next\": 42}")
                   .when()
                   .put("/services/core/numbering")
                   .then()
                   .statusCode(204);
            given().when()
                   .get("/services/core/numbering")
                   .then()
                   .statusCode(200)
                   .body("find { it.series == '" + SERIES + "' && it.partition == 'SEED' }.next", equalTo(42));

            assertEquals("T-0042", allocate("/next/SEED"), "the partition row's Next is what the next document renders");
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void aDeclaredPartitionSourceLabelsRowsAndSeedsCountersBeforeFirstUse() throws Exception {
        // The partition source: the table whose rows ARE the partition values (per: Company).
        try (java.sql.Connection connection = dataSourcesManager.getDefaultDataSource()
                                                                .getConnection();
                java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE \"" + PARTITION_TABLE + "\" (\"COMPANY_ID\" INTEGER PRIMARY KEY, \"COMPANY_NAME\" VARCHAR(100))");
            statement.executeUpdate("INSERT INTO \"" + PARTITION_TABLE + "\" VALUES (7, 'ACME Ltd.'), (9, 'Globex')");
        }
        publishDeclarationAndController();

        restAssuredExecutor.execute(() -> {
            // Every declared partition value appears BEFORE its first allocation - a VIRTUAL row
            // rendered from the base shape, labeled by the entity's display name.
            given().when()
                   .get("/services/core/numbering")
                   .then()
                   .statusCode(200)
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '7' }.partitionLabel", equalTo("ACME Ltd."))
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '7' }.virtual", equalTo(true))
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '9' }.partitionLabel", equalTo("Globex"))
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '' }.partitioned", equalTo(true));

            // Seeding the virtual row provisions it: the operator sets the company's starting number
            // BEFORE its first document...
            given().contentType(ContentType.JSON)
                   .body("{\"series\": \"" + PARTITIONED_SERIES + "\", \"partition\": \"7\", \"next\": 42}")
                   .when()
                   .put("/services/core/numbering")
                   .then()
                   .statusCode(204);
            given().when()
                   .get("/services/core/numbering")
                   .then()
                   .statusCode(200)
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '7' }.virtual", equalTo(false))
                   .body("find { it.series == '" + PARTITIONED_SERIES + "' && it.partition == '7' }.next", equalTo(42));

            // ...and the FIRST issued document renders exactly it.
            assertEquals("P-0042", given().when()
                                          .get(ENDPOINT + "/nextPartitioned/7")
                                          .then()
                                          .statusCode(200)
                                          .extract()
                                          .asString(),
                    "the seeded Next is what the first document renders");
        }, ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void aFreshPartitionInheritsTheBaseRowsSeededCounter() {
        publishDeclarationAndController();

        restAssuredExecutor.execute(() -> {
            // On a fresh tenant nothing marks the series partitioned yet, so the base row is the only
            // thing an operator CAN seed before the first document. Seed it...
            given().contentType(ContentType.JSON)
                   .body("{\"series\": \"" + SERIES + "\", \"partition\": \"\", \"next\": 300}")
                   .when()
                   .put("/services/core/numbering")
                   .then()
                   .statusCode(204);
            // ...and the FIRST allocation of a brand-new partition must render exactly the seed - the
            // partition materializes inheriting the base row's shape AND counter (a zero-started
            // partition silently discarded the seed: the first document rendered ...0001).
            assertEquals("T-0300", allocate("/next/FRESHSEED"), "a fresh partition's first number is the base row's seeded next value");
            assertEquals("T-0301", allocate("/next/FRESHSEED"), "and it continues from there");
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
    void cleanup() throws Exception {
        try (java.sql.Connection connection = dataSourcesManager.getDefaultDataSource()
                                                                .getConnection();
                java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS \"" + PARTITION_TABLE + "\"");
        }
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

                    @Get("/nextPartitioned/{partition}")
                    public String nextPartitioned(@PathParam("partition") String partition) {
                        return DocumentNumbers.next("NumberingPartIT", partition);
                    }

                    @Get("/undeclared")
                    public String undeclared() {
                        return DocumentNumbers.next("NumberingUndeclaredIT");
                    }
                }
                """;
    }

}
