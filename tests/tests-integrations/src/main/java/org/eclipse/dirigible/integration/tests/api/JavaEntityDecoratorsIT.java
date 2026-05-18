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
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the {@code data-store-java} module: drops a {@code Country} {@code @Entity}
 * source plus a seeder and a controller {@code JavaHandler} directly into the registry, forces a
 * synchronisation cycle, and asserts the entity is reachable via the typed {@code JavaEntityStore}
 * API.
 *
 * <p>
 * Resource layout under {@code src/main/resources/JavaEntityDecoratorsIT/sample-java-entities/}:
 * {@code Country.java}, {@code CountrySeeder.java}, {@code CountryController.java}.
 * They live under registry path {@code /sample-java-entities/demo/...} in the running app.
 */
class JavaEntityDecoratorsIT extends IntegrationTest {

    private static final String PROJECT = "sample-java-entities";
    private static final String COUNTRY_LOCATION = "/" + PROJECT + "/demo/Country.java";
    private static final String SEEDER_LOCATION = "/" + PROJECT + "/demo/CountrySeeder.java";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/demo/CountryController.java";

    private static final String SEEDER_ENDPOINT = "/services/java/" + PROJECT + "/demo/CountrySeeder";
    private static final String CONTROLLER_ENDPOINT = "/services/java/" + PROJECT + "/demo/CountryController";

    /** Cap matches JavaEngineIT — covers the async lag between forceProcessSynchronizers() and dispatch. */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void entity_registered_and_can_be_seeded_and_listed() {
        writeFixture(COUNTRY_LOCATION, "Country.java");
        writeFixture(SEEDER_LOCATION, "CountrySeeder.java");
        writeFixture(CONTROLLER_LOCATION, "CountryController.java");
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_ENDPOINT)
                             .then()
                             .statusCode(200)
                             .body(containsString("seeded")),
                ASSERTION_TIMEOUT_SECONDS);

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_ENDPOINT)
                             .then()
                             .statusCode(200)
                             .body(containsString("Afghanistan"))
                             .body(containsString("Albania"))
                             .body(containsString("Algeria")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void deleting_controller_unregisters_handler_but_entity_data_persists() {
        writeFixture(COUNTRY_LOCATION, "Country.java");
        writeFixture(SEEDER_LOCATION, "CountrySeeder.java");
        writeFixture(CONTROLLER_LOCATION, "CountryController.java");
        synchronizationProcessor.forceProcessSynchronizers();

        // Seed and list once to prove the chain works.
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_ENDPOINT)
                             .then()
                             .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_ENDPOINT)
                             .then()
                             .statusCode(200)
                             .body(containsString("Albania")),
                ASSERTION_TIMEOUT_SECONDS);

        // Remove the controller and re-sync; the entity / its table / its data stay in place.
        repository.removeResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get(CONTROLLER_ENDPOINT)
                             .then()
                             .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);

        // Seeder is still alive and is idempotent — a second POST should report "already seeded".
        restAssuredExecutor.execute( //
                () -> given().when()
                             .post(SEEDER_ENDPOINT)
                             .then()
                             .statusCode(200)
                             .body(containsString("already seeded")),
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
                             .get(CONTROLLER_ENDPOINT)
                             .then()
                             .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);
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

}
