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
 * A picker whose selected label is long keeps its New and Refresh buttons on the form (issue
 * #8659). The picker row is a flex row of the select and its buttons; a flex item never shrinks
 * below its content unless it is allowed to, so a nowrap label of a couple of hundred characters
 * widened the select past the form, pushed the buttons over the Details rail and made Refresh
 * unclickable, while the label's own ellipsis never applied.
 *
 * <p>
 * This needs a real browser: where the buttons end up is decided by layout. Both forms that carry
 * the row shape are covered - the manage form and the document (header-items) form - each with a
 * to-one picker (New + Refresh) and a subset picker (Refresh).
 */
class PickerLongLabelHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "picker-long-label-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/maintenance/index.html#";

    /** About 200 characters, the length of the asset name the issue was observed with. */
    private static final String LONG_LABEL = "Hydraulic press line ".repeat(10)
                                                                    .trim();

    private static final String INTENT_YAML = """
            name: maintenance
            entities:
              - name: Asset
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 300, required: true }
              - name: Tag
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 300, required: true }
              - name: WorkOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string }
                relations:
                  - { name: asset, kind: manyToOne, to: Asset }
                  - { name: tags, kind: subset, to: Tag }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: reference, type: string }
                relations:
                  - { name: asset, kind: manyToOne, to: Asset }
                  - { name: tags, kind: subset, to: Tag }
              - name: InvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer }
                relations:
                  - { name: invoice, kind: manyToOne, to: Invoice, composition: true }
            seeds:
              - name: assets
                entity: Asset
                rows:
                  - { id: 1, name: "%1$s" }
              - name: tags
                entity: Tag
                rows:
                  - { id: 1, name: "%1$s" }
            """.formatted(LONG_LABEL);

    /**
     * Discards an open leave guard, then, on the page component {@code arguments[0]}, selects record 1
     * in the picker bound to {@code form.<arguments[1]>} (an array for a subset) and, once its trigger
     * shows the long label, measures the row: {@code ok}, {@code pending} until the label is shown, or
     * what went wrong. With the row scrolled into view, every visible button of the row must lie within
     * the field the row sits in and be the element a click at its centre reaches, and the label must be
     * truncated.
     */
    private static final String MEASURE_PICKER_ROW =
            """
                    var page = arguments[0], property = arguments[1], multiple = arguments[2], label = arguments[3];
                    if (!window.Alpine) return 'pending';
                    document.querySelectorAll('[x-data]').forEach(function (el) {
                      var data = Alpine.$data(el);
                      if (data && data.leaveOpen) data.leaveDiscard();
                    });
                    var root = document.querySelector('[x-data="' + page + '"]');
                    var input = root && root.querySelector('#f_' + property);
                    var select = input && input.closest('[data-slot=select]');
                    var trigger = select && select.querySelector('[data-slot=select-input]');
                    if (!trigger) return 'pending';
                    var form = Alpine.$data(input).form;
                    var selected = multiple ? (Array.isArray(form[property]) && form[property].indexOf('1') >= 0) : form[property] === '1';
                    if (!selected) {
                      form[property] = multiple ? ['1'] : '1';
                      return 'pending';
                    }
                    if (trigger.textContent.trim() !== label) return 'pending';
                    var row = select.parentElement;
                    row.scrollIntoView({ block: 'center' });
                    var field = row.parentElement.getBoundingClientRect();
                    var problems = [];
                    Array.from(row.querySelectorAll(':scope > button')).filter(b => b.offsetParent !== null).forEach(function (b) {
                      var r = b.getBoundingClientRect(), name = b.getAttribute('aria-label') || b.textContent.trim();
                      if (r.right > field.right + 1) problems.push(name + ' ends at ' + Math.round(r.right) + ', past the field at ' + Math.round(field.right));
                      var hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
                      if (!hit || !b.contains(hit)) {
                        problems.push(name + ' is not reachable by a click at its centre (' + Math.round(r.left) + ',' + Math.round(r.top)
                            + '), it is covered by ' + (hit ? hit.outerHTML.slice(0, 200) : 'nothing - off screen'));
                      }
                    });
                    if (row.querySelectorAll(':scope > button').length === 0) problems.push('no buttons beside the picker');
                    var text = trigger.querySelector('span');
                    if (!text || text.scrollWidth <= text.clientWidth) problems.push('the label is not truncated');
                    return problems.length ? problems.join('; ') : 'ok';
                    """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_long_picker_label_truncates_and_keeps_the_buttons_on_the_form() {
        generateAndPublish();
        ide.openHomePage();
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1280, 720));

        browser.openPath(APP + "/WorkOrder/create");
        assertEquals("ok", measure("WorkOrderFormPage", "Asset", false), "manage form, to-one picker");
        assertEquals("ok", measure("WorkOrderFormPage", "Tags", true), "manage form, subset picker");

        // The work order now holds unsaved picks, so leaving it raises the leave guard; the measurement
        // discards it before it looks for the invoice page.
        browser.openPath(APP + "/Invoice/create");
        assertEquals("ok", measure("InvoiceDocumentPage", "Asset", false), "document form, to-one picker");
        assertEquals("ok", measure("InvoiceDocumentPage", "Tags", true), "document form, subset picker");
    }

    private String measure(String page, String property, boolean multiple) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(MEASURE_PICKER_ROW, page, property, multiple, LONG_LABEL);
            if (value != null && !"pending".equals(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return "the picker never showed the long label";
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
