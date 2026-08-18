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

import java.sql.Connection;
import java.sql.Statement;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code JavaRepository.findById} answers {@code null} for an id that is not there, as its javadoc
 * documents.
 *
 * <p>
 * It used to throw {@code IllegalArgumentException} instead, which made every documented
 * {@code findById} + null-guard dead code: a controller looking a record up by a path parameter
 * returned {@code 500} for an unknown id instead of its own {@code 404}, and an event handler meant
 * to skip a dangling foreign key failed its whole run (issue #6420).
 */
class JavaRepositoryFindByIdIT extends IntegrationTest {

    private static final String PROJECT = "JavaRepositoryFindByIdIT";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/things/ThingController";
    private static final String TABLE_NAME = "FIND_BY_ID_THING";
    private static final int UNKNOWN_ID = 4242;
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void an_unknown_id_reads_back_as_null_not_as_a_failure() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // The controller's own 404 is reachable, on both the null-returning and the Optional variant.
        // This is also the first call, so it retries until the freshly compiled route is registered.
        assertResponse("/byId/" + UNKNOWN_ID, 404, "no such thing");
        assertResponse("/byOne/" + UNKNOWN_ID, 404, "no such thing");

        // The stored record still comes back - the null is about absence, not about the whole lookup.
        String id = seed();
        assertResponse("/byId/" + id, 200, "seeded");
        assertResponse("/byOne/" + id, 200, "seeded");
    }

    private String seed() {
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(CONTROLLER + "/seed")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString()
                                                                  .trim());
    }

    private void assertResponse(String path, int expectedStatus, String expectedFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + path)
                                                 .then()
                                                 .statusCode(expectedStatus)
                                                 .body(containsString(expectedFragment)),
                TIMEOUT_SECONDS);
    }

    /**
     * The fixture files go away with the Dirigible folder the base class wipes per test class; the
     * table itself would survive a local run against an unclean target and carry its rows into the next
     * one.
     */
    @AfterEach
    void dropTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
