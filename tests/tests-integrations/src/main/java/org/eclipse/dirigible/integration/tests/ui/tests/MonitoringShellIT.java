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

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * Smoke test for the Monitoring shell. It asserts what only a real browser can: that the Harmonia +
 * Alpine bootstrap completed and each page rendered content produced by its store.
 * <p>
 * This matters more than it looks - a single mistake in the markup (an Alpine binding on a Lucide
 * placeholder the plugin replaces, or a tab strip missing the structure Harmonia requires) aborts
 * Alpine's walk with no DOM or server-side symptom, and every binding below that node silently
 * stays raw markup.
 * <p>
 * The page roots carry a stable id, so the assertions hold whether or not the instance happens to
 * have processes, jobs or queues deployed.
 */
// One Dirigible boot for the whole class: the methods are read-only or clean up after themselves,
// so the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MonitoringShellIT extends UserInterfaceIntegrationTest {

    private static final String MONITORING_PATH = "/services/web/monitoring/index.html";

    /**
     * The configuration key the version endpoint - and therefore the sidebar - reports as the product.
     */
    private static final String DIRIGIBLE_PRODUCT_NAME = "DIRIGIBLE_PRODUCT_NAME";

    private static final String PROCESS_PROJECT = "monitoring-shell-it";

    private static final String PROCESS_KEY = "monitoring-shell-it-process";

    private static final String BUSINESS_KEY = PROCESS_KEY + "-key";

    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROCESS_PROJECT + "/process.bpmn";

    /** The instance list is a store poll away from the page render, so it gets its own budget. */
    private static final Duration INSTANCE_LIST_TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void removeDeployedProcess() {
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void overviewRendersTheInstanceState() {
        ide.openPath(MONITORING_PATH);

        // The shell chrome: the sidebar entry is rendered by Alpine from the shell component.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Overview");
        // The page itself, routed into #app by Pinecone.
        browser.assertElementExistsByTypeAndContainsText(HtmlElementType.PARAGRAPH,
                "The health of this instance and what needs attention.");
        // Tiles that only exist once the store's first poll resolved - i.e. the platform endpoints
        // answered and the bindings evaluated.
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Health");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Artefacts in error");
    }

    @Test
    void everySectionIsReachable() {
        ide.openPath(MONITORING_PATH);

        openSection("processes", "Running");
        openSection("jobs", "Jobs");
        openSection("logs", "Live");
        openSection("messaging", "Queues");
        openSection("system", "The build this instance runs, and the Java virtual machine running it.");
    }

    /**
     * The instance detail's tab strip only exists once an instance is selected, so no read-only walk
     * ever reaches it - and Harmonia 3 throws on a tab strip whose tabs are not wrapped in
     * {@code x-h-tab-item} or name no panel, which leaves the whole detail pane unrendered. A tiny
     * process with one user task is deployed and started, then selected on the Processes page.
     * <p>
     * The same walk asserts the sidebar contract: the active destination announces itself with
     * {@code aria-current="page"}, which is what {@code x-h-sidebar-menu-nav} derives from the
     * {@code data-active} flag the shell binds.
     */
    @Test
    void processesRenderTheSelectedInstanceTabs() {
        deployProcess();
        startProcess();

        ide.openPath(MONITORING_PATH);
        browser.clickOnElementById("monitoring-nav-processes");
        Selenide.$(By.id("monitoring-nav-processes"))
                .shouldHave(attribute("aria-current", "page"));

        Selenide.$(By.xpath("//*[@id='monitoring-processes-page']//td[normalize-space(.)='" + BUSINESS_KEY + "']"))
                .shouldBe(visible, INSTANCE_LIST_TIMEOUT)
                .click();
        Selenide.$$(By.cssSelector("#monitoring-processes-page [x-h-tab-item]"))
                .shouldHave(size(3));
        Selenide.$(By.id("monitoring-process-tab-variables"))
                .click();
        Selenide.$(By.id("monitoring-process-panel-variables"))
                .shouldBe(visible);
    }

    /**
     * The build is the first thing asked of an instance ("what is deployed here?"), so it is asserted
     * on its own: the System page's card, filled from {@code /services/core/version}.
     */
    @Test
    void systemNamesTheDeployedBuild() {
        ide.openPath(MONITORING_PATH);

        browser.clickOnElementById("monitoring-nav-system");
        browser.assertElementExistsByIdAndContainsText("monitoring-system-version", "Product");
        browser.assertElementExistsByIdAndContainsText("monitoring-system-version", "Version");
        // The same figures condensed onto the sidebar, where every page shows them. The expected name is
        // read from the configuration the sidebar itself renders, not hard-coded: this IT ships in
        // dirigible-tests-integrations for downstream editions to reuse, and a rebranded assembly names
        // its own build here - hard-coding "Eclipse Dirigible" made the test unpassable for them.
        browser.assertElementExistsByIdAndContainsText("monitoring-build", Configuration.get(DIRIGIBLE_PRODUCT_NAME));
    }

    /**
     * Click a sidebar entry and assert its page rendered. The entry is addressed by its id, not its
     * label: a label match would also hit page content (a "System" span in the Overview's database-pool
     * tile, for instance).
     *
     * @param section the sidebar entry / page name
     * @param expectedText text the page renders regardless of what is deployed on the instance
     */
    private void openSection(String section, String expectedText) {
        browser.clickOnElementById("monitoring-nav-" + section);
        browser.assertElementExistsByIdAndContainsText("monitoring-" + section + "-page", expectedText);
    }

    /**
     * Publishes a one-user-task process straight into the registry and lets the synchronizers deploy
     * it.
     */
    private void deployProcess() {
        String bpmn = """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Monitoring shell approval" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                    <userTask id="approve" name="Approve" flowable:candidateGroups="ADMINISTRATOR"/>
                    <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY);
        repository.createResource(BPMN_REGISTRY_PATH, bpmn.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /** Starts an instance that parks on the user task, so the Processes page lists it as running. */
    private void startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + BUSINESS_KEY + "\",\"parameters\":\"{}\"}";
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(body)
                                                 .when()
                                                 .post("/services/bpm/bpm-processes/instance")
                                                 .then()
                                                 .statusCode(200));
    }
}
