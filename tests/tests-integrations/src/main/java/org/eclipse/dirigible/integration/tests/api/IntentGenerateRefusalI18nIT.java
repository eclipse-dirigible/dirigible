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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * A create-from's refusals take part in the module's i18n - dirigible #8646.
 *
 * <p>
 * A generated create-from refused a run with fixed English sentences, whatever the request's
 * language. This generates a module whose create-froms refuse a source with no qualifying line, a
 * line their rule refuses (with the author's {@code refuse:} sentence) and a missing required
 * input, translates them the way a label is translated - a hand-written bg-BG overlay under the
 * keys the generated en-US catalog writes - and asserts on the running, generated code: each
 * refusal is answered in the request's {@code Accept-Language} naming the entities and the action
 * by their translated labels, the body carries the key, and the missing input names its field.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGenerateRefusalI18nIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "genref";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String STAYS = API + "/stay/StayController";
    private static final String NIGHTS = API + "/stay/StayNightController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String CATALOG = PROJECT + ":" + PROJECT + "-model.checks.";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: genref
            description: create-from refusals per language
            languages: [en, bg]

            entities:
              - name: Stay
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: guest, type: string, length: 100 }
              - name: StayNight
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Stay, kind: manyToOne, to: Stay, composition: true, required: true }
              - name: Bill
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
              - name: BillLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }

            generates:
              - name: bill-from-stay
                from: Stay
                to: Bill
                label: Bill the stay
                items:
                  from: StayNight
                  to: BillLine
                  where:
                    - { field: amount, op: gt, value: 0 }
                  map:
                    Amount: amount
              - name: checked-bill
                from: Stay
                to: Bill
                label: Bill every night
                items:
                  from: StayNight
                  to: BillLine
                  where:
                    - { field: amount, op: gt, value: 0 }
                  refuse: A night carries no amount
                  map:
                    Amount: amount
              - name: noted-bill
                from: Stay
                to: Bill
                label: Bill with a note
                prompt:
                  - { field: note, required: true }
            """;

    /**
     * A translator's overlay: the sentences, the entities' and the actions' labels, and the input's.
     */
    private static final String BG_OVERLAY =
            """
                    {
                      "genref-model": {
                        "checks": {
                          "generateNoItemQualifies": "Нито една „{item}“ на „{entity}“ не отговаря за „{action}“ - „{target}“ би останала без редове",
                          "checkedBillItemsRefused": "Нощувка без сума",
                          "fieldRequired": "Полето '{label}' е задължително"
                        },
                        "t": {
                          "GENREF_STAY": "Престой",
                          "GENREF_STAY_NIGHT": "Нощувка",
                          "GENREF_BILL": "Сметка",
                          "BILL_NOTE": "Бележка"
                        },
                        "actions": { "bill-from-stay": "Таксувай престоя" }
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
    void create_from_refusals_are_answered_in_the_request_language() {
        generateProject();
        assertGeneratedCatalog();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/genref.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int stay = create(STAYS, "{\"Guest\":\"Ada\"}");
        int night = create(NIGHTS, "{\"Stay\":" + stay + ",\"Amount\":0}");

        // No line qualifies: the sentence and every label in the reader's language, the key beside it.
        run("BillFromStayGenerate", "{\"id\":" + stay + "}", "bg").body("message",
                equalTo("Нито една „Нощувка“ на „Престой“ не отговаря за „Таксувай престоя“ - „Сметка“ би останала без редове"))
                                                                  .body("messageKey", equalTo(CATALOG + "generateNoItemQualifies"));
        run("BillFromStayGenerate", "{\"id\":" + stay + "}", "en").body("message",
                equalTo("no Stay Night row of the Stay qualifies for Bill the stay, so the Bill would have no lines"));

        // The author's refuse: sentence, translated under its own key, followed by the refused rows.
        run("CheckedBillGenerate", "{\"id\":" + stay + "}", "bg").body("message", equalTo("Нощувка без сума (Нощувка [" + night + "])"))
                                                                 .body("messageKey", equalTo(CATALOG + "checkedBillItemsRefused"));
        run("CheckedBillGenerate", "{\"id\":" + stay + "}", "en").body("message",
                equalTo("A night carries no amount (Stay Night [" + night + "])"));

        // A missing required input is a field refusal: translated, and naming the input it is about.
        run("NotedBillGenerate", "{\"id\":" + stay + "}", "bg").body("message", equalTo("Полето 'Бележка' е задължително"))
                                                               .body("messageKey", equalTo(CATALOG + "fieldRequired"))
                                                               .body("field", equalTo("Note"));
        run("NotedBillGenerate", "{\"id\":" + stay + "}", "en").body("message", equalTo("The 'Note' property is required"))
                                                               .body("field", equalTo("Note"));
        // A language the module does not translate falls back to the default text, the key unchanged.
        run("CheckedBillGenerate", "{\"id\":" + stay + "}", "de")
                                                                 .body("message",
                                                                         equalTo("A night carries no amount (Stay Night [" + night + "])"))
                                                                 .body("field", nullValue());
    }

    /**
     * The generated en-US catalog carries the create-from sentences in the default language - the
     * shared ones and the author's refuse: sentence - under the keys the generated run resolves.
     */
    private void assertGeneratedCatalog() {
        IResource catalog = repository.getResource(PROJECT_PATH + "/i18n/en-US/genref.model.json");
        assertTrue(catalog.exists(), "the en-US catalog of the model was not generated");
        JsonObject checks = JsonParser.parseString(new String(catalog.getContent(), StandardCharsets.UTF_8))
                                      .getAsJsonObject()
                                      .getAsJsonObject("genref-model")
                                      .getAsJsonObject("checks");
        assertEquals("no {item} row of the {entity} qualifies for {action}, so the {target} would have no lines",
                checks.get("generateNoItemQualifies")
                      .getAsString());
        assertEquals("A night carries no amount", checks.get("checkedBillItemsRefused")
                                                        .getAsString());
    }

    private ValidatableResponse run(String generate, String body, String language) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(language).body(body)
                                                                        .when()
                                                                        .post(EVENTS + "/" + generate + "/run")
                                                                        .then()
                                                                        .statusCode(400)),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(request("en").body(body)
                                                              .when()
                                                              .post(controller)
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private static RequestSpecification request(String language) {
        return given().contentType("application/json")
                      .header("Accept-Language", language);
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
