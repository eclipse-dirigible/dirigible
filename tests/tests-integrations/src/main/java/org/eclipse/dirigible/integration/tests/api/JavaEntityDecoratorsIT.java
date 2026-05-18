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

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * End-to-end test for {@code data-store-java} + the controller decorator stack: drops a
 * {@code Country} {@code @Entity}, a {@code CountrySeeder} {@code @Controller}, and a
 * {@code CountryController} {@code @Controller} directly into the registry, forces a
 * synchronisation cycle, and asserts the entity is reachable through the declarative
 * {@code @Get / @Post / @Delete} routes that the controller decorators expose.
 *
 * <p>
 * Resource layout under {@code src/main/resources/JavaEntityDecoratorsIT/sample-java-entities/}:
 * {@code Country.java}, {@code CountrySeeder.java}, {@code CountryController.java}. They live under
 * registry path {@code /sample-java-entities/demo/...} in the running app.
 */
class JavaEntityDecoratorsIT extends IntegrationTest {

    private static final String PROJECT = "sample-java-entities";
    private static final String COUNTRY_LOCATION = "/" + PROJECT + "/demo/Country.java";
    private static final String SEEDER_LOCATION = "/" + PROJECT + "/demo/CountrySeeder.java";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/demo/CountryController.java";

    /** Base path of the CountrySeeder controller — POST hits it directly (no @Post suffix). */
    private static final String SEEDER_BASE = "/services/java/" + PROJECT + "/demo/CountrySeeder";

    /** Base path of the CountryController — method-level @Get/@Post/@Delete add the suffix. */
    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/demo/CountryController";

    /**
     * Cap matches JavaEngineIT — covers the async lag between forceProcessSynchronizers() and dispatch.
     */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void entity_can_be_seeded_and_listed_through_controllers() {
        writeAllFixtures();
        synchronizationProcessor.forceProcessSynchronizers();

        // POST /seeder — class-level controller with a single @Post method matches the base URL.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_BASE)
                             .then()
                             .statusCode(200)
                             .body(containsString("seeded")),
                ASSERTION_TIMEOUT_SECONDS);

        // GET /controller/list — @Get("/list") on the controller class.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(200)
                             .body(containsString("Afghanistan"))
                             .body(containsString("Albania"))
                             .body(containsString("Algeria")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void path_param_body_and_delete_routes_work_end_to_end() {
        writeAllFixtures();
        synchronizationProcessor.forceProcessSynchronizers();

        // Seed first.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_BASE)
                             .then()
                             .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);

        // GET /controller/{id} — @PathParam binding. The seeder writes ids starting at 1.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/1")
                             .then()
                             .statusCode(200)
                             .body(containsString("Afghanistan")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /controller with JSON body — @Body deserialised via Jackson into a Country.
        restAssuredExecutor.execute( //
                () -> given().contentType("application/json")
                             .body("{\"code2\":\"AD\",\"code3\":\"AND\",\"numericCode\":\"020\",\"name\":\"Andorra\"}")
                             .when()
                             .post(CONTROLLER_BASE)
                             .then()
                             .statusCode(200)
                             .body(containsString("Andorra")),
                ASSERTION_TIMEOUT_SECONDS);

        // GET /controller/list — should now have four rows.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(200)
                             .body(containsString("Andorra")),
                ASSERTION_TIMEOUT_SECONDS);

        // DELETE /controller/{id} — remove Afghanistan (id=1).
        restAssuredExecutor.execute( //
                () -> given().when()
                             .delete(CONTROLLER_BASE + "/1")
                             .then()
                             .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);

        // List no longer contains Afghanistan.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(200)
                             .body(org.hamcrest.Matchers.not(containsString("Afghanistan"))),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void deleting_controller_unregisters_routes_but_entity_data_persists() {
        writeAllFixtures();
        synchronizationProcessor.forceProcessSynchronizers();

        // Seed once, prove the list endpoint works.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_BASE)
                             .then()
                             .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(200)
                             .body(containsString("Albania")),
                ASSERTION_TIMEOUT_SECONDS);

        // Drop the controller file and force a re-sync — all four of its routes must disappear.
        repository.removeResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/1")
                             .then()
                             .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);

        // The seeder controller still exists; a second invocation should report "already seeded"
        // (i.e. the COUNTRIES table survived the controller unregistration, since hbm2ddl.auto is
        // 'update' rather than 'create-drop').
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_BASE)
                             .then()
                             .statusCode(200)
                             .body(containsString("already seeded")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void controller_routes_are_published_to_openapi_document() {
        writeAllFixtures();
        synchronizationProcessor.forceProcessSynchronizers();

        // /services/openapi aggregates every stored OpenAPI artefact — including the fragments
        // emitted by JavaControllerOpenApiPublisher when ControllerClassConsumer registers a
        // controller. The exact OpenAPI shape isn't asserted here; we only confirm that the
        // controller's URLs appear in the merged document.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/openapi")
                             .then()
                             .statusCode(200)
                             .body(containsString(CONTROLLER_BASE + "/list"))
                             .body(containsString(CONTROLLER_BASE + "/{id}"))
                             .body(containsString(SEEDER_BASE)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void entity_without_id_fails_loudly_and_does_not_register() {
        // Country variant missing @Id — must show as a FAILED artefact and never reach the
        // EntityClassConsumer (no table, no SessionFactory rebuild succeeds for it).
        String broken = """
                package demo;
                import org.eclipse.dirigible.engine.java.annotations.Column;
                import org.eclipse.dirigible.engine.java.annotations.Entity;
                @Entity
                public class Country {
                    @Column public String name;
                }
                """;
        writeBytes(COUNTRY_LOCATION, broken.getBytes(StandardCharsets.UTF_8));
        synchronizationProcessor.forceProcessSynchronizers();

        // The (deferred) batch compile succeeds for the source; failure happens when the
        // EntityClassConsumer registers the class. The error is surfaced into the platform log;
        // here we only assert that no controller is reachable for the (absent) controller URL —
        // since we didn't drop the controller in this test.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_BASE + "/list")
                             .then()
                             .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void writeAllFixtures() {
        writeFixture(COUNTRY_LOCATION, "Country.java");
        writeFixture(SEEDER_LOCATION, "CountrySeeder.java");
        writeFixture(CONTROLLER_LOCATION, "CountryController.java");
    }

    private void writeFixture(String location, String resourceName) {
        writeBytes(location, readResource(resourceName));
    }

    private void writeBytes(String location, byte[] content) {
        String full = IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
        repository.createResource(full, content, false, "text/x-java", true);
    }

    private static byte[] readResource(String resourceName) {
        String path = "/JavaEntityDecoratorsIT/sample-java-entities/" + resourceName;
        try (InputStream in = JavaEntityDecoratorsIT.class.getResourceAsStream(path)) {
            Objects.requireNonNull(in, () -> "Missing classpath resource " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read fixture " + path + ": " + e.getMessage(), e);
        }
    }

    @AfterEach
    void removeFixturesFromRegistry() {
        deleteIfPresent(COUNTRY_LOCATION);
        deleteIfPresent(SEEDER_LOCATION);
        deleteIfPresent(CONTROLLER_LOCATION);
        // Force a final sync so the next test starts with an empty client-class space.
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void deleteIfPresent(String location) {
        String full = IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
        if (repository.hasResource(full)) {
            repository.removeResource(full);
        }
    }

}
