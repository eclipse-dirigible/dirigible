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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * The browser half of the context-locked parent control (issue #6551): a child worked on from
 * inside its parent must show that parent as a read-only label, never as a dropdown that could
 * re-point the record mid-flow, while the child's own top-level create keeps free selection.
 *
 * <p>
 * This needs a real browser: which of the two branches renders is an Alpine decision taken at
 * runtime from the URL, so the generated markup (asserted by {@code IntentEngineIT}) proves only
 * that both branches exist. It also covers the query the shared detail panel builds - a master's
 * panel that stopped naming the FK would leave the form unlocked with no other symptom.
 */
class ContextLockedParentHarmoniaIT extends UserInterfaceIntegrationTest {

    /**
     * The budget every poll in this class gets, replacing a fixed 40 x 250 ms = 10 s. 60 s is the cap
     * the framework's own cross-frame element search uses, and it is a budget rather than a fixed
     * attempt count so a loaded runner cannot shorten the wait (issue #7282).
     */
    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "context-locked-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/invoices/index.html#";

    /**
     * A master with a composition child that is NOT a document line (the name must not end in "Item",
     * which switches the master to the document layout and its own item dialog) - so the child gets the
     * routed manage form the detail panel opens.
     */
    private static final String INTENT_YAML = """
            name: invoices
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: SalesInvoiceNote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: comment, type: string }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
            seeds:
              - name: invoices
                entity: SalesInvoice
                rows:
                  - { id: 1, name: INV-001 }
              - name: notes
                entity: SalesInvoiceNote
                rows:
                  - { id: 1, comment: First note, SalesInvoice: 1 }
            """;

    /**
     * The child form's readiness, independent of which parent-control branch renders. The field row's
     * label is emitted by both branches and is the anchor; its parent is the row's {@code x-h-field}
     * wrapper, which Harmonia stamps with {@code data-slot="field"} as it processes it. That stamp is
     * therefore proof that Alpine has walked the injected fragment - so by the time it is there, the
     * two {@code x-if} branches inside the same row have been decided too. Polled before the control is
     * classified, so a page that has not painted the form can no longer be reported as a form that
     * painted without the control (issue #7282 - the flake was the shell rendering its DEFAULT route,
     * and the old script classified that as {@code missing} exactly like the defect).
     */
    private static final String FORM_READY_STATE = """
            var label = document.querySelector('label[for="f_SalesInvoice"]');
            var field = label ? label.parentElement : null;
            return field && field.getAttribute('data-slot') === 'field' ? 'ready' : 'not-rendered';
            """;

    /**
     * Classifies the parent control as the page rendered it: {@code locked:<label>} for the read-only
     * display, {@code free} for the combobox (its trigger is a button the select directive appends next
     * to the bound input), {@code missing} when the form rendered but neither branch produced a control
     * - which is the defect this test guards, and is only ever reported once {@link #FORM_READY_STATE}
     * has settled.
     */
    private static final String PARENT_CONTROL_STATE = """
            var e = document.getElementById('f_SalesInvoice');
            if (!e) return 'missing';
            if (e.readOnly) return 'locked:' + e.value;
            return e.parentElement.querySelector('[role=combobox]') ? 'free' : 'unrendered';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_parent_control_is_locked_exactly_when_the_navigation_context_implies_it() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // Create FROM the parent: the URL names the FK, so the control is the parent's label. The
        // label resolves once the options arrive, hence the poll.
        browser.openPath(APP + "/SalesInvoiceNote/create?SalesInvoice=1");
        assertEquals("locked:INV-001", parentControlState("locked:INV-001"),
                "a create opened from the parent must show the parent's label read-only");

        // Edit FROM the parent: same lock, value from the record rather than the query param.
        browser.openPath(APP + "/SalesInvoiceNote/1/edit?SalesInvoice=1");
        assertEquals("locked:INV-001", parentControlState("locked:INV-001"),
                "an edit opened from the parent must show the parent's label read-only");

        // The child's own top-level create - nothing implies the parent, so it stays selectable.
        browser.openPath(APP + "/SalesInvoiceNote/create");
        assertEquals("free", parentControlState("free"), "with no implying context the parent must stay a free dropdown");

        // And the master's panel is what supplies that context: its Add opens the child form in the
        // shared iframe dialog with the FK named in the URL. The panels are editable on the master's
        // own edit form - the browse pane renders them read-only, with no Add.
        browser.openPath(APP + "/SalesInvoice/1/edit");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Add");
        String dialogSources = poll("""
                return Array.from(document.querySelectorAll('iframe'))
                            .map(f => f.getAttribute('src') || '')
                            .join('|');
                """, src -> src.contains("SalesInvoiceNote/create"));
        assertTrue(dialogSources.contains("SalesInvoiceNote/create?SalesInvoice=1"),
                "the detail panel must name the master FK in the child form's URL, got: " + dialogSources);
    }

    /**
     * The parent control's state, polled until it settles on the expected one (or times out) - after
     * the form itself is confirmed rendered, so the two failure modes stay apart: a form that never
     * painted fails here, naming the wait, and only a form that painted without a control reaches the
     * caller's assertion as {@code missing}.
     */
    private String parentControlState(String expected) {
        assertEquals("ready", poll(FORM_READY_STATE, "ready"::equals), "the child form did not render within " + POLL_TIMEOUT_MILLIS / 1000
                + "s - the page never reached this route, not the parent control this test classifies");
        return poll(PARENT_CONTROL_STATE, expected::equals);
    }

    private String poll(String script, Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
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
