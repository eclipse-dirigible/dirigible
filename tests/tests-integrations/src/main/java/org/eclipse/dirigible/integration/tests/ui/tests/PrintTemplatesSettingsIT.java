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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

/**
 * Settings > Print Templates in the application shell, in a browser (#7755): the section a tenant
 * admin who never sees the IDE gets. A release ships a template; the admin sees it as the active
 * shipped version, duplicates it into a tenant template, makes that one print, goes back to the
 * default, edits the copy and deletes it. Every step is the shared fragment
 * ({@code application-core/shell/views/_print-templates.html}) on the printTemplates store, so what
 * passes here holds for the other platform shells and for every generated application shell, which
 * mount the same fragment.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrintTemplatesSettingsIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "print-templates-shell-it";
    private static final String ENTITY = "ShellPrintDoc";
    private static final String PROJECT_DESCRIPTOR = registryPath("/" + PROJECT + "/project.json");
    private static final String TEMPLATE = registryPath("/" + PROJECT + "/doc/Templates/" + ENTITY + "/Print/en/standard.print");
    private static final String SHIPPED = "standard@1.28.0";
    private static final String COPY = "shell-copy";
    private static final String SELECTION_KEY = "DIRIGIBLE_PRINT_TEMPLATE_SHELLPRINTDOC_EN";
    private static final String SHELL = "/services/web/application/index.html";
    private static final String SECTION = SHELL + "#/settings/print-templates";
    private static final Duration WAIT = Duration.ofSeconds(30);

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void anAdminManagesTheTenantsPrintTemplatesFromTheShell() {
        publish(PROJECT_DESCRIPTOR, "{\"guid\": \"" + PROJECT + "\", \"version\": \"1.28.0\"}");
        publish(TEMPLATE, template("shell-v1"));

        // The sign-in redirect lands on the shell root whatever the hash was: open the shell, then route.
        ide.openPath(SHELL);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));
        browser.openPath(SECTION);
        Selenide.$(By.id("print-templates-page"))
                .shouldBe(Condition.visible, Duration.ofSeconds(60));

        // 1. the shipped version is listed, and prints: the tenant stored no selection yet
        row(SHIPPED).shouldBe(Condition.visible, WAIT)
                    .shouldHave(Condition.attribute("data-kind", "shipped"));
        row(SHIPPED).$("[data-status='active']")
                    .shouldBe(Condition.visible, WAIT);
        Selenide.$(By.id("print-templates-use-default"))
                .shouldBe(Condition.disabled);

        // 2. duplicating it makes a tenant template that records where it came from
        openMenu(SHIPPED, "duplicate");
        Selenide.$(By.id("print-templates-duplicate-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$(By.id("print-templates-duplicate-name"))
                .setValue(COPY);
        Selenide.$(By.id("print-templates-duplicate-button"))
                .shouldBe(Condition.enabled, WAIT)
                .click();
        Selenide.$(By.id("print-templates-duplicate-dialog"))
                .shouldNotBe(Condition.visible, WAIT);
        row(COPY).shouldBe(Condition.visible, WAIT)
                 .shouldHave(Condition.attribute("data-kind", "tenant"))
                 .shouldHave(Condition.text(SHIPPED));

        // 3. making the copy the active one is the tenant configuration; the shipped version is now the
        // default
        openMenu(COPY, "activate");
        row(COPY).$("[data-status='active']")
                 .shouldBe(Condition.visible, WAIT);
        row(SHIPPED).$("[data-status='default']")
                    .shouldBe(Condition.visible, WAIT);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/configurations/tenant")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.key == '" + SELECTION_KEY + "' }.value", equalTo(COPY)));

        // 4. the way back: "Use default" clears the selection and the shipped version prints again
        Selenide.$(By.id("print-templates-use-default"))
                .shouldBe(Condition.enabled, WAIT)
                .click();
        row(SHIPPED).$("[data-status='active']")
                    .shouldBe(Condition.visible, WAIT);
        Selenide.$(By.id("print-templates-use-default"))
                .shouldBe(Condition.disabled, WAIT);

        // 5. a tenant template is edited in place and saved back
        openMenu(COPY, "edit");
        SelenideElement source = Selenide.$(By.id("print-templates-source"))
                                         .shouldBe(Condition.visible, WAIT);
        source.shouldHave(Condition.value(template("shell-v1").trim()), WAIT);
        String edited = source.getValue() + "\n<!-- edited in the shell -->";
        source.setValue(edited);
        Selenide.$(By.id("print-templates-save"))
                .click();
        Selenide.$(By.id("print-templates-editor"))
                .shouldNotBe(Condition.visible, WAIT);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/print/" + ENTITY + "/templates/" + COPY + "?lang=en")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("edited in the shell")));

        // 6. a shipped version offers no Edit or Delete; the copy is deleted after a confirmation
        row(SHIPPED).$("button[aria-label='Actions for " + SHIPPED + "']")
                    .click();
        Selenide.$$("li[data-action='remove']")
                .filterBy(Condition.visible)
                .shouldHave(CollectionCondition.size(0));
        Selenide.$(By.id("print-templates-page"))
                .click();
        openMenu(COPY, "remove");
        Selenide.$(By.id("print-templates-remove-dialog"))
                .shouldBe(Condition.visible, WAIT);
        Selenide.$(By.id("print-templates-remove-confirm"))
                .click();
        row(COPY).shouldNot(Condition.exist, WAIT);
        row(SHIPPED).shouldBe(Condition.visible);

        assertEquals(Boolean.TRUE, Selenide.executeJavaScript("return Alpine.store('printTemplates').visible;"));
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/core/configurations/tenant?key=" + SELECTION_KEY)
                                                 .then()
                                                 .statusCode(204));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/print/" + ENTITY + "/templates/" + COPY + "?lang=en"));
        boolean any = false;
        for (String path : new String[] {TEMPLATE, PROJECT_DESCRIPTOR}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                any = true;
            }
        }
        if (any) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private static SelenideElement row(String name) {
        return Selenide.$("#print-templates-table tr[data-template='" + name + "']");
    }

    /** Opens a row's action menu and picks an item. */
    private static void openMenu(String name, String action) {
        row(name).$("button[aria-label='Actions for " + name + "']")
                 .shouldBe(Condition.visible, WAIT)
                 .click();
        Selenide.$$("li[data-action='" + action + "']")
                .findBy(Condition.visible)
                .click();
    }

    private void publish(String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(bytes);
        } else {
            repository.createResource(path, bytes, false, "text/plain", true);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String registryPath(String location) {
        return IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
    }

    private static String template(String marker) {
        return """
                <document id="%s">
                    <page>
                        <section>
                            <field label="Number">{{document.number}}</field>
                        </section>
                    </page>
                </document>
                """.formatted(marker);
    }
}
