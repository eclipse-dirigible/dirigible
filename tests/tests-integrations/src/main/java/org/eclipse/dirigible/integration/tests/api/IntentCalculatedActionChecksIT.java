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

/**
 * The checks of a create and of an update judge the fields a calculated ACTION derives, as the
 * write will store them (dirigible #8000).
 *
 * <p>
 * The controllers run the checks on {@code calculatedForCreate} / {@code calculatedForUpdate}, the
 * record the write would store. Those evaluated the neutral expressions only, while the
 * {@code calculatedActionOnCreate} / {@code calculatedActionOnUpdate} fields were derived later,
 * inside the repository's write. So a check over an action-derived field read the caller's value -
 * usually nothing: a supplier note whose supplier is read off its reference was never asked about a
 * duplicate supplier number, and a rule that depends on the supplier never fired, although the
 * stored row carried the supplier. A request that omits the derived field is now judged exactly as
 * one that sends it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCalculatedActionChecksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "calcact";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String NOTES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/suppliernote/SupplierNoteController";
    private static final String REASON_REQUIRED = "A note from ACME must give its reason";
    private static final String DUPLICATE = "This supplier already sent a note with this number";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: calcact
            description: a supplier note whose supplier is derived from its reference by a calculated action

            entities:
              - name: SupplierNote
                imports: |
                  import custom.NoteSupplierAction;
                checks:
                  - { kind: requiredWhen, field: reason, when: "supplier == 'ACME'", message: "%s" }
                  - { kind: duplicate, fields: [supplier, supplierNumber], message: "%s" }
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: reference,      type: string, length: 40, required: true }
                  - { name: supplier,       type: string, length: 40,
                      calculatedActionOnCreate: NoteSupplierAction, calculatedActionOnUpdate: NoteSupplierAction }
                  - { name: supplierNumber, type: string, length: 40 }
                  - { name: reason,         type: string, length: 200 }
            """.formatted(REASON_REQUIRED, DUPLICATE);

    /** The supplier is the reference's prefix, unless the caller named one. */
    private static final String ACTION_JAVA = """
            package custom;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.db.CalculatedField;

            import gen.calcact.data.suppliernote.SupplierNoteEntity;

            @Component
            public class NoteSupplierAction implements CalculatedField<SupplierNoteEntity, String> {

                @Override
                public String calculate(SupplierNoteEntity note) {
                    if (note.Supplier != null || note.Reference == null) {
                        return note.Supplier;
                    }
                    int slash = note.Reference.indexOf('/');
                    return slash < 0 ? note.Reference : note.Reference.substring(0, slash);
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void checks_judge_the_fields_a_calculated_action_derives() {
        generateProject();
        writeProjectFile("custom/NoteSupplierAction.java", ACTION_JAVA);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // A rule over the derived supplier: a create that omits it is refused exactly as one that sends it.
        assertCreateRefused("{\"Reference\":\"ACME/1\",\"SupplierNumber\":\"A-1\"}");
        assertCreateRefused("{\"Reference\":\"ACME/1\",\"Supplier\":\"ACME\",\"SupplierNumber\":\"A-1\"}");

        // A duplicate over the derived supplier: the second note of the same supplier and number is asked
        // about, though neither request names the supplier.
        int first = create("{\"Reference\":\"BETA/1\",\"SupplierNumber\":\"B-1\"}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTES + "/" + first)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Supplier", equalTo("BETA")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Reference\":\"BETA/2\",\"SupplierNumber\":\"B-1\"}")
                                                 .when()
                                                 .post(NOTES)
                                                 .then()
                                                 .statusCode(428)
                                                 .body(containsString(DUPLICATE)),
                TIMEOUT_SECONDS);

        // The update path: the update-time action derives the supplier the rule then reads.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + first + ",\"Reference\":\"ACME/9\",\"SupplierNumber\":\"B-1\"}")
                                                 .when()
                                                 .put(NOTES + "/" + first)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REASON_REQUIRED)),
                TIMEOUT_SECONDS);

        // ...and a note that satisfies the rule is stored as before.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Reference\":\"ACME/2\",\"SupplierNumber\":\"A-2\",\"Reason\":\"returned goods\"}")
                                                 .when()
                                                 .post(NOTES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Supplier", equalTo("ACME")),
                TIMEOUT_SECONDS);
    }

    private void assertCreateRefused(String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(NOTES)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REASON_REQUIRED)),
                TIMEOUT_SECONDS);
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(NOTES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        writeProjectFile("app.intent", INTENT_YAML);
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

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
