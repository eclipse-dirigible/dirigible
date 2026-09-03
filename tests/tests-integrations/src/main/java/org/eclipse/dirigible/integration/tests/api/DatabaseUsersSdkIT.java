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

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end test for the user and schema DDL on {@code sdk.db.Database}: a client class creates a
 * database user, rotates its password, creates a schema owned by it and drops both, without writing
 * a line of SQL. The suite runs on H2 and on PostgreSQL, which is the point - the two spell these
 * statements differently ({@code ALTER USER ... SET PASSWORD} against {@code ALTER USER ...
 * PASSWORD}) and keep their principals in catalogs of their own.
 */
// One Dirigible boot for the whole class: the test cleans up after itself, so the per-method
// context
// reset inherited from IntegrationTest would only add boot time.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class DatabaseUsersSdkIT extends IntegrationTest {

    private static final String PROJECT = "db-users-it";
    private static final String CONTROLLER_LOCATION = "/" + PROJECT + "/api/DatabaseUsersTestController.java";
    private static final String CONTROLLER_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + CONTROLLER_LOCATION;
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/api/DatabaseUsersTestController";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void createsRotatesAndDropsAUserAndItsSchemaWithoutAnySql() {
        publishController();

        // One pass: the whole life cycle is asserted as a single string so a compile-readiness retry
        // re-runs it from a clean state rather than half-way through.
        restAssuredExecutor.execute(() -> assertEquals("absent,created,rotated,schema-created,schema-absent,absent", call("/lifecycle"),
                "the user and schema life cycle must round-trip through the SDK"), ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasResource(CONTROLLER_PATH)) {
            repository.removeResource(CONTROLLER_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void publishController() {
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String call(String path) {
        return given().when()
                      .get(ENDPOINT + path)
                      .then()
                      .statusCode(200)
                      .extract()
                      .asString();
    }

    private static String controllerSource() {
        return """
                package api;

                import java.util.ArrayList;
                import java.util.List;

                import org.eclipse.dirigible.sdk.db.Database;
                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;

                @Controller
                public class DatabaseUsersTestController {

                    private static final String USER = "IT_DB_USER";
                    private static final String SCHEMA = "IT_DB_SCHEMA";

                    @Get("/lifecycle")
                    public String lifecycle() throws Throwable {
                        // Leftovers from a previous run would make every step below lie.
                        if (Database.existsSchema(SCHEMA)) {
                            Database.dropSchema(SCHEMA, true);
                        }
                        if (Database.existsUser(USER)) {
                            Database.dropUser(USER);
                        }

                        List<String> steps = new ArrayList<>();
                        steps.add(Database.existsUser(USER) ? "present" : "absent");

                        Database.createUser(USER, "S3cret-first");
                        steps.add(Database.existsUser(USER) ? "created" : "missing");

                        Database.setUserPassword(USER, "S3cret-second");
                        steps.add(Database.existsUser(USER) ? "rotated" : "missing");

                        Database.createSchema(SCHEMA, USER);
                        steps.add(Database.existsSchema(SCHEMA) ? "schema-created" : "schema-missing");

                        Database.dropSchema(SCHEMA, true);
                        steps.add(Database.existsSchema(SCHEMA) ? "schema-present" : "schema-absent");

                        Database.dropUser(USER);
                        steps.add(Database.existsUser(USER) ? "present" : "absent");

                        return String.join(",", steps);
                    }
                }
                """;
    }

}
