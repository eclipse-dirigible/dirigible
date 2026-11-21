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

import static com.codeborne.selenide.CollectionCondition.itemWithText;
import static com.codeborne.selenide.CollectionCondition.texts;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

import io.restassured.http.ContentType;

/**
 * The Monitoring shell names a process in the user's language (#8788). Its pages are translated,
 * but the process names on them were the raw BPMN names - a Bulgarian user read Bulgarian headers
 * around {@code TicketApproval}. A process declaring its module's label catalog now carries the key
 * of its name, and the shell loads that module's catalog and renders the name through it - on the
 * Processes page, in its search, and in the Overview's stuck instances. A hand-written process
 * declares no catalog and keeps its BPMN name.
 *
 * <p>
 * The fixture is an intent-generated module (its {@code TicketApproval} starts on every new ticket)
 * with a translator's bg-BG catalog for its processes, a hand-written process, and a process that
 * declares the module's catalog as a generated one does and dead-letters its only step, so the
 * Overview has an incident to name. The two hand-written ones live below the project root, where
 * the intent's Generate does not scrub them as stale output. This needs a real browser: the names
 * are resolved by the shell through the shared i18n runtime, in the language the user picked.
 */
class MonitoringProcessNamesHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String FIXTURE = "MonitoringProcessNamesHarmoniaIT";
    private static final String PROJECT = "monprocnames";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final String MONITORING = "/services/web/monitoring/index.html";

    private static final String GENERATED_PROCESS = "TicketApproval";
    private static final String STUCK_PROCESS = "TicketEscalation";
    private static final String HAND_WRITTEN_PROCESS = "MonitoringHandWritten";

    private static final String GENERATED_BG = "Одобрение на заявка";
    private static final String STUCK_BG = "Ескалация на заявка";
    private static final String HAND_WRITTEN_NAME = "Hand written review";

    /**
     * A step that fails with no retry cycle dead-letters after the default retries - about half a
     * minute.
     */
    private static final long DEAD_LETTER_TIMEOUT_SECONDS = 120;
    private static final long PROCESS_TIMEOUT_SECONDS = 90;
    private static final Duration RENDER_TIMEOUT = Duration.ofSeconds(60);

    private static final By PROCESS_TITLES = By.cssSelector("#monitoring-processes-page tbody tr td:first-child");
    private static final By INCIDENT_PROCESSES = By.cssSelector("#monitoring-overview-page tbody tr td:first-child");

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_monitoring_pages_name_a_process_in_the_users_language() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(FIXTURE + "/" + PROJECT);
        generate();
        String catalog = new String(repository.getResource(PROJECT_PATH + "/i18n/en-US/" + PROJECT + ".model.json")
                                              .getContent(),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("\"" + GENERATED_PROCESS + "\""), "the en-US catalog must name the generated process: " + catalog);
        repository.createResource(PROJECT_PATH + "/i18n/bg-BG/" + PROJECT + ".model.json",
                ResourceUtil.loadResource("/" + FIXTURE + "/" + PROJECT + ".model.bg-BG.json")
                            .getBytes(StandardCharsets.UTF_8));
        publish();

        createTicket();
        startProcess(HAND_WRITTEN_PROCESS);
        String stuck = startProcess(STUCK_PROCESS);
        awaitRunning(GENERATED_PROCESS);
        awaitDeadLetter(stuck);

        ide.openHomePage();
        // The names follow the language the instance offers, never a saved one it does not (#8632).
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        browser.openPath(MONITORING);
        Selenide.refresh();

        Selenide.$$(INCIDENT_PROCESSES)
                .shouldHave(itemWithText(STUCK_BG), RENDER_TIMEOUT);

        browser.clickOnElementById("monitoring-nav-processes");
        Selenide.$$(PROCESS_TITLES)
                .shouldHave(itemWithText(GENERATED_BG), RENDER_TIMEOUT)
                .shouldHave(itemWithText(STUCK_BG))
                .shouldHave(itemWithText(HAND_WRITTEN_NAME));

        Selenide.$("#monitoring-processes-page input[type=search]")
                .setValue("одобрение");
        Selenide.$$(PROCESS_TITLES)
                .shouldHave(texts(GENERATED_BG), RENDER_TIMEOUT);
    }

    private void generate() {
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

    private void publish() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /** A new ticket starts the generated approval. */
    private void createTicket() {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{\"Title\":\"Printer jam\"}")
                                                 .when()
                                                 .post(TICKETS)
                                                 .then()
                                                 .statusCode(200),
                PROCESS_TIMEOUT_SECONDS);
    }

    private String startProcess(String processKey) {
        String body = "{\"processDefinitionKey\":\"" + processKey + "\",\"businessKey\":\"" + processKey + "-key\",\"parameters\":\"{}\"}";
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString());
    }

    private void awaitRunning(String processKey) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instances?key=" + processKey)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("processDefinitionKey", hasItem(equalTo(processKey))),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void awaitDeadLetter(String processInstanceId) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instance/" + processInstanceId + "/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", not(empty())),
                DEAD_LETTER_TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        if (WebDriverRunner.hasWebDriverStarted()) {
            Selenide.executeJavaScript("localStorage.removeItem('codbex.harmonia.language');");
        }
        Configuration.remove(DirigibleConfig.APPLICATION_LANGUAGES.getKey());
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
