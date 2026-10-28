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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;

/**
 * The deprecation signals of server-side JavaScript and TypeScript (#8049): publishing a project
 * that has some logs one warning and adds one Problems view entry naming what was found and linking
 * the migration guide; a republish replaces the entry, an unpublish removes it, and a project with
 * Java and browser JavaScript only gets neither. A project migrated off them by deleting the files
 * from its workspace is clear after its next publish (#8195). HTTP only - the publish is the IDE's
 * own endpoint.
 */
// One boot for the class: each method works on its own project and asserts only about it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ServerSideScriptDeprecationIT extends IntegrationTest {

    private static final String FIXTURES = "ServerSideScriptDeprecationIT/";

    private static final String SCRIPTS_PROJECT = "deprecation-scripts";

    private static final String JAVA_PROJECT = "deprecation-java";

    private static final String MIGRATED_PROJECT = "deprecation-migrated";

    private static final String MIGRATED_JOB = "/" + MIGRATED_PROJECT + "/jobs/conv.job";

    private static final String GUIDE = "https://www.dirigible.io/help/develop/migrate-typescript-to-java";

    private static final String EXPECTED_WARNING =
            "Project [" + SCRIPTS_PROJECT + "] uses server-side JavaScript or TypeScript (1 TypeScript file, 1 extension module),"
                    + " which is deprecated and will be removed in 16.0. Migration guide: " + GUIDE;

    private static final String PROBLEMS = "/services/ide/problems";

    private static final String NOTICE = "/services/ide/javascript-deprecation";

    private static final String ARTEFACTS = "/services/core/artefacts";

    private static final long SYNCHRONIZATION_TIMEOUT_SECONDS = 60;

    private static final String DEPRECATION_LOGGER =
            "org.eclipse.dirigible.components.ide.workspace.deprecation.ServerSideScriptDeprecation";

    @Autowired
    private ProjectDeployer projectDeployer;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private LogsAsserter logsAsserter;

    /**
     * Attached here and not in a field initializer: Spring re-initializes logback while it starts the
     * application context, and that reset drops an appender attached any earlier.
     */
    @BeforeEach
    void attachLogsAsserter() {
        logsAsserter = new LogsAsserter(DEPRECATION_LOGGER, Level.INFO);
    }

    @Test
    void aProjectWithServerSideScriptsIsFlaggedUntilItIsUnpublished() {
        projectDeployer.deploy(FIXTURES + SCRIPTS_PROJECT);

        logsAsserter.assertLoggedMessage(EXPECTED_WARNING, Level.WARN);
        assertOneEntry();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTICE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("projects", hasItem(SCRIPTS_PROJECT))
                                                 .body("guide", equalTo(GUIDE)));

        // A republish replaces the entry rather than adding a second one.
        projectDeployer.deploy(FIXTURES + SCRIPTS_PROJECT);
        assertOneEntry();

        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/workspace/" + SCRIPTS_PROJECT)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(entriesOf(SCRIPTS_PROJECT) + ".size()", equalTo(0)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTICE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("projects", not(hasItem(SCRIPTS_PROJECT))));
    }

    @Test
    void aProjectWithJavaAndBrowserJavaScriptOnlyIsNotFlagged() {
        projectDeployer.deploy(FIXTURES + JAVA_PROJECT);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(entriesOf(JAVA_PROJECT) + ".size()", equalTo(0)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTICE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("projects", not(hasItem(JAVA_PROJECT))));
        assertThat(logsAsserter.containsMessage("Project [" + JAVA_PROJECT + "]", Level.WARN)).isFalse();
    }

    @Test
    void aRepublishAfterDeletingTheScriptsFromTheWorkspaceRemovesThemAndTheirArtefacts() {
        projectDeployer.deploy(FIXTURES + MIGRATED_PROJECT);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ARTEFACTS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(artefactsAt(MIGRATED_JOB) + ".status", contains("CREATED")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTICE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("projects", hasItem(MIGRATED_PROJECT)));

        deleteFromWorkspace(MIGRATED_PROJECT + "/jobs/conv.job");
        deleteFromWorkspace(MIGRATED_PROJECT + "/jobs/handler.js");
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/workspace/" + MIGRATED_PROJECT)
                                                 .then()
                                                 .statusCode(200));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web" + MIGRATED_JOB)
                                                 .then()
                                                 .statusCode(404));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ARTEFACTS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(artefactsAt(MIGRATED_JOB), empty()),
                SYNCHRONIZATION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTICE)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("projects", not(hasItem(MIGRATED_PROJECT))));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(entriesOf(MIGRATED_PROJECT) + ".size()", equalTo(0)));
    }

    private void deleteFromWorkspace(String path) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/workspaces/workspace/" + path)
                                                 .then()
                                                 .statusCode(204));
    }

    /** A GPath selecting the artefacts synchronized from the location. */
    private static String artefactsAt(String location) {
        return "findAll { it.location == '" + location + "' }";
    }

    private void assertOneEntry() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(entriesOf(SCRIPTS_PROJECT) + ".size()", equalTo(1))
                                                 .body(entriesOf(SCRIPTS_PROJECT) + "[0].type", equalTo("project"))
                                                 .body(entriesOf(SCRIPTS_PROJECT) + "[0].cause", equalTo(EXPECTED_WARNING)));
    }

    /** A GPath selecting the project's deprecation entries out of the Problems view's list. */
    private static String entriesOf(String project) {
        return "findAll { it.category == 'JavaScript deprecation' && it.location == '/" + project + "' }";
    }
}
