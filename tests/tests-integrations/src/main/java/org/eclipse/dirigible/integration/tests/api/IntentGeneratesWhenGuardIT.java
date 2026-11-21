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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.JsonPath;

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

import com.google.gson.Gson;

/**
 * A create-from guarded by the source's own values (issue #7992): {@code when:} on a
 * {@code generates:} entry refuses the click with 409 and the authored {@code whenMessage} while
 * the condition does not hold on the source - a paused invoice template no longer answers "Create
 * now" with a new invoice - creates as before once it holds, and combines with {@code fromStatus}
 * (both must admit the click). The button descriptor the record page reads carries the same
 * condition, so the shell's customActions store stops offering the action on a record it would
 * refuse.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGeneratesWhenGuardIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "whenguard";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TEMPLATES = API + "/invoicetemplate/InvoiceTemplateController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String GENERATE = EVENTS + "/InvoiceFromTemplateGenerate/run";
    private static final String APPROVE = EVENTS + "/ApproveTemplateTransition/run";
    private static final String DESCRIPTOR = "invoice-from-template-generate-action.json";
    private static final String MESSAGE = "Activate the template first";
    private static final long TIMEOUT_SECONDS = 90;

    private static final int APPROVED = 2;

    private static final String INTENT_YAML = """
            name: whenguard
            description: a create-from guarded by its source's own values

            entities:
              - name: TemplateStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }

              - name: InvoiceTemplate
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 40 }
                  - { name: active, type: boolean }
                relations:
                  - { name: Status, kind: manyToOne, to: TemplateStatus, function: EntityStatus, init: 1 }

              - name: Invoice
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 40 }
                relations:
                  - { name: Template, kind: manyToOne, to: InvoiceTemplate }

            generates:
              - name: invoice-from-template
                from: InvoiceTemplate
                to: Invoice
                label: Create now
                map: { Template: id, note: name }
                fromStatus: [APPROVED]
                when: "active == true"
                whenMessage: "Activate the template first"

            transitions:
              - { name: ApproveTemplate, forEntity: InvoiceTemplate, from: [1], setStatus: 2, label: Approve }

            seeds:
              - name: template-statuses
                entity: TemplateStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void an_inactive_template_refuses_create_now_and_an_active_one_creates() {
        deploy();

        int paused = approve(create("{\"Name\":\"paused\",\"Active\":false}"));
        int running = approve(create("{\"Name\":\"running\",\"Active\":true}"));
        int draft = create("{\"Name\":\"draft\",\"Active\":true}");

        // The condition does not hold: 409 with the authored message, and nothing created.
        refused(paused, MESSAGE);
        assertEquals(0, countInvoices(), "a refused create-from must create nothing");

        // It combines with fromStatus: an active template still has to stand in an allowed status.
        refused(draft, "allowed only from status [" + APPROVED + "]");
        assertEquals(0, countInvoices());

        // Both admit the click: the invoice is created from the template.
        JsonPath created = generate(running);
        assertEquals(running, created.getInt("Template"));
        assertEquals("running", created.getString("Note"));
        assertEquals(1, countInvoices());

        // Reactivating the paused template admits it from then on.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + paused + ",\"Name\":\"paused\",\"Active\":true,\"Status\":" + APPROVED
                                                         + "}")
                                                 .when()
                                                 .put(TEMPLATES + "/" + paused)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        assertEquals(paused, generate(paused).getInt("Template"));
        assertEquals(2, countInvoices());
    }

    @Test
    void the_button_descriptor_carries_the_condition_the_page_hides_the_action_by() {
        deploy();

        IResource descriptor = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + DESCRIPTOR);
        assertTrue(descriptor.exists(), "the create-from's button descriptor is published");
        Map<?, ?> view = new Gson().fromJson(new String(descriptor.getContent(), StandardCharsets.UTF_8), Map.class);
        // The shell's customActions.isAvailable compares each term as text against the record it holds:
        // a template whose Active reads "false" is not offered the action.
        assertEquals(List.of(Map.of("property", "Active", "equal", true, "value", "true")), view.get("when"));
        // ...beside the status guard it already carried.
        assertEquals("Status", ((Map<?, ?>) view.get("guard")).get("property"));
    }

    private JsonPath generate(int template) {
        AtomicReference<String> body = new AtomicReference<>();
        restAssuredExecutor.execute(() -> body.set(given().contentType("application/json")
                                                          .body("{\"id\":" + template + "}")
                                                          .when()
                                                          .post(GENERATE)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .asString()),
                TIMEOUT_SECONDS);
        return JsonPath.from(body.get());
    }

    private int approve(int template) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + template + "}")
                                                 .when()
                                                 .post(APPROVE)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        return template;
    }

    private void refused(int template, String message) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + template + "}")
                                                 .when()
                                                 .post(GENERATE)
                                                 .then()
                                                 .statusCode(409)
                                                 // served as text/plain, so matched on the raw body
                                                 .body(containsString(message)),
                TIMEOUT_SECONDS);
    }

    private void deploy() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(TEMPLATES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private int countInvoices() {
        AtomicInteger count = new AtomicInteger();
        restAssuredExecutor.execute(() -> count.set(given().when()
                                                           .get(INVOICES + "/count")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt("count")));
        return count.get();
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
