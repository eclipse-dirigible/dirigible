/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IntentEditorView;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * The {@code sample-intent-resilience} fixture project (dirigible #6762), configured exactly as a
 * developer would in the browser IDE - copy into the workspace, open {@code app.intent} in the
 * Intent Editor and click its Generate, then Publish all from the Workbench - and verified over
 * REST. The fixture doubles as the manual-testing sample (see its README), so this project is what
 * keeps the sample from silently rotting - it asserts both outcomes the sample was built to
 * demonstrate:
 *
 * <ul>
 * <li>a tenant titled normally recovers by RETRY: the schema delegate fails its first two attempts,
 * the declared {@code retry: { count: 3, every: PT10S }} carries it to the third, the produced
 * {@code dbPassword} flows through {@code uses:} into the stamped key, and {@code clearAfter}
 * removes the credential so it does not survive in the process history;
 * <li>a tenant titled with "fail" EXHAUSTS its retry: the app delegate refuses every attempt, its
 * {@code retry: { count: 2 }} allows exactly three, and the {@code onError} route records the FINAL
 * attempt's message on the record via {@code {error}} - instead of a dead-letter incident.
 * </ul>
 */
@Lazy
@Component
class SampleIntentResilienceTestProject extends BaseTestProject {

    private static final String PROJECT = "sample-intent-resilience";
    private static final String INTENT_FILE = "app.intent";
    private static final String TENANT_API =
            "/services/java/" + PROJECT + "/gen/provisioning/api/tenantapplication/" + "TenantApplicationController";

    /** Seed ids of the ProvisioningStatus nomenclature the sample's setters write. */
    private static final int STATUS_PROVISIONED = 2;
    private static final int STATUS_FAILED = 3;

    private final IntentEditorView intentEditorView;
    private final RestAssuredExecutor restAssuredExecutor;

    SampleIntentResilienceTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, IntentEditorView intentEditorView,
            RestAssuredExecutor restAssuredExecutor) {
        super(PROJECT, ide, projectUtil, edmView);
        this.intentEditorView = intentEditorView;
        this.restAssuredExecutor = restAssuredExecutor;
    }

    @Override
    public void configure() {
        copyToWorkspace();
        // Opening the workbench logs into the IDE and binds the browser session - the same prologue
        // BaseTestProject.generateEDM performs before driving its editor view.
        getIde().openWorkbench();
        intentEditorView.generate(getProjectResourcesFolder(), INTENT_FILE);
        publish();
    }

    @Override
    public void verify() {
        // Both tenants up front, so their retry cycles run concurrently.
        AtomicInteger recovering = new AtomicInteger();
        AtomicInteger doomed = new AtomicInteger();
        restAssuredExecutor.execute(() -> recovering.set(given().contentType("application/json")
                                                                .body("{\"Title\":\"acme\"}")
                                                                .when()
                                                                .post(TENANT_API)
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")));
        restAssuredExecutor.execute(() -> doomed.set(given().contentType("application/json")
                                                            .body("{\"Title\":\"please fail\"}")
                                                            .when()
                                                            .post(TENANT_API)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .path("Id")));

        // Happy path: the schema delegate failed twice on purpose; the declared PT10S cycle re-ran
        // it to success, and the produced dbPassword reached the app delegate through `uses:` - an
        // "app-...-missing" key would mean the step data never flowed. The poll is generous: two
        // 10s retry waits plus the async executor's acquire cycles on a loaded box.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TENANT_API + "/" + recovering.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("GeneratedKey", allOf(startsWith("app-"), not(endsWith("missing"))))
                                                 .body("Status", equalTo(STATUS_PROVISIONED)),
                180);

        // clearAfter: the credential must not survive in the completed instance's history - the
        // end-listener removed the runtime variable and Flowable dropped the historic row with it.
        AtomicReference<String> processId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> processId.set(given().when()
                                                               .get(TENANT_API + "/" + recovering.get())
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .path("ProcessId")));
        restAssuredExecutor.execute(() -> {
            String variables = given().when()
                                      .get("/services/bpm/bpm-processes/historic-instances/" + processId.get() + "/variables")
                                      .then()
                                      .statusCode(200)
                                      .extract()
                                      .asString();
            assertTrue(variables.contains("__entityUrl"),
                    "the historic instance must be inspectable (its context variables present), got: " + variables);
            assertFalse(variables.contains("dbPassword"),
                    "clearAfter must keep the produced credential out of the process history, got: " + variables);
        });

        // Failure path: the app delegate refused all three allowed attempts (1 + count: 2), the
        // runtime conversion routed the EXHAUSTED failure to the onError step, and {error} recorded
        // exactly the final attempt's message - no dead-letter incident, a filed record instead.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TENANT_API + "/" + doomed.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("FailureMessage", equalTo("no capacity for 'please fail' (attempt 3)"))
                                                 .body("Status", equalTo(STATUS_FAILED)),
                180);
    }
}
