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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

import io.restassured.response.ValidatableResponse;

/**
 * The checks of a generated create judge the row as it will be stored - on the power, the personal
 * and the partner surface.
 *
 * <p>
 * Two orderings in the generated controllers let a check judge something else:
 * <ul>
 * <li>The row checks ran before the field checks (dirigible #8909), so a required relation left
 * empty was answered by a {@code forbidWhen} reading through it - "This store is not active", no
 * field marked - instead of the required-field refusal naming the relation.</li>
 * <li>The create defaults were filled only by the repository's save, after the checks (dirigible
 * #8910), so an {@code agree} over a relation left to its {@code init:} skipped the null it saw and
 * the row was stored with the very ledger it forbids; a {@code compare} over a defaulted field
 * likewise passed a value the row then held.</li>
 * </ul>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentChecksReadStoredRowIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "storedrow";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String RECEIPTS = API + "/receipt/ReceiptController";
    private static final String MY_RECEIPTS = API + "/receipt/ReceiptMyController";
    private static final String DELIVERIES = API + "/delivery/DeliveryController";
    private static final String PARTNER_DELIVERIES = API + "/delivery/DeliveryPartnerController";
    private static final String CATALOG = PROJECT + ":" + PROJECT + "-model.checks.";
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * Store 1 and ledger 1 belong to company 1, store 2 and ledger 2 (the base one) to company 2; store
     * 3 (company 1) is not active.
     */
    private static final String INTENT_YAML =
            """
                    name: storedrow
                    description: the checks read the row as it will be stored
                    languages: [en, bg]

                    entities:
                      - name: Employee
                        identity: email
                        fields:
                          - { name: id,    type: integer, primaryKey: true, generated: true }
                          - { name: name,  type: string, required: true, length: 100 }
                          - { name: email, type: string, required: true, length: 100 }

                      - name: Company
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 100 }

                      - name: Ledger
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 100 }
                          - { name: base, type: boolean, single: true }
                        relations:
                          - { name: Company, kind: manyToOne, to: Company, required: true }

                      - name: Store
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: name,   type: string, required: true, length: 100 }
                          - { name: active, type: integer }
                        relations:
                          - { name: Company, kind: manyToOne, to: Company, required: true }

                      # The power and the personal surface; a literal init: and a defaulted scalar.
                      - name: Receipt
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: quantity, type: integer, defaultValue: 0 }
                        relations:
                          - { name: Store,    kind: manyToOne, to: Store, required: true }
                          - { name: Ledger,   kind: manyToOne, to: Ledger, init: 1 }
                          - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                        checks:
                          - { kind: forbidWhen, id: activeStore, when: "Store.active != 1", verbs: [create, update], message: "This store is not active" }
                          - { kind: agree, id: storeCompany, relations: [Store, Ledger], onProperty: Company,
                              message: "The store belongs to another company than the ledger" }
                          - { kind: compare, id: positiveQuantity, field: quantity, op: gt, value: 0, message: "The quantity is greater than zero" }

                      # The partner surface; an init: naming the company that holds a flag.
                      - name: Delivery
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                        relations:
                          - { name: Store,    kind: manyToOne, to: Store, required: true }
                          - { name: Ledger,   kind: manyToOne, to: Ledger, init: { flag: base } }
                          - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }
                        checks:
                          - { kind: forbidWhen, id: activeStore, when: "Store.active != 1", verbs: [create, update], message: "This store is not active" }
                          - { kind: agree, id: storeCompany, relations: [Store, Ledger], onProperty: Company,
                              message: "The store belongs to another company than the ledger" }

                    seeds:
                      - name: employees
                        entity: Employee
                        rows:
                          - { id: 1, name: Admin, email: admin }
                      - name: companies
                        entity: Company
                        rows:
                          - { id: 1, name: Co-A }
                          - { id: 2, name: Co-B }
                      - name: ledgers
                        entity: Ledger
                        rows:
                          - { id: 1, name: Ledger A, base: false, Company: 1 }
                          - { id: 2, name: Ledger B, base: true, Company: 2 }
                      - name: stores
                        entity: Store
                        rows:
                          - { id: 1, name: WH-A, active: 1, Company: 1 }
                          - { id: 2, name: WH-B, active: 1, Company: 2 }
                          - { id: 3, name: WH-X, active: 0, Company: 1 }
                    """;

    /** A translator's overlay: the two sentences the test reads in bg, and the relations' labels. */
    private static final String BG_OVERLAY = """
            {
              "storedrow-model": {
                "checks": {
                  "fieldRequired": "Полето „{label}“ е задължително",
                  "Receipt_storeCompany": "Обектът е на друга фирма, а не на книгата"
                },
                "t": {
                  "RECEIPT_STORE": "Обект",
                  "DELIVERY_STORE": "Обект"
                }
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
    void a_create_is_checked_as_it_will_be_stored() {
        generateProject();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/" + PROJECT + ".model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // #8909: a required relation left empty is the required-field refusal naming it, not the
        // forbidWhen that reads through it - in every language, on every surface.
        refused("POST", RECEIPTS, "{\"Quantity\":1,\"Employee\":1}", "en").body("field", equalTo("Store"))
                                                                          .body("messageKey", equalTo(CATALOG + "fieldRequired"))
                                                                          .body("message", equalTo("The 'Store' property is required"));
        refused("POST", RECEIPTS, "{\"Quantity\":1,\"Employee\":1}", "bg").body("field", equalTo("Store"))
                                                                          .body("message", equalTo("Полето „Обект“ е задължително"));
        refused("POST", MY_RECEIPTS, "{\"Quantity\":1}", "en").body("field", equalTo("Store"))
                                                              .body("messageKey", equalTo(CATALOG + "fieldRequired"));
        refused("POST", DELIVERIES, "{\"Employee\":1}", "en").body("field", equalTo("Store"))
                                                             .body("messageKey", equalTo(CATALOG + "fieldRequired"));
        refused("POST", PARTNER_DELIVERIES, "{}", "bg").body("field", equalTo("Store"))
                                                       .body("message", equalTo("Полето „Обект“ е задължително"));
        // A store that is there but inactive is still the check's own refusal.
        refused("POST", RECEIPTS, "{\"Store\":3,\"Quantity\":1,\"Employee\":1}", "en").body("field", nullValue())
                                                                                      .body("messageKey", endsWith(".Receipt_activeStore"));
        refused("POST", PARTNER_DELIVERIES, "{\"Store\":3}", "en").body("messageKey", endsWith(".Delivery_activeStore"));

        // #8910: the agree judges the ledger the row will be stored with - init: 1 (company 1) against
        // store 2 of company 2 is refused as if ledger 1 had been sent, on the personal surface too.
        refused("POST", RECEIPTS, "{\"Store\":2,\"Quantity\":1,\"Employee\":1}",
                "en").body("messageKey", equalTo(CATALOG + "Receipt_storeCompany"))
                     .body("message", equalTo("The store belongs to another company than the ledger"));
        refused("POST", RECEIPTS, "{\"Store\":2,\"Quantity\":1,\"Employee\":1}", "bg").body("message",
                equalTo("Обектът е на друга фирма, а не на книгата"));
        refused("POST", MY_RECEIPTS, "{\"Store\":2,\"Quantity\":1}", "en").body("messageKey", equalTo(CATALOG + "Receipt_storeCompany"));
        // A store of the default ledger's company is stored with that default.
        created(RECEIPTS, "{\"Store\":1,\"Quantity\":1,\"Employee\":1}").body("Ledger", equalTo(1));
        created(MY_RECEIPTS, "{\"Store\":1,\"Quantity\":1}").body("Ledger", equalTo(1));
        // A defaulted scalar is compared as stored: the default 0 is not greater than zero.
        refused("POST", RECEIPTS, "{\"Store\":1,\"Employee\":1}", "en").body("messageKey", equalTo(CATALOG + "Receipt_positiveQuantity"));

        // init: { flag: base } is ledger 2 (company 2): a store of company 1 is refused, one of company 2
        // is stored with it - through the power and the partner surface.
        refused("POST", DELIVERIES, "{\"Store\":1,\"Employee\":1}", "en").body("messageKey", equalTo(CATALOG + "Delivery_storeCompany"));
        refused("POST", PARTNER_DELIVERIES, "{\"Store\":1}", "en").body("messageKey", equalTo(CATALOG + "Delivery_storeCompany"));
        created(DELIVERIES, "{\"Store\":2,\"Employee\":1}").body("Ledger", equalTo(2));
        created(PARTNER_DELIVERIES, "{\"Store\":2}").body("Ledger", equalTo(2));
    }

    private ValidatableResponse refused(String method, String controller, String body, String language) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .header("Accept-Language", language)
                                                              .body(body)
                                                              .when()
                                                              .request(method, controller)
                                                              .then()
                                                              .statusCode(400)),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse created(String controller, String body) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .body(body)
                                                              .when()
                                                              .post(controller)
                                                              .then()
                                                              .statusCode(200)),
                TIMEOUT_SECONDS);
        return response.get();
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
