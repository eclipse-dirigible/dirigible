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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
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

/**
 * A {@code forbidWhen} over a {@code readOnly} column judges the STORED value on an update, not the
 * payload's (dirigible #7633).
 *
 * <p>
 * The controller's checks ran against the request body, while the system-owned columns - an
 * author's {@code readOnly}, a roll-up target, the audit columns - are taken from the stored row
 * only inside the repository's {@code update()}. So a rule about the stored state was read from
 * whatever the caller sent: a PUT that omitted the column, or carried a harmless value for it,
 * walked past the refusal while the row said the opposite. The delete half already read the stored
 * row (#7372). The counter here is what a roll-up over the posted entries would maintain; it is set
 * below the controller, which never writes it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentReadOnlyFieldChecksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rcpguard";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String RECEIPTS = API + "/receipt/ReceiptController";
    private static final String REFUSAL = "A posted receipt is final";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: rcpguard
            description: a receipt whose entries are posted cannot be edited, whatever the PUT carries

            entities:
              - name: Receipt
                checks:
                  - { kind: forbidWhen, when: ["postedEntries != null", "postedEntries != 0"], message: "%s" }
                fields:
                  - { name: id,            type: integer, primaryKey: true, generated: true }
                  - { name: amount,        type: decimal, precision: 18, scale: 2 }
                  - { name: postedEntries, type: integer, readOnly: true, defaultValue: 0 }
            """.formatted(REFUSAL);

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void an_update_is_judged_on_the_stored_read_only_column_whatever_the_payload_carries() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int open = create(RECEIPTS, "{\"Amount\":5}");
        int posted = create(RECEIPTS, "{\"Amount\":5}");
        markPosted(posted);

        // The issue's three PUTs: the stored value echoed back, the column omitted, a harmless value.
        // All three are refused, and the row keeps its amount.
        assertRefused(posted, "{\"Id\":" + posted + ",\"Amount\":1,\"PostedEntries\":1}");
        assertRefused(posted, "{\"Id\":" + posted + ",\"Amount\":1}");
        assertRefused(posted, "{\"Id\":" + posted + ",\"Amount\":1,\"PostedEntries\":0}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + posted)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Amount", equalTo(5.0f))
                                                 .body("PostedEntries", equalTo(1)));

        // A receipt nothing has posted is edited as before - including by a payload that claims a
        // posted counter it does not have: the stored zero is what the rule reads, and what is kept.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + open + ",\"Amount\":7.5,\"PostedEntries\":3}")
                                                 .when()
                                                 .put(RECEIPTS + "/" + open)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Amount", equalTo(7.5f))
                                                 .body("PostedEntries", equalTo(0)));
    }

    private void assertRefused(int receipt, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
    }

    /** What the roll-up over the posted entries writes - a system write, below the controller. */
    private void markPosted(int receipt) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE \"RCPGUARD_RECEIPT\" SET \"RECEIPT_POSTED_ENTRIES\" = 1 WHERE \"RECEIPT_ID\" = ?")) {
            update.setInt(1, receipt);
            assertEquals(1, update.executeUpdate(), "the receipt to mark must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to mark receipt " + receipt + " posted", ex);
        }
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
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
