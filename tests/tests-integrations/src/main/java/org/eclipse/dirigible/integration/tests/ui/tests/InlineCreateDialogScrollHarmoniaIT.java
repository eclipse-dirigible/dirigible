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
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

/**
 * The inline "create new" dialog an FK picker opens (New Customer from an invoice, New Supplier,
 * ...) scrolls in exactly one place (issue #7723). It hosts the target entity's own create page in
 * an iframe, so a long form could scroll the dialog body, the embedded page AND the page behind it
 * - the reviewer saw two vertical scrollbars side by side.
 *
 * <p>
 * This needs a real browser: which elements overflow is decided by layout, at a real viewport size.
 * The page behind and the iframe are both inspected - the iframe is same-origin.
 */
class InlineCreateDialogScrollHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "inline-create-scroll-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/sales/index.html#";

    /**
     * A customer form long enough to overflow the dialog on a laptop-sized window, opened from an order
     * form long enough to overflow the page behind it - the documents the reviewer worked in.
     */
    private static final String INTENT_YAML = """
            name: sales
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
                  - { name: legalName, type: string }
                  - { name: vatNumber, type: string }
                  - { name: registrationNumber, type: string }
                  - { name: email, type: string }
                  - { name: phone, type: string }
                  - { name: website, type: string }
                  - { name: street, type: string }
                  - { name: city, type: string }
                  - { name: postalCode, type: string }
                  - { name: country, type: string }
                  - { name: contactPerson, type: string }
                  - { name: paymentTermDays, type: integer }
                  - { name: creditLimit, type: decimal }
                  - { name: notes, type: text }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: reference, type: string }
                  - { name: orderedOn, type: date }
                  - { name: deliveryStreet, type: string }
                  - { name: deliveryCity, type: string }
                  - { name: deliveryPostalCode, type: string }
                  - { name: deliveryCountry, type: string }
                  - { name: contactEmail, type: string }
                  - { name: contactPhone, type: string }
                  - { name: carrier, type: string }
                  - { name: trackingNumber, type: string }
                  - { name: discountPercent, type: decimal }
                  - { name: shippingCost, type: decimal }
                  - { name: internalNote, type: text }
                  - { name: customerNote, type: text }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            """;

    /**
     * Every vertical scroll container the user can see: the page itself, then each visible element
     * whose content overflows a box that scrolls - in the document and in every visible same-origin
     * iframe, each entry prefixed with where it lives.
     */
    private static final String VISIBLE_SCROLLERS = """
            function visible(el, win) {
              for (var e = el; e && e.nodeType === 1; e = e.parentElement) {
                var s = win.getComputedStyle(e);
                if (s.display === 'none' || s.visibility === 'hidden') return false;
              }
              var r = el.getBoundingClientRect();
              return r.width > 0 && r.height > 0;
            }
            function describe(el) {
              return el.tagName.toLowerCase() + (el.id ? '#' + el.id : '')
                  + (el.getAttribute('data-slot') ? '[' + el.getAttribute('data-slot') + ']' : '');
            }
            function scrollers(doc, where) {
              var win = doc.defaultView, out = [];
              var root = doc.scrollingElement;
              var locked = ['hidden', 'clip'].indexOf(win.getComputedStyle(doc.documentElement).overflowY) >= 0
                  || ['hidden', 'clip'].indexOf(win.getComputedStyle(doc.body).overflowY) >= 0;
              if (root && !locked && root.scrollHeight > root.clientHeight + 1) out.push(where + ':document');
              doc.querySelectorAll('body *').forEach(function (el) {
                var oy = win.getComputedStyle(el).overflowY;
                if ((oy === 'auto' || oy === 'scroll') && el.scrollHeight > el.clientHeight + 1 && visible(el, win)) {
                  out.push(where + ':' + describe(el));
                }
              });
              doc.querySelectorAll('iframe').forEach(function (f) {
                if (visible(f, win) && f.contentDocument && f.contentDocument.body) {
                  out = out.concat(scrollers(f.contentDocument, where + '>iframe'));
                }
              });
              return out;
            }
            return scrollers(document, 'page').join(' | ');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_inline_create_dialog_scrolls_in_exactly_one_place() {
        generateAndPublish();
        ide.openHomePage();
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1280, 720));

        browser.openPath(APP + "/SalesOrder/create");
        assertEquals("opened", poll("""
                var add = Array.from(document.querySelectorAll('button'))
                               .find(b => (b.getAttribute('@click') || '').indexOf("addRelated('Customer'") === 0);
                if (!add || add.offsetParent === null) return 'no-button';
                add.click();
                return 'opened';
                """, "opened"::equals), "the order form never rendered its customer picker's New button");

        // The embedded create form has rendered once its last field is laid out inside the iframe.
        assertEquals("ready", poll("""
                var f = Array.from(document.querySelectorAll('iframe')).find(i => i.offsetParent !== null);
                var d = f && f.contentDocument;
                return d && d.getElementById('f_Notes') && d.getElementById('f_Notes').offsetParent !== null ? 'ready' : 'loading';
                """, "ready"::equals), "the inline-create dialog never rendered the customer form");

        String scrollers = poll(VISIBLE_SCROLLERS, s -> !s.isEmpty() && !s.contains(" | "));
        assertEquals(1, scrollers.isEmpty() ? 0 : scrollers.split(" \\| ").length,
                "the open inline-create dialog must scroll in exactly one place - its body - and the page behind it must not"
                        + " scroll; the visible scroll containers are: " + scrollers);

        // Closed the way its X button closes it - assigning `open`, not calling close() - the page behind
        // scrolls again.
        Selenide.executeJavaScript("Alpine.store('related').open = false");
        String restored = poll(VISIBLE_SCROLLERS, s -> s.contains("page:div#app"));
        assertTrue(restored.contains("page:div#app"),
                "closing the dialog must give the page back its scrolling, the visible scroll containers are: " + restored);
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
        return value == null ? "" : value;
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
