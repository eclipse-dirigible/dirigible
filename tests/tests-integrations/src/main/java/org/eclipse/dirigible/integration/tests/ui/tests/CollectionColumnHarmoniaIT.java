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
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The browser half of a list column showing a record's child rows (issue #8295): a {@code list:}
 * collection path renders the texts the controller answers as chips wherever {@code list:} decides
 * the columns - a composition child's detail panel on its master's page, the power list, and a
 * {@code related:} register without {@code show:}. The texts themselves (one read per page, a write
 * ignoring them) are asserted over HTTP by {@code IntentEmissionCoverageIT}; this asserts what a
 * person sees.
 */
class CollectionColumnHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "collection-column-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/roster/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/roster/api";
    /** What Harmonia's {@code x-h-chip} puts on the pill - no other element of these pages has it. */
    private static final String CHIP_CLASS = "rounded-full";

    /**
     * The issue's team (a composition child, so the skills show in its master's detail panel) and an
     * employee of a department (a root entity, so they show in its own power list and in the
     * department's register).
     */
    private static final String INTENT_YAML = """
            name: roster
            entities:
              - name: Skill
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Team
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: members, kind: oneToMany, to: TeamMember }
              - name: TeamMember
                list: [email, skills.skill, joinedOn]
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: email,    type: string }
                  - { name: joinedOn, type: date }
                relations:
                  - { name: Team,   kind: manyToOne, to: Team, composition: true }
                  - { name: skills, kind: oneToMany, to: TeamMemberSkill }
              - name: TeamMemberSkill
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: grantedBy, type: string }
                relations:
                  - { name: TeamMember, kind: manyToOne, to: TeamMember, composition: true }
                  - { name: skill,      kind: manyToOne, to: Skill }
              - name: Department
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                related:
                  - entity: Employee
              - name: Employee
                list: [name, skills.skill]
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: department, kind: manyToOne, to: Department }
                  - { name: skills,     kind: oneToMany, to: EmployeeSkill }
              - name: EmployeeSkill
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, composition: true }
                  - { name: skill,    kind: manyToOne, to: Skill }
            seeds:
              - name: skills
                entity: Skill
                rows:
                  - { id: 1, name: Java }
                  - { id: 2, name: SQL }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_child_rows_texts_render_as_chips_in_the_detail_panel_the_power_list_and_the_register() {
        generateAndPublish();
        int team = create("/team/TeamController", "{\"Name\":\"Core\"}");
        int ann = create("/team/TeamMemberController", "{\"Team\":" + team + ",\"Email\":\"ann@example.com\",\"JoinedOn\":\"2026-01-05\"}");
        create("/team/TeamMemberSkillController", "{\"TeamMember\":" + ann + ",\"Skill\":1,\"GrantedBy\":\"lead\"}");
        create("/team/TeamMemberSkillController", "{\"TeamMember\":" + ann + ",\"Skill\":2,\"GrantedBy\":\"cto\"}");
        int department = create("/department/DepartmentController", "{\"Name\":\"Engineering\"}");
        int eve = create("/employee/EmployeeController", "{\"Name\":\"Eve\",\"Department\":" + department + "}");
        create("/employee/EmployeeSkillController", "{\"Employee\":" + eve + ",\"Skill\":1}");
        create("/employee/EmployeeSkillController", "{\"Employee\":" + eve + ",\"Skill\":2}");

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // The Team page's TeamMember panel: ann's row, her skills as chips.
        browser.openPath(APP + "/Team/" + team + "/edit");
        browser.assertElementExistsByTypeAndText(HtmlElementType.DIV, "ann@example.com");
        assertChips("Java", "SQL");

        // The Employee power list: the column headed by the collection's label, the skills as chips.
        browser.openPath(APP + "/Employee");
        browser.assertElementExistsByTypeAndText(HtmlElementType.SPAN, "Skills");
        browser.assertElementExistsByTypeAndText("td", "Eve");
        assertChips("Java", "SQL");

        // The Department page's Employee register reproduces the list, chips included.
        browser.openPath(APP + "/Department/" + department + "/edit");
        browser.assertElementExistsByTypeAndText(HtmlElementType.DIV, "Eve");
        assertChips("Java", "SQL");
    }

    private void assertChips(String... texts) {
        for (String text : texts) {
            browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.CLASS, CHIP_CLASS, text);
        }
    }

    /** POSTs a record to a generated controller - retried until the published controller answers. */
    private int create(String controller, String body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                30);
        return id.get();
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
