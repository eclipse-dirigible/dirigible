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
 * #8728: the generated application's own sidebar lists only the entities the caller's role may
 * read. A user holding one narrow role ({@code Accountant}, which may write invoices and nothing
 * else) used to find every entity there, and Customers opened the list page's error state; the
 * application shell hosting the app already listed only the perspectives the role reads. A user
 * whose role reads both keeps both entries.
 *
 * <p>
 * This needs a real browser: the entries are hidden by an Alpine decision taken at runtime from the
 * platform's role-filtered perspective listing, so the generated markup proves only that the
 * conditions exist. The classification waits for that decision first - a hidden entry would
 * otherwise pass for the plain reason that the listing had not been read yet.
 */
class NarrowRoleSidebarHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "narrow-sidebar-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/narrowsidebar/index.html#";

    private static final String PASSWORD = "narrow-sidebar-it-password";
    private static final String ACCOUNTANT = "narrow-sidebar-it-accountant";
    private static final String CLERK = "narrow-sidebar-it-clerk";

    private static final String INTENT_YAML = """
            name: narrowsidebar
            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            permissions:
              - { role: NarrowSidebarAccountant, can: [Invoice:write] }
              - { role: NarrowSidebarClerk,      can: [Customer:write, Invoice:write] }
            """;

    /**
     * The sidebar's Entities group as the caller sees it, once the shell has decided what it offers:
     * {@code filtered=<the listing was applied>;entities=<shown entries>}.
     */
    private static final String SIDEBAR_STATE = """
            if (!window.Alpine) return 'not-rendered';
            var shell = Alpine.$data(document.body);
            if (!shell.navUnfiltered && !shell.offeredPerspectives) return 'pending';
            var group = Array.from(document.querySelectorAll('[x-h-sidebar-group]'))
                             .find(g => {
                               var label = g.querySelector('[x-h-sidebar-group-label]');
                               return label && label.textContent.trim() === 'Entities';
                             });
            if (!group) return 'no-entities-group';
            var entries = Array.from(group.querySelectorAll('li[x-h-sidebar-menu-item]'))
                               .filter(li => li.style.display !== 'none')
                               .map(li => li.textContent.trim());
            return 'filtered=' + !shell.navUnfiltered + ';entities=' + entries.join(',');
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
    void the_sidebar_lists_exactly_the_entities_the_role_reads() {
        generateAndPublish();
        securityUtil.ensureUserInDefaultTenant(ACCOUNTANT, PASSWORD, "NarrowSidebarAccountant");
        securityUtil.ensureUserInDefaultTenant(CLERK, PASSWORD, "NarrowSidebarClerk");

        ideFactory.create(browser, ACCOUNTANT, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/dashboard");
        assertEquals("filtered=true;entities=Invoices", poll(SIDEBAR_STATE, "filtered=true;entities=Invoices"),
                "a role that reads invoices alone must find Invoices in the sidebar, and no Customers");

        browser.clearCookies();
        ideFactory.create(browser, CLERK, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/dashboard");
        assertEquals("filtered=true;entities=Customers,Invoices", poll(SIDEBAR_STATE, "filtered=true;entities=Customers,Invoices"),
                "a role that reads both entities must keep both entries");
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
