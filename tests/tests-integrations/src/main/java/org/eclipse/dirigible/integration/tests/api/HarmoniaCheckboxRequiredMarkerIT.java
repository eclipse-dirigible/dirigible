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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * A required boolean renders without the required {@code *} marker (dirigible #8563).
 *
 * <p>
 * A NOT NULL boolean is always satisfied by a checkbox - the form initialises it to {@code false}
 * and an unticked box is a valid answer - so the marker only told the user the box had to be
 * ticked. The generated form, the document header and the document line dialog leave it off a
 * checkbox, and keep it on every other required widget. Asserted on the views the generation
 * actually wrote and the published app serves, and on the running controller accepting the unticked
 * value.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HarmoniaCheckboxRequiredMarkerIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "checkboxmark";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final String TICKET_FORM = "gen/" + PROJECT + "/views/Ticket/Ticket-form.html";
    private static final String ORDER_DOCUMENT = "gen/" + PROJECT + "/views/SalesOrder/SalesOrder-document.html";
    private static final String MARKER = "<span class=\"text-negative\">*</span>";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: checkboxmark
            description: a required boolean carries no required marker

            entities:
              - name: Ticket
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: subject,  type: string, required: true, length: 100 }
                  - { name: internal, type: boolean, required: true }
              - name: SalesOrder
                function: Document
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: note,  type: string, required: true, length: 200 }
                  - { name: rush,  type: boolean, required: true }
              - name: SalesOrderLine
                function: DocumentItem
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: text,    type: string, required: true, length: 200 }
                  - { name: gift,    type: boolean, required: true }
                relations:
                  - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_required_checkbox_carries_no_required_marker_and_saves_unticked() {
        generateProject();

        String form = contentOf(TICKET_FORM);
        assertFalse(label(form, "Internal").contains(MARKER), "a required checkbox must carry no required marker on the form");
        assertTrue(label(form, "Subject").contains(MARKER), "a required text field keeps its required marker on the form");

        String document = contentOf(ORDER_DOCUMENT);
        assertFalse(label(document, "Rush").contains(MARKER), "a required checkbox must carry no required marker on the document header");
        assertTrue(label(document, "Note").contains(MARKER), "a required text field keeps its required marker on the document header");
        assertTrue(document.contains("x-show=\"col.required &amp;&amp; !col.readonly &amp;&amp; col.widget !== 'CHECKBOX'\""),
                "the document line dialog must leave the required marker off a checkbox column");

        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The published app serves the form without the marker on its checkbox.
        AtomicReference<String> served = new AtomicReference<>();
        restAssuredExecutor.execute(() -> served.set(given().when()
                                                            .get("/services/web/" + PROJECT + "/" + TICKET_FORM)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .asString()),
                TIMEOUT_SECONDS);
        assertFalse(label(served.get(), "Internal").contains(MARKER), "the served form must carry no required marker on its checkbox");

        // Unticked is a valid answer: the running controller accepts and keeps false.
        int ticket = create("{\"Subject\":\"printer\",\"Internal\":false}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TICKETS + "/" + ticket)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Internal", equalTo(false)),
                TIMEOUT_SECONDS);
    }

    /** The label element bound to the field {@code f_<property>}. */
    private static String label(String view, String property) {
        Matcher label = Pattern.compile("<label x-h-label for=\"f_" + property + "\">.*?</label>", Pattern.DOTALL)
                               .matcher(view);
        assertTrue(label.find(), "no label for f_" + property + " in the generated view");
        return label.group();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(TICKETS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private void writeResource(String path, String content) {
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
