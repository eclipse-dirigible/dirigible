/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import io.restassured.http.ContentType;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.junit.jupiter.api.BeforeEach;

import ch.qos.logback.classic.Level;

public class JavaJobDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-job-decorator.git";
    }

    @Override
    protected void verifyProject() {
        // Self-describing interface style — CleanupJob implements JobHandler (schedule from cron()).
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("CleanupJob executed!", Level.INFO));

        // Method-level annotation style — Maintenance's @Scheduled method.
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Maintenance.purgeTempFiles executed", Level.INFO));

        // Client-Java jobs are now real Job definitions on the shared scheduler, so they are VISIBLE
        // and MONITORED in the Jobs perspective (which reads /services/jobs) with engine "java" -
        // not hidden on a private in-JVM scheduler as before.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.engine == 'java' }.size()", greaterThanOrEqualTo(2)));

        // And they SURVIVE a registry synchronization pass - the synchronizer must not reap the
        // runtime-registered rows (they are not backed by a registry artefact).
        synchronizationProcessor.forceProcessSynchronizers();
        SynchronizationUtil.waitForStableSynchronization();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.engine == 'java' }.size()", greaterThanOrEqualTo(2)));

        verifyTriggerNowRunsTheClientJavaJob();
    }

    /**
     * Trigger-now (the Jobs perspective's play button) must run a client-Java job on the Java engine.
     * The manual path used to be JavaScript-only, so it tried to run the job's CLASS NAME as a
     * repository path to a JS module and answered 500 (dirigible #6305).
     *
     * <p>
     * The status is the whole assertion: the Java dispatch either resolves the client bean and invokes
     * it, or throws (an unknown bean, a job body that fails) - and either way the endpoint surfaces
     * that as a 500. It cannot answer 200 without having run the job.
     */
    private void verifyTriggerNowRunsTheClientJavaJob() {
        String name = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                         .get("/services/jobs")
                                                                         .then()
                                                                         .statusCode(200)
                                                                         .extract()
                                                                         .path("find { it.engine == 'java' }.name"));

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("[]")
                                                 .when()
                                                 .post("/services/jobs/trigger/" + name)
                                                 .then()
                                                 .statusCode(200));
    }

}
