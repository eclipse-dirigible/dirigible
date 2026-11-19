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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * {@code systemProperties: { processId: { visibleTo: [...] } }} (dirigible #8692): the generated
 * pages show the {@code ProcessId} / {@code ProcessIds} pair only to the listed roles, while the
 * record's REST response keeps serving the value - the page correlates the record's process tasks
 * on it.
 *
 * <p>
 * The ITs run as a user local basic auth answers {@code isInRole(true)} for, so a caller OUTSIDE
 * the roles cannot be provoked over HTTP here (the same limit {@code IntentEmissionCoverageIT}
 * states for {@code visibleTo:}); that branch of the generated controller is covered by
 * {@code RoleScopedFieldControllerTemplateIT#aDisplayOnlyRoleIsListedAsWithheldButNeverStripped}.
 * What this proves at runtime: the generated module compiles and runs with the setting, the
 * trigger's stamped {@code ProcessId} is served, the {@code /restricted} endpoint the page asks
 * exists and withholds nothing from a caller inside the roles, and the generated page gates the
 * rail rows on it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProcessIdDisplayIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "processiddisplay";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String RECEIPTS = "/services/java/" + PROJECT + "/gen/stock/api/receipt/ReceiptController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: stock
            description: a posted receipt whose process id only support sees on the page
            systemProperties:
              processId: { visibleTo: [Support] }
            # The role only has to exist; it gates another entity, so the receipt stays open to the test's admin.
            permissions:
              - { role: Support, description: Support staff, can: [Warehouse:read] }
            entities:
              - name: Warehouse
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
              - name: Receipt
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
            processes:
              - name: PostReceipt
                trigger: { onCreate: Receipt }
                steps:
                  - { name: markPosted, kind: serviceTask, args: { setField: note, value: posted, next: done } }
                  - { name: done,       kind: end }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_page_withholds_the_process_id_and_the_response_keeps_it() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int receipt = create("{\"Note\":\"new\"}");

        // The trigger stamps the started instance back, and the response serves it to a caller outside
        // the roles - the page's task correlation reads it from there.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ProcessId", notNullValue()),
                TIMEOUT_SECONDS);

        // The page asks which fields to leave out; a caller inside the roles is withheld nothing.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/restricted")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("restricted", empty()),
                TIMEOUT_SECONDS);
        String controller = contentOf("gen/stock/api/receipt/ReceiptController.java");
        assertTrue(
                controller.contains("if (!isInAnyRole(\"Support\")) {") && controller.contains("restricted.add(\"ProcessId\");")
                        && controller.contains("restricted.add(\"ProcessIds\");"),
                "a caller outside the roles must be told both are withheld from the page: " + controller);

        // The generated form asks the controller, and gates the Details rail rows by its answer.
        String form = contentOf("gen/stock/views/Receipt/Receipt-form.html");
        assertTrue(form.contains("canSee('ProcessId')"), "the Details rail must gate ProcessId by the caller: " + form);
        String page = contentOf("gen/stock/js/components/pages/Receipt/ReceiptFormPage.js");
        assertTrue(page.contains("this.loadRestrictedFields()"), "the page must ask which fields it withholds");
        assertTrue(page.contains("this.canSee('ProcessId') ? this.form.ProcessId : null"),
                "a withheld ProcessId must not make the Details rail show with nothing in it");
        assertTrue(page.contains("this.form.ProcessId"), "the page still reads the value for its task correlation");
        assertFalse(controller.contains("redactRead"), "the controller must not strip the value from its responses");
    }

    private String contentOf(String path) {
        IResource resource = repository.getResource(PROJECT_PATH + "/" + path);
        assertTrue(resource.exists(), "generated file missing: " + path);
        return new String(resource.getContent(), StandardCharsets.UTF_8);
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(RECEIPTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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
