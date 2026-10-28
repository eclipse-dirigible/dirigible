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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * A generated application's Reports section and dashboard tiles for a user who holds only the
 * application's own roles (issue #8277).
 *
 * <p>
 * The shell used to discover its reports at runtime through the core repository API, which answers
 * {@code ADMINISTRATOR} and {@code OPERATOR} only, so a business user saw an empty dashboard and no
 * Reports section although the report itself answered them. The intent now generates the reports
 * into the shell's configuration: the user the report's {@code can:} grants sees its entry and its
 * KPI tile, one who may not read it sees neither, and no page asks the repository API anything.
 */
class IntentReportsForBusinessUsersIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "shop";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String APP = "/services/web/" + PROJECT + "/gen/shop/index.html#/";
    private static final String REPORT_CONTROLLER = "/services/java/" + PROJECT + "/gen/invoicetally/api/reports/InvoiceTallyController";
    private static final String PASSWORD = "secret";

    private static final String INTENT_YAML = """
            name: shop
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
            reports:
              - name: InvoiceTally
                source: Invoice
                dimensions: [number]
                widget: { kind: count, label: Invoices on file, icon: shopping-cart }
            permissions:
              - role: Viewer
                can: [Invoice:read, InvoiceTally:read]
              - role: Clerk
                can: [Invoice:read]
            seeds:
              - name: invoices
                entity: Invoice
                rows:
                  - { id: 1, number: INV-1 }
                  - { id: 2, number: INV-2 }
            """;

    /** Whether the page ever requested anything from the core repository API. */
    private static final String REPOSITORY_REQUESTS =
            "return performance.getEntriesByType('resource').map(e => e.name).filter(n => n.indexOf('/services/core/repository/') >= 0);";

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
    void a_report_is_listed_and_its_tile_shown_to_exactly_the_users_its_permissions_grant() {
        writeProjectFile("app.intent", INTENT_YAML);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Neither user holds a platform role: the report's permissions alone decide what they read.
        securityUtil.createUserInDefaultTenant("viewer", PASSWORD, "Viewer");
        securityUtil.createUserInDefaultTenant("clerk", PASSWORD, "Clerk");
        assertCountStatus("viewer", 200);
        assertCountStatus("clerk", 403);

        // The Viewer sees the report in the sidebar and its KPI on the dashboard - the count of the
        // two seeded invoices - and no request of the shell went to the repository API.
        IDE viewer = openApp("viewer");
        assertEquals(List.of("InvoiceTally"), reportNames());
        Selenide.$$("[x-h-sidebar-group]")
                .findBy(Condition.text("Reports"))
                .shouldBe(Condition.visible)
                .shouldHave(Condition.text("Invoice Tally"));
        Selenide.$$("[x-h-card]")
                .findBy(Condition.text("Invoices on file"))
                .shouldBe(Condition.visible)
                .shouldHave(Condition.text("2"));
        assertEquals(List.of(), Selenide.executeJavaScript(REPOSITORY_REQUESTS), "the shell lists its reports from its own configuration");
        viewer.close();

        // The Clerk may not read the report: no entry, no Reports section, no tile.
        IDE clerk = openApp("clerk");
        assertEquals(List.of(), reportNames());
        assertFalse(Selenide.$$("[x-h-sidebar-group]")
                            .findBy(Condition.text("Reports"))
                            .is(Condition.visible),
                "a user who may read no report sees no Reports section");
        assertFalse(Selenide.$$("[x-h-card]")
                            .findBy(Condition.text("Invoices on file"))
                            .exists(),
                "nor the report's tile");
        clerk.close();

        // The administrator keeps seeing what they saw before.
        IDE admin = openApp(null);
        assertEquals(List.of("InvoiceTally"), reportNames());
        admin.close();
    }

    /** Opens the generated application signed in as the user (the administrator for null). */
    private IDE openApp(String user) {
        IDE userIde = user == null ? ideFactory.create() : ideFactory.create(user, PASSWORD);
        userIde.openPath(APP);
        // The sign-in redirect drops the fragment, so the route is opened again once the session exists.
        userIde.getBrowser()
               .openPath(APP);
        for (int i = 0; i < 80 && !Boolean.TRUE.equals(Selenide.executeJavaScript(
                "return !!(window.Alpine && Alpine.store('reports') && Alpine.store('reports').loaded);")); i++) {
            Selenide.sleep(250);
        }
        return userIde;
    }

    /** The names of the reports the open shell's store lists. */
    private static List<String> reportNames() {
        assertTrue(Boolean.TRUE.equals(Selenide.executeJavaScript("return Alpine.store('reports').loaded;")),
                "the reports store finished loading");
        return Selenide.executeJavaScript("return Alpine.store('reports').items.map(r => r.name);");
    }

    private void assertCountStatus(String user, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(REPORT_CONTROLLER + "/count")
                                                 .then()
                                                 .statusCode(status),
                user, PASSWORD, 60);
    }

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generateProject() {
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
