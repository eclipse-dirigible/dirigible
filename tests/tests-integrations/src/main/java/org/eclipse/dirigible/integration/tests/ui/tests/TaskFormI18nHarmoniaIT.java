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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8785: a form-builder task form reports its outcome in the user's language even when its module
 * has no catalog in that language. The form's chrome keys are the generated pages' sections
 * ({@code messages}, {@code defaults}), so a key the module's catalog lacks resolves against the
 * platform's translated {@code application-core:generated.*} - a Bulgarian user completing the task
 * reads "Изпратено успешно.", and one whose completion fails reads "Изпращането е неуспешно.".
 *
 * <p>
 * This needs a real browser: the sentence is chosen by the generated form's code and resolved by
 * the shared i18n runtime, in the language the user picked.
 */
class TaskFormI18nHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final String PROJECT = "taskformi18nit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String BOOKINGS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/booking/BookingController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String FORM = "/services/web/" + PROJECT + "/gen/ConfirmBooking/forms/ConfirmBooking/index.html";

    private static final String INTENT_YAML = """
            name: taskformi18nit
            entities:
              - name: Booking
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
            processes:
              - name: BookingConfirmation
                trigger: { onCreate: Booking }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmBooking } }
                  - { name: end,     kind: end }
            forms:
              - { name: ConfirmBooking, forEntity: Booking, fields: [note], actions: [confirm] }
            """;

    /** The message the form shows, once the catalogs it resolves against have loaded. */
    private static final String MESSAGE = """
            if (!window.Alpine || !Alpine.store('i18n') || !Alpine.store('i18n').ready) return null;
            return Alpine.$data(document.body).message || null;
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_task_form_reports_its_outcome_in_bulgarian_without_a_bulgarian_catalog_of_its_own() {
        generate();
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        assertFalse(repository.hasResource(PROJECT_PATH + "/i18n/bg-BG/ConfirmBooking.form.json"),
                "the module must have no Bulgarian catalog of the form - the shared fallback is under test");
        publish();
        String task = taskFor(createBooking());

        ide.openHomePage();
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");

        assertEquals("Изпращането е неуспешно.", confirm("no-such-task"), "a failing completion must report the Bulgarian failure");
        assertEquals("Изпратено успешно.", confirm(task), "a completion must report the Bulgarian success");
    }

    /**
     * Opens the task form of the given task, presses its Confirm button and returns what it reports.
     */
    private String confirm(String task) {
        browser.openPath(FORM + "?taskId=" + task);
        // Selenide's driver is bound to this thread, so the condition is polled on it.
        await().pollInSameThread()
               .atMost(TIMEOUT)
               .until(() -> Selenide.executeJavaScript(
                       "return !!(window.Alpine && Alpine.store('i18n') && Alpine.store('i18n').ready && document.querySelector('button'));"));
        Selenide.executeJavaScript("[...document.querySelectorAll('button')].find(b => b.innerText.trim() === 'Confirm').click();");
        AtomicReference<String> message = new AtomicReference<>();
        await().pollInSameThread()
               .atMost(TIMEOUT)
               .until(() -> {
                   message.set(Selenide.executeJavaScript(MESSAGE));
                   return message.get() != null;
               });
        return message.get();
    }

    private int createBooking() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Note", "first"))
                                                        .when()
                                                        .post(BOOKINGS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    /** The confirm task of this booking's instance, found by the business key the trigger stamped. */
    private String taskFor(int booking) {
        String match = "find { it.processInstanceBusinessKey == '" + booking + "' }.id";
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body(match, notNullValue())
                                                          .extract()
                                                          .path(match)),
                TIMEOUT.toSeconds());
        return task.get();
    }

    private void write(String path, String content) {
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generate() {
        write(PROJECT_PATH + "/app.intent", INTENT_YAML);
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
    }

    private void publish() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @AfterEach
    void cleanup() {
        Selenide.executeJavaScript("localStorage.removeItem('codbex.harmonia.language');");
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
