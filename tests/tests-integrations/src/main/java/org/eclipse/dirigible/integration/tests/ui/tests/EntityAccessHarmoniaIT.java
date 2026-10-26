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
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * The browser half of #8089: the generated manage pages offer New / Edit / Delete / Save only to a
 * caller the controller's {@code GET /access} says may write. A caller with the read gate alone
 * sees the list without them, the record in preview, and a notice instead of the create form -
 * where before it filled in a whole form only to be refused by the 403 on Create.
 *
 * <p>
 * This needs a real browser: the controls are hidden by an Alpine decision taken from the answer at
 * runtime, so the generated markup proves only that the conditions exist. Every classification
 * below waits for the answer first ({@code access} set on the page) - a reader's hidden New would
 * otherwise pass for the plain reason that nothing had been decided yet.
 */
class EntityAccessHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "access-ui-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/accessui/index.html#";

    private static final String PASSWORD = "access-ui-it-password";
    private static final String READER = "access-ui-it-reader";
    private static final String WRITER = "access-ui-it-writer";

    private static final String INTENT_YAML = """
            name: accessui
            entities:
              - name: AuditEntry
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: summary, type: string }
            permissions:
              - { role: AccessUiAuditor, can: [AuditEntry:read] }
              - { role: AccessUiClerk,   can: [AuditEntry:write] }
            seeds:
              - name: entries
                entity: AuditEntry
                rows:
                  - { id: 1, summary: First entry }
            """;

    /**
     * The list page as the caller sees it, once the access answer and the rows are both in:
     * {@code new=<toolbar New shown>;row=<the row menu's shown items>}. A closed menu's items are not
     * rendered visible, so an item counts as shown when Alpine has not hidden it.
     */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="AuditEntryManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            var actions = root.querySelector('[data-row-actions]');
            if (!page.access || !actions) return 'pending';
            var newButton = Array.from(root.querySelectorAll('button')).find(b => b.textContent.trim() === 'New');
            var items = Array.from(actions.querySelectorAll('li[x-h-menu-item]'))
                             .filter(li => li.style.display !== 'none')
                             .map(li => li.textContent.trim());
            return 'new=' + (!!newButton && newButton.checkVisibility()) + ';row=' + items.join(',');
            """;

    /**
     * The form page as the caller sees it, once the access answer is in and the page has settled:
     * {@code mode=<mode>;state=<state>;save=<visible Save/Create buttons>;notice=<create notice shown>}.
     */
    private static final String FORM_STATE = """
            var root = document.querySelector('[x-data="AuditEntryFormPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (!page.access || page.state === 'loading') return 'pending';
            var saves = Array.from(root.querySelectorAll('button'))
                             .filter(b => ['Create', 'Save'].includes(b.textContent.trim()) && b.checkVisibility());
            var notice = Array.from(root.querySelectorAll('[x-h-info-page-title]'))
                              .some(t => t.textContent.includes('but not create them') && t.checkVisibility());
            return 'mode=' + page.mode + ';state=' + page.state + ';save=' + saves.length + ';notice=' + notice;
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    @Test
    void the_write_controls_show_exactly_for_a_caller_who_may_write() {
        generateAndPublish();
        securityUtil.ensureUserInDefaultTenant(READER, PASSWORD, "AccessUiAuditor");
        securityUtil.ensureUserInDefaultTenant(WRITER, PASSWORD, "AccessUiClerk");

        ideFactory.create(browser, READER, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/AuditEntry");
        assertEquals("new=false;row=Preview", poll(LIST_STATE, "new=false;row=Preview"),
                "a reader's list must offer no New, and a row no Edit or Delete");
        browser.openPath(APP + "/AuditEntry/create");
        assertEquals("mode=create;state=cannot-create;save=0;notice=true",
                poll(FORM_STATE, "mode=create;state=cannot-create;save=0;notice=true"),
                "a reader opening the create route must get the notice, not a form it cannot save");
        browser.openPath(APP + "/AuditEntry/1/edit");
        assertEquals("mode=preview;state=ready;save=0;notice=false", poll(FORM_STATE, "mode=preview;state=ready;save=0;notice=false"),
                "a reader opening the edit route must get the record in preview");

        browser.clearCookies();
        ideFactory.create(browser, WRITER, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/AuditEntry");
        assertEquals("new=true;row=Preview,Edit,Delete", poll(LIST_STATE, "new=true;row=Preview,Edit,Delete"),
                "a writer's list must offer every write control as before");
        browser.openPath(APP + "/AuditEntry/create");
        assertEquals("mode=create;state=empty-form;save=1;notice=false",
                poll(FORM_STATE, "mode=create;state=empty-form;save=1;notice=false"),
                "a writer opening the create route must get the form and its Create");
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
