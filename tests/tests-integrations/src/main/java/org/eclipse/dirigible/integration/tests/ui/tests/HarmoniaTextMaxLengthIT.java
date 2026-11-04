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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * #8135: a generated text input stops at the column length the controller enforces.
 *
 * <p>
 * The inputs carried no {@code maxlength}, so a user could paste 201 characters into a
 * 200-character Name and learn about the limit only from the 400 on save. Every text input bound to
 * a string column with a declared length now carries it - the entity form, the document header, the
 * line dialog (from the column config) and the administration editor - while dates and numbers
 * carry none. The browser leg types past the limit into a real form and checks the field stopped
 * there and says why: at the limit the field's description reads "Maximum 30 characters", and only
 * then.
 *
 * <p>
 * #8340: a date typed into a new document's date picker while the form's option lists still load
 * stays. The picker takes the text typed into its input only on that input's {@code change}, so
 * until then the date is on screen and not in the form - the defaults pass that runs once the
 * options arrive used to read the field as untouched and put today over it.
 */
class HarmoniaTextMaxLengthIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "maxlengthit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/maxlength/index.html#";
    private static final String GEN = PROJECT_PATH + "/gen/maxlength/";

    private static final String INTENT_YAML = """
            name: maxlength
            entities:
              - name: Store
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, length: 200 }
                  - { name: code,     type: string, length: 30 }
                  - { name: opened,   type: date }
                  - { name: capacity, type: integer }
              - name: Receipt
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: note,     type: string, length: 120 }
                  - { name: issuedOn, type: date, defaultValue: now }
                relations:
                  - { name: store, kind: manyToOne, to: Store }
              - name: ReceiptItem
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: comment, type: string, length: 20 }
                relations:
                  - { name: Receipt, kind: manyToOne, to: Receipt, composition: true, required: true }
            """;

    private static final String CODE_VALUE = """
            var input = document.getElementById('f_Code');
            return input ? input.value : 'not-rendered';
            """;

    /** The visible length hint of the field holding the input {@code %s}, or empty when none shows. */
    private static final String LENGTH_HINT = """
            var field = document.getElementById('%s').closest('[x-h-field]');
            var hint = Array.from(field.querySelectorAll('[x-h-field-description]')).find(p => p.checkVisibility());
            return hint ? hint.textContent.trim() : '';
            """;

    /**
     * Holds the Store option list a Receipt form asks for until {@code window.__releaseStores()} - the
     * window in which the user types into a create form whose options are still loading.
     */
    private static final String HOLD_STORE_OPTIONS = """
            var fetchNow = window.fetch;
            window.__heldStores = [];
            window.fetch = function (url, options) {
              if (String(url).indexOf('/StoreController') < 0) return fetchNow.apply(this, arguments);
              return new Promise(resolve => window.__heldStores.push(() => resolve(fetchNow(url, options))));
            };
            window.__releaseStores = function () { window.fetch = fetchNow; window.__heldStores.splice(0).forEach(go => go()); };
            """;

    /** The Receipt form's state, what its Issued On input shows and what its form holds for it. */
    private static final String ISSUED_ON = """
            var input = document.getElementById('f_IssuedOn');
            if (!input || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(input);
            return page.state + '|' + input.value + '|' + page.form.IssuedOn + '|' + (window.__heldStores.length ? 'held' : 'free');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_text_input_stops_at_the_column_length() {
        generateAndPublish();

        String storeForm = contentOf("views/Store/Store-form.html");
        assertTrue(element(storeForm, "id=\"f_Name\"").contains("maxlength=\"200\""), storeForm);
        assertTrue(element(storeForm, "id=\"f_Code\"").contains("maxlength=\"30\""), storeForm);
        assertFalse(element(storeForm, "id=\"f_Opened\"").contains("maxlength"), "a date input carries no length");
        assertFalse(element(storeForm, "id=\"f_Capacity\"").contains("maxlength"), "a number input carries no length");

        String receiptDocument = contentOf("views/Receipt/Receipt-document.html");
        assertTrue(element(receiptDocument, "id=\"f_Note\"").contains("maxlength=\"120\""), receiptDocument);
        assertTrue(receiptDocument.contains(":maxlength=\"col.maxLength\""), "the line dialog's text input must bind the column length");
        String lineRegister = contentOf("js/components/pages/Receipt/ReceiptItem.detail.js");
        assertTrue(lineRegister.contains("maxLength: 20"), lineRegister);

        String admin = contentOf("admin/index.html");
        assertTrue(admin.contains("\"name\":\"Code\"") && admin.contains("\"maxLength\":30"), admin);

        // The browser enforces it: typing 40 characters into Code leaves 30.
        ide.openHomePage();
        browser.openPath(APP + "/Store/create");
        assertEquals("", poll(CODE_VALUE, ""), "the Store form must render");
        Selenide.$("#f_Code")
                .sendKeys("A".repeat(40));
        assertEquals("A".repeat(30), Selenide.executeJavaScript(CODE_VALUE), "the input must stop at the column length");
        // ...and says why: the full field's description names the limit, an unfilled one shows nothing.
        assertEquals("Maximum 30 characters", poll(String.format(LENGTH_HINT, "f_Code"), "Maximum 30 characters"),
                "a field that reached its length must say so");
        assertEquals("", Selenide.executeJavaScript(String.format(LENGTH_HINT, "f_Name")), "a field below its length shows no hint");
    }

    @Test
    void a_date_typed_while_the_options_load_is_not_replaced_by_the_default() {
        generateAndPublish();
        // A day the default can never produce, in the instance's default (ISO) display order.
        String typed = LocalDate.now()
                                .minusDays(40)
                                .toString();

        ide.openHomePage();
        browser.openPath(APP + "/Store/create");
        assertEquals("", poll(CODE_VALUE, ""), "the application must render");
        Selenide.executeJavaScript(HOLD_STORE_OPTIONS);
        Selenide.executeJavaScript("location.hash = '#/Receipt/create'");
        assertEquals("ready|||held", poll(ISSUED_ON, "ready|||held"), "the new Receipt must render while its Store options load");

        // Typed, not committed: no Enter, no leaving the field - the picker has not taken it yet.
        Selenide.$("#f_IssuedOn")
                .sendKeys(typed);
        Selenide.executeJavaScript("window.__releaseStores()");
        String settled = poll(ISSUED_ON, "empty-form|" + typed + "||free");
        assertTrue(settled.startsWith("empty-form|" + typed + "|"), "the defaults must leave the typed date on screen, got: " + settled);

        // Leaving the field commits it: the form holds the typed date, not today.
        Selenide.$("#f_IssuedOn")
                .sendKeys(Keys.TAB);
        String expected = "empty-form|" + typed + "|" + typed + "|free";
        assertEquals(expected, poll(ISSUED_ON, expected), "the typed date must be what the form holds");
    }

    /** The element a marker opens, up to its end - the generated attributes wrap. */
    private static String element(String content, String marker) {
        int start = content.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("no element with " + marker + " in: " + content);
        }
        int end = content.indexOf(">", start);
        return content.substring(content.lastIndexOf("<", start), end);
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(GEN + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (expected.equals(value)) {
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
