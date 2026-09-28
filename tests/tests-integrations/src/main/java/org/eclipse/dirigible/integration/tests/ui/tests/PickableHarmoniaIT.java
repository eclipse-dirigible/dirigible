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
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * The browser half of a to-one relation's {@code pickable:} rule (issue #7496): a target row
 * failing the rule is offered disabled with the rule's message under its name ({@code else: mark}),
 * or not offered at all ({@code else: hide}) - while a value the record already holds keeps its
 * label.
 *
 * <p>
 * This needs a real browser because the rule is evaluated by the shared runtime over the rows the
 * picker fetched, and the marking is Harmonia's own option contract ({@code aria-disabled},
 * {@code data-description}) - the generated source is byte-correct whether or not either of them
 * takes effect. Every surface that builds its options differently is visited: the manage form, the
 * document header, and the document's line dialog.
 */
class PickableHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "pickable-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/pickables/index.html#";

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    /**
     * The adopter's shape: a customer is complete only once it carries a registration number. Invoice
     * is a document (it owns an {@code *Item} child), so its header and its line dialog are the two
     * document surfaces; Contract is a plain entity, rendered by the manage form. Invoice 1 already
     * references the incomplete customer through the hiding relation.
     */
    private static final String INTENT_YAML =
            """
                    name: pickables
                    entities:
                      - name: Customer
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 60 }
                          - { name: registrationNumber, type: string, length: 20 }
                      - name: Product
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 60 }
                          - { name: active, type: boolean }
                      - name: Contract
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: title, type: string, length: 60 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [registrationNumber != null], message: Registration number missing } }
                      - name: Invoice
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: title, type: string, length: 60 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer, pickable: { when: [registrationNumber != null], message: Registration number missing } }
                          - { name: Referrer, kind: manyToOne, to: Customer, pickable: { when: [registrationNumber != null], else: hide } }
                      - name: InvoiceItem
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: quantity, type: integer }
                        relations:
                          - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                          - { name: Product, kind: manyToOne, to: Product, pickable: { when: [active == true], message: Discontinued } }
                    seeds:
                      - name: customers
                        entity: Customer
                        rows:
                          - { id: 1, name: Complete Ltd, registrationNumber: BG123 }
                          - { id: 2, name: Incomplete Ltd }
                      - name: products
                        entity: Product
                        rows:
                          - { id: 1, name: Widget, active: true }
                          - { id: 2, name: Gadget, active: false }
                      - name: invoices
                        entity: Invoice
                        rows:
                          - { id: 1, title: INV-1, Referrer: 2 }
                    """;

    /**
     * The state of the option labelled {@code arguments[1]} in the picker whose bound input has the id
     * {@code arguments[0]} - or, with no id, anywhere on the page (the line dialog's inputs carry
     * none): {@code absent}, {@code enabled}, or {@code disabled:<description>}. Harmonia gives an
     * option its label through {@code aria-labelledby}, so the label is read from there, apart from the
     * description line it appends under it.
     */
    private static final String OPTION_STATE = """
            var inputId = arguments[0], wanted = arguments[1];
            var root = inputId ? document.getElementById(inputId) : document;
            if (!root) return 'no-picker';
            if (inputId) root = root.parentElement;
            var option = Array.from(root.querySelectorAll('[role=option]')).find(function (o) {
              var label = document.getElementById(o.getAttribute('aria-labelledby') || '');
              return label && label.textContent.trim() === wanted;
            });
            if (!option) return 'absent';
            return option.getAttribute('aria-disabled') === 'true'
                ? 'disabled:' + (option.getAttribute('data-description') || '') : 'enabled';
            """;

    /** The label the trigger of the picker bound to input {@code arguments[0]} shows. */
    private static final String TRIGGER_LABEL = """
            var input = document.getElementById(arguments[0]);
            var trigger = input ? input.parentElement.querySelector('button[role=combobox]') : null;
            return trigger ? trigger.textContent.trim() : '';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_row_failing_the_rule_is_marked_or_hidden_and_a_held_value_keeps_its_label() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // MANAGE form, else: mark - the incomplete customer is offered, disabled, saying why.
        browser.openPath(APP + "/Contract/create");
        assertEquals("enabled", optionState("f_Customer", "Complete Ltd", "enabled"::equals));
        assertEquals("disabled:Registration number missing",
                optionState("f_Customer", "Incomplete Ltd", "disabled:Registration number missing"::equals));

        // DOCUMENT header: the marking relation marks, the hiding one leaves the row out.
        browser.openPath(APP + "/Invoice/create");
        assertEquals("disabled:Registration number missing",
                optionState("f_Customer", "Incomplete Ltd", "disabled:Registration number missing"::equals));
        assertEquals("enabled", optionState("f_Referrer", "Complete Ltd", "enabled"::equals));
        assertEquals("absent", optionState("f_Referrer", "Incomplete Ltd", "absent"::equals),
                "else: hide must leave a row failing the rule out of the picker");

        // A record that already holds a now-unpickable value: the hidden row comes back for it -
        // disabled, with the rule as its message - so the field still shows what is stored.
        browser.openPath(APP + "/Invoice/1/edit");
        assertEquals("disabled:registrationNumber != null",
                optionState("f_Referrer", "Incomplete Ltd", "disabled:registrationNumber != null"::equals),
                "a held value must stay listed even under else: hide, with the rule as its default message");
        assertEquals("Incomplete Ltd", poll(TRIGGER_LABEL, "Incomplete Ltd"::equals, "f_Referrer"),
                "the held value's label must still resolve");

        // DOCUMENT line dialog: its options are built from the column metadata the register carries.
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Add");
        assertEquals("enabled", optionState(null, "Widget", "enabled"::equals));
        assertEquals("disabled:Discontinued", optionState(null, "Gadget", "disabled:Discontinued"::equals));
    }

    private String optionState(String inputId, String label, Predicate<String> settled) {
        return poll(OPTION_STATE, settled, inputId, label);
    }

    private String poll(String script, Predicate<String> settled, Object... arguments) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script, arguments);
            if (value != null && settled.test(value)) {
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
