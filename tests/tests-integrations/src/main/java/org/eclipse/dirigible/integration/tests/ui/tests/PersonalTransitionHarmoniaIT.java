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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8406: the personal list offers a {@code personal: true} transition on its owner's rows. The row
 * in a {@code from:} status carries the action, the row outside it carries none; running it from
 * the row menu goes through the personal run and the row then shows the new status with nothing
 * left to offer. The personal action is the personal pages' alone - the power pages list only the
 * power one.
 */
class PersonalTransitionHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "mytransitionui";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/mytransition/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/mytransition/api";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/mytransition";

    private static final int DRAFT = 1;
    private static final int SUBMITTED = 2;
    private static final int WITHDRAWN = 6;

    private static final String INTENT_YAML = """
            name: mytransition
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, length: 100, unique: true }
              - name: VacationStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
              - name: VacationRequest
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                  - { name: Status, kind: manyToOne, to: VacationStatus, function: EntityStatus, init: 1 }
            transitions:
              - { name: WithdrawVacationRequest, forEntity: VacationRequest, from: [2, 3], setStatus: 6, label: Withdraw, personal: true }
            seeds:
              - name: vacation-statuses
                entity: VacationStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }
                  - { id: 3, name: APPROVED }
                  - { id: 6, name: WITHDRAWN }
            """;

    /**
     * Each row of the personal list as {@code <note>:<the labels of its actions>}, sorted - once the
     * rows and the contributed actions have both arrived.
     */
    private static final String ROW_ACTIONS = """
            var root = document.querySelector('[x-data="VacationRequestMyListPage"]');
            if (!root || !window.Alpine || !Alpine.store('customActions').loaded) return 'pending';
            var page = Alpine.$data(root);
            if (page.state !== 'default') return 'pending';
            return page.items.map(function (row) {
                return row.Note + ':' + page.rowActions(row).map(function (a) { return a.label; }).join(',');
            }).sort().join('|');
            """;

    /** Whether each row renders its "..." menu, as {@code <note>:menu|none}, sorted. */
    private static final String ROW_MENUS = """
            var root = document.querySelector('[x-data="VacationRequestMyListPage"]');
            return Array.from(root.querySelectorAll('tbody tr')).map(function (tr) {
                var note = tr.textContent.indexOf('submitted one') >= 0 ? 'submitted one' : 'draft one';
                return note + ':' + (tr.querySelector('[data-row-actions] button') ? 'menu' : 'none');
            }).sort().join('|');
            """;

    /** The endpoints of the per-record actions the power pages and the personal pages are offered. */
    private static final String ENDPOINTS = """
            var store = Alpine.store('customActions');
            var endpoints = function (surface) {
                return store.getActions('VacationRequest', 'entity', null, surface).map(function (a) { return a.endpoint; }).join(',');
            };
            return 'power=' + endpoints() + '|my=' + endpoints('my');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_personal_list_offers_the_transition_on_the_rows_it_applies_to_and_runs_it() {
        generateAndPublish();
        // The browser signs in as admin: admin is the employee these requests belong to.
        int admin = create("/employee/EmployeeController", Map.of("Email", "admin"));
        int submitted = create("/vacationrequest/VacationRequestController",
                Map.of("Employee", admin, "Status", SUBMITTED, "Note", "submitted one"));
        create("/vacationrequest/VacationRequestController", Map.of("Employee", admin, "Status", DRAFT, "Note", "draft one"));
        ide.openHomePage();

        browser.openPath(APP + "/my/VacationRequest");
        assertEquals("draft one:|submitted one:Withdraw", pollUntil(ROW_ACTIONS, v -> !"pending".equals(v)),
                "the row in a from: status offers the transition, the row outside it offers nothing");
        assertEquals("draft one:none|submitted one:menu", Selenide.executeJavaScript(ROW_MENUS),
                "only the row with an action to offer renders a menu");
        assertEquals("power=" + EVENTS + "/WithdrawVacationRequestTransition/run|my=" + EVENTS + "/WithdrawVacationRequestTransitionMy/run",
                Selenide.executeJavaScript(ENDPOINTS), "each surface is offered the run whose endpoint serves it");

        Selenide.$$("[x-data='VacationRequestMyListPage'] tbody tr")
                .findBy(Condition.text("submitted one"))
                .$("[data-row-actions] button")
                .click();
        Selenide.$$("[x-h-menu-item]")
                .findBy(Condition.exactText("Withdraw"))
                .shouldBe(Condition.visible)
                .click();
        Selenide.executeJavaScript("return Alpine.store('customActions').confirmRun();");

        assertEquals("draft one:|submitted one:", pollUntil(ROW_ACTIONS, "draft one:|submitted one:"::equals),
                "once withdrawn, the list re-reads the row and it offers nothing more");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/vacationrequest/VacationRequestController/" + submitted)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(WITHDRAWN)));
    }

    private int create(String controller, Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private String pollUntil(String script, Predicate<String> done) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && done.test(value)) {
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
