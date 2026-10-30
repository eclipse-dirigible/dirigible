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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8370: a record page is titled with the record's label, captioned with the entity name, and the
 * line under the title is exactly what {@code subtitle:} declares - each value with its field's
 * label as the tooltip. An entity without a label keeps the entity name as its title, with no
 * caption.
 */
class RecordHeaderHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "recordheaderit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/recordheader/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/recordheader/api";

    private static final String INTENT_YAML = """
            name: recordheader
            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
              - name: Order
                label: "{customer.name} / {number}"
                subtitle: [dueDate, customer]
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: number,  type: string, length: 20 }
                  - { name: dueDate, type: date }
                  - { name: notes,   type: text }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
              - name: Memo
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, length: 100 }
            """;

    /**
     * The record page's header as {@code
     *
    <h1>|<caption>|<subtitle text>|<part>@<tooltip>,...}, or {@code pending} until the record has
     * loaded. A hidden caption reads as empty.
     */
    private static final String HEADER = """
            var root = document.querySelector('[x-data="%sFormPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).loadedRecord) return 'pending';
            var caption = root.querySelector('[data-record-caption]');
            var subtitle = root.querySelector('[data-record-subtitle]');
            var shown = function (e) { return e && getComputedStyle(e).display !== 'none'; };
            var parts = shown(subtitle) ? Array.from(subtitle.querySelectorAll('[title]'))
                .map(function (s) { return s.textContent.trim() + '@' + s.getAttribute('title'); }) : [];
            return root.querySelector('h1').textContent.trim() + '|' + (shown(caption) ? caption.textContent.trim() : '')
                + '|' + (shown(subtitle) ? subtitle.textContent.replace(/\\s+/g, ' ').trim() : '') + '|' + parts.join(',');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_record_is_titled_with_its_label_and_its_subtitle_is_the_declared_line() {
        generateAndPublish();
        int customer = create("/customer/CustomerController", Map.of("Name", "Acme Ltd"));
        int order = create("/order/OrderController",
                Map.of("Customer", customer, "Number", "2026-001", "DueDate", "2026-11-30", "Notes", "Deliver to the back door."));
        int memo = create("/memo/MemoController", Map.of("Subject", "Quarterly review"));
        ide.openHomePage();

        browser.openPath(APP + "/Order/" + order + "/preview");
        assertEquals("Acme Ltd / 2026-001|Order|2026-11-30 \u00b7 Acme Ltd|2026-11-30@Due Date,Acme Ltd@Customer",
                pollUntil(HEADER.formatted("Order"), v -> !"pending".equals(v)),
                "the title is the record's label, the caption the entity, the subtitle exactly the declared values with their labels");

        browser.openPath(APP + "/Memo/" + memo + "/preview");
        assertEquals("Memo|||", pollUntil(HEADER.formatted("Memo"), v -> !"pending".equals(v)),
                "an entity without a label keeps its name as the title, with no caption and no subtitle");
    }

    private int create(String controller, Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private String pollUntil(String script, Predicate<String> done) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && done.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
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
