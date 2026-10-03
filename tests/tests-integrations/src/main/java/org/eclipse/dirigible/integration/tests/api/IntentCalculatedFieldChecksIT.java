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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.specification.RequestSpecification;

/**
 * A check on a calculated field reads the value the write will store, not the one the caller sent
 * (#7544).
 *
 * <p>
 * The rule is the issue's own: warn - never refuse - when an invoice is issued more than 5 days
 * after its tax event (a legal deadline for issuing; late issue happens and must still go through).
 * It is declared as a calculated delay and a soft compare on it. The controllers ran their checks
 * BEFORE the write, while the calculated expressions are assigned inside it, so the warning
 * compared the payload's value: absent on a create, stale on an edit, and the soft tier was
 * enforced only when a client happened to send the computed number. A refusing compare on the same
 * calculated field was blind the same way, which the second check here pins.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCalculatedFieldChecksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "calcchecks";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String INVOICES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/invoice/InvoiceController";
    private static final String CONFIRM = "X-Confirm-Warnings";
    private static final String LATE_ISSUE = "Invoice.compare.1";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: calcchecks
            description: calculated-field checks fixture - the checks judge the computed delay, not the payload

            entities:
              - name: Invoice
                checks:
                  - { kind: compare, field: issueDelayDays, op: ge, value: 0, message: "An invoice cannot predate its tax event" }
                  - { kind: compare, field: issueDelayDays, op: le, value: 5, severity: warn,
                      message: "Issued more than 5 days after the tax event" }
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: date,           type: date }
                  - { name: taxEventDate,   type: date }
                  - { name: issueDelayDays, type: integer,
                      calculatedOnCreate: "daysBetween(TaxEventDate, Date)",
                      calculatedOnUpdate: "daysBetween(TaxEventDate, Date)" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_checks_judge_the_computed_delay_on_create_and_on_update() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Within the deadline: nothing to ask, and the delay is stored as computed.
        int onTime = create("{\"Date\":\"2026-09-29\",\"TaxEventDate\":\"2026-09-26\"}", null);
        assertStoredDelay(onTime, 3);

        // Nine days late, the payload carrying no delay at all: the person saving is asked, and nothing
        // is written until they confirm.
        restAssuredExecutor.execute(() -> warned(post("{\"Date\":\"2026-09-29\",\"TaxEventDate\":\"2026-09-20\"}", null)), TIMEOUT_SECONDS);
        assertCount(1);
        int late = create("{\"Date\":\"2026-09-29\",\"TaxEventDate\":\"2026-09-20\"}", LATE_ISSUE);
        assertStoredDelay(late, 9);

        // An edit moving the tax event back, the payload still carrying a stale, harmless delay: the
        // warning reads the delay the update computes (19), not the 2 it was sent.
        String moved = "{\"Date\":\"2026-09-29\",\"TaxEventDate\":\"2026-09-10\",\"IssueDelayDays\":2}";
        restAssuredExecutor.execute(() -> warned(put(onTime, moved, null)), TIMEOUT_SECONDS);
        assertStoredDelay(onTime, 3);
        // ...and the same edit with the delay omitted is asked the same way.
        restAssuredExecutor.execute(() -> warned(put(onTime, "{\"Date\":\"2026-09-29\",\"TaxEventDate\":\"2026-09-10\"}", null)),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> put(onTime, moved, LATE_ISSUE).statusCode(200), TIMEOUT_SECONDS);
        assertStoredDelay(onTime, 19);

        // The refusing check on the same calculated field is not blind either: an invoice dated before
        // its tax event is refused outright, whatever delay the payload claims.
        restAssuredExecutor.execute(
                () -> post("{\"Date\":\"2026-09-20\",\"TaxEventDate\":\"2026-09-29\",\"IssueDelayDays\":0}", LATE_ISSUE).statusCode(400),
                TIMEOUT_SECONDS);
        assertCount(2);
    }

    private io.restassured.response.ValidatableResponse post(String body, String confirm) {
        return request(confirm).body(body)
                               .when()
                               .post(INVOICES)
                               .then();
    }

    private io.restassured.response.ValidatableResponse put(int id, String body, String confirm) {
        return request(confirm).body(body)
                               .when()
                               .put(INVOICES + "/" + id)
                               .then();
    }

    private void warned(io.restassured.response.ValidatableResponse response) {
        response.statusCode(428)
                .body("errorType", equalTo("ConfirmationRequired"))
                .body("warnings", hasSize(1))
                .body("warnings[0].code", equalTo(LATE_ISSUE));
    }

    private int create(String body, String confirm) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(post(body, confirm).statusCode(200)
                                                                    .extract()
                                                                    .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void assertStoredDelay(int id, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + id)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("IssueDelayDays", equalTo(expected)),
                TIMEOUT_SECONDS);
    }

    private void assertCount(int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(expected)),
                TIMEOUT_SECONDS);
    }

    private static RequestSpecification request(String confirm) {
        RequestSpecification request = given().contentType("application/json");
        return confirm == null ? request : request.header(CONFIRM, confirm);
    }

    private void generateProject() {
        writeIntent();
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
