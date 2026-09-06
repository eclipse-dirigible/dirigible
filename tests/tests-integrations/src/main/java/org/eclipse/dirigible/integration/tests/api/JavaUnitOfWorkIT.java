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
 * {@code UnitOfWork} makes several entity writes one transaction: a failure anywhere in the block
 * leaves none of them behind.
 *
 * <p>
 * Each repository call is otherwise its own transaction, which is what let a create-from commit an
 * invoice header and flip its source to INVOICED and then fail on a line - a document that exists,
 * counts as the period's billing, and is missing what it was for (issue #7069). The control case
 * here runs the same two writes without the block, so the assertion is about the block and not
 * about the database happening to refuse both.
 */
class JavaUnitOfWorkIT extends IntegrationTest {

    private static final String PROJECT = "JavaUnitOfWorkIT";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/ledger/EntryController";
    private static final String TABLE_NAME = "UOW_LEDGER_ENTRY";
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
    void a_failed_write_takes_the_whole_unit_of_work_with_it() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // The first call, so it retries until the freshly compiled route is registered.
        assertGet("/unit/pass/committed", 200, "written");
        assertGet("/count/committed", 200, "2");

        // The refused second write rolls the first one back with it.
        assertGet("/unit/fail/rolledback", 500);
        assertGet("/count/rolledback", 200, "0");

        // Without the block the same pair leaves the first write behind - which is the defect, and
        // what makes the assertion above about the unit of work rather than about the failure.
        assertGet("/nounit/fail/leftbehind", 500);
        assertGet("/count/leftbehind", 200, "1");

        // A read inside the block sees the block's own uncommitted writes, so a guard that re-reads
        // the row it just wrote behaves as it would after a commit.
        assertGet("/unit/reads/visible", 200, "visible");
    }

    private void assertGet(String path, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + path)
                                                 .then()
                                                 .statusCode(expectedStatus),
                TIMEOUT_SECONDS);
    }

    private void assertGet(String path, int expectedStatus, String expectedBody) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + path)
                                                 .then()
                                                 .statusCode(expectedStatus)
                                                 .body(containsString(expectedBody)),
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
