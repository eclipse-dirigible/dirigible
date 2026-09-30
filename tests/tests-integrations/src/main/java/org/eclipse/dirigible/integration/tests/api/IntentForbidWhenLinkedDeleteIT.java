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
 * A row can be refused DELETE because of what it is linked to (dirigible #7555).
 *
 * <p>
 * A payroll entry swept into a payslip must stay in the register - deleting it loses it from the
 * audit trail - but no construct could say so: a {@code forbidWhen} could not test whether a
 * relation is set, the null literal being refused. {@code forbidWhen: "Slip != null"} now says
 * exactly that, and since every forbidWhen reaches the delete verb (#7372), the linked entry's
 * DELETE is refused with the authored message while an unlinked one deletes as before. The link
 * itself is a system write - the payroll run's delegate - so it is made here below the controller,
 * which refuses it too.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentForbidWhenLinkedDeleteIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "payguard";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String SLIPS = API + "/slip/SlipController";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final String REFUSAL = "An entry swept into a payslip is part of it";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: payguard
            description: a payroll entry swept into a payslip cannot be deleted

            entities:
              - name: Slip
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string, length: 100 }

              - name: Entry
                checks:
                  - { kind: forbidWhen, when: "Slip != null", message: "%s" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Slip, kind: manyToOne, to: Slip }
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
    void a_linked_row_is_refused_delete_and_an_unlinked_one_is_not() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int slip = create(SLIPS, "{\"Label\":\"September\"}");
        int swept = create(ENTRIES, "{\"Note\":\"bonus\"}");
        int loose = create(ENTRIES, "{\"Note\":\"allowance\"}");
        link(swept, slip);

        // The swept entry's delete is refused with the authored message, and the row stays...
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ENTRIES + "/" + swept)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + swept)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Slip", equalTo(slip)));
        // ...its edit too - the rule refuses every user write while the row is linked...
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + swept + ",\"Note\":\"changed\",\"Slip\":" + slip + "}")
                                                 .when()
                                                 .put(ENTRIES + "/" + swept)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
        // ...while an entry no payslip swept up deletes as before.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ENTRIES + "/" + loose)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + loose)
                                                 .then()
                                                 .statusCode(404));
    }

    /**
     * The link the payroll run's delegate sets - a system write, below the controller that refuses it.
     */
    private void link(int entry, int slip) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update =
                        connection.prepareStatement("UPDATE \"PAYGUARD_ENTRY\" SET \"ENTRY_SLIP\" = ? WHERE \"ENTRY_ID\" = ?")) {
            update.setInt(1, slip);
            update.setInt(2, entry);
            assertEquals(1, update.executeUpdate(), "the entry to link must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to link entry " + entry + " to slip " + slip, ex);
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
