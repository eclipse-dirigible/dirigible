/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.restassured.response.Response;

/**
 * {@code .settings} {@code access.extraRoles} admits the deployment's roles at every generated gate
 * without displacing the convention roles - dirigible #7652 - except where the module says
 * otherwise: an {@code exclusive: true} grant keeps them off its resource's gates (#8112), and a
 * user task takes them only with {@code extraRoles: true}, never letting its submitter approve
 * (#8111).
 *
 * <p>
 * A multitenant application runs a tenant member's request under the fixed application role
 * ({@code Owner} / {@code User}), so every generated gate must admit those - while the per-entity
 * convention roles stay declared and assignable, or every assignment made in the Security
 * perspective stops working. The intent here authors no {@code permissions:}: the extras are the
 * only access model the deployment adds, the case the setting exists for.
 *
 * <p>
 * Each holder is a real basic-auth user of the default tenant holding exactly one role, so the
 * generated controllers' {@code isInAnyRole} gate is what answers. The holders of the convention
 * roles prove more than access: {@code SecurityUtil} looks a role up by its name, which fails when
 * the template layer declares the whole gate as one role name (the convention role then does not
 * exist) or when an extra role is declared twice.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentExtraRolesIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "extraroles";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String SETTINGS_PATH = PROJECT_PATH + "/" + PROJECT + ".settings";
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final String REPORT = "/services/java/" + PROJECT + "/gen/tickettotals/api/reports/TicketTotalsController";
    private static final String SALARIES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/salary/SalaryController";
    private static final String LEAVES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/leaverequest/LeaveRequestController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String PASSWORD = "extra-roles-it-password";
    private static final String OWNER = "owner-holder";
    private static final String WRITER = "full-access-holder";
    private static final String REPORT_READER = "report-reader";
    private static final String AUDITOR = "auditor-holder";
    private static final String STRANGER = "no-role-holder";
    private static final String REQUESTER = "leave-requester";
    private static final String COLLEAGUE = "tenant-colleague";
    private static final String PAYROLL = "payroll-holder";
    /** The other test's Owner is a user of the same context, so this one needs a name of its own. */
    private static final String TENANT_OWNER = "tenant-owner";

    private static final String INTENT_YAML = """
            name: extraroles
            description: every generated gate admits the deployment's roles beside its own

            entities:
              - name: Ticket
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: title,  type: string, required: true, length: 100 }
                  - { name: points, type: integer }
              - name: Salary
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: integer }
              - name: LeaveRequest
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: days, type: integer }

            reports:
              - name: TicketTotals
                source: Ticket
                dimensions: [title]
                measures: ["sum(points)"]

            processes:
              - name: LeaveApproval
                trigger: { onCreate: LeaveRequest }
                steps:
                  - { name: submit,  kind: userTask, args: { assignee: employee, form: SubmitLeave } }
                  - { name: approve, kind: userTask, args: { assignee: manager, form: ApproveLeave, extraRoles: true } }

            forms:
              - { name: SubmitLeave,  forEntity: LeaveRequest, fields: [days], actions: [submit] }
              - { name: ApproveLeave, forEntity: LeaveRequest, fields: [days], actions: [approve] }

            permissions:
              - { role: Employee, can: [LeaveRequest:write] }
              - { role: Payroll,  can: [Salary:write], exclusive: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void every_gate_admits_the_extra_roles_and_the_convention_roles_keep_working() {
        generateAndPublish();

        securityUtil.createUserInDefaultTenant(OWNER, PASSWORD, "Owner");
        securityUtil.createUserInDefaultTenant(WRITER, PASSWORD, PROJECT + ".Ticket.TicketFullAccess");
        securityUtil.createUserInDefaultTenant(REPORT_READER, PASSWORD, PROJECT + ".Report.TicketTotalsReadOnly");
        securityUtil.createUserInDefaultTenant(AUDITOR, PASSWORD, "Auditor");
        securityUtil.createUserInDefaultTenant(STRANGER, PASSWORD);

        // The deployment's role passes every entity gate, read and write, and the report gate.
        assertFullAccess(OWNER);
        assertStatus(OWNER, 200, () -> given().get(REPORT));

        // The convention roles are still declared and still the gate's own.
        assertFullAccess(WRITER);
        assertStatus(REPORT_READER, 200, () -> given().get(REPORT));

        // A read-only extra opens the read gates only.
        assertStatus(AUDITOR, 200, () -> given().get(TICKETS));
        assertStatus(AUDITOR, 200, () -> given().get(REPORT));
        assertStatus(AUDITOR, 403, () -> given().contentType("application/json")
                                                .body("{\"Title\":\"audited\",\"Points\":1}")
                                                .post(TICKETS));

        // Holding neither is still refused at every gate.
        int id = create(OWNER, "kept");
        assertStatus(STRANGER, 403, () -> given().get(TICKETS));
        assertStatus(STRANGER, 403, () -> given().contentType("application/json")
                                                 .body("{\"Title\":\"stranger\",\"Points\":1}")
                                                 .post(TICKETS));
        assertStatus(STRANGER, 403, () -> given().contentType("application/json")
                                                 .body("{\"Id\":" + id + ",\"Title\":\"stranger\",\"Points\":1}")
                                                 .put(TICKETS + "/" + id));
        assertStatus(STRANGER, 403, () -> given().delete(TICKETS + "/" + id));
        assertStatus(STRANGER, 403, () -> given().get(REPORT));
    }

    /**
     * A tenant {@code User} - the role every tenant member holds - is refused what the module keeps
     * from it: an {@code exclusive} entity (#8112), the submit task of somebody else's request, and the
     * approval of their own (#8111).
     */
    @Test
    void the_module_keeps_a_tenant_user_off_an_exclusive_entity_and_off_their_own_approval() {
        generateAndPublish();
        securityUtil.createUserInDefaultTenant(TENANT_OWNER, PASSWORD, "Owner");
        securityUtil.createUserInDefaultTenant(COLLEAGUE, PASSWORD, "User");
        securityUtil.createUserInDefaultTenant(REQUESTER, PASSWORD, "User");
        securityUtil.assignRoleToUser(REQUESTER, DirigibleTestTenant.createDefaultTenant()
                                                                    .getId(),
                "Employee");
        securityUtil.createUserInDefaultTenant(PAYROLL, PASSWORD, "Payroll");

        // An exclusive grant: the gates are the module's roles only, read and write alike.
        String salary = "{\"Amount\":1000}";
        assertStatus(COLLEAGUE, 403, () -> given().contentType("application/json")
                                                  .body(salary)
                                                  .post(SALARIES));
        assertStatus(TENANT_OWNER, 403, () -> given().contentType("application/json")
                                                     .body(salary)
                                                     .post(SALARIES));
        assertStatus(COLLEAGUE, 403, () -> given().get(SALARIES));
        assertStatus(PAYROLL, 200, () -> given().contentType("application/json")
                                                .body(salary)
                                                .post(SALARIES));

        // The submit task names a role: the deployment's tenant roles are not its candidates, so a
        // colleague cannot submit somebody else's request.
        int leave = createLeave();
        String submit = taskFor(leave, "Submit");
        // A completion is attempted ONCE: a retried 200 would have completed the task, and the retry would
        // then be refused for a task that is gone.
        restAssuredExecutor.execute(() -> complete(submit, "submit").then()
                                                                    .statusCode(403),
                "localhost", COLLEAGUE, PASSWORD);
        restAssuredExecutor.execute(() -> complete(submit, "submit").then()
                                                                    .statusCode(200),
                "localhost", REQUESTER, PASSWORD);

        // The approval opts in to them - and still refuses the one who submitted.
        String approve = taskFor(leave, "Approve");
        restAssuredExecutor.execute(() -> complete(approve, "approve").then()
                                                                      .statusCode(403)
                                                                      .body(containsString("You cannot approve your own leave request")),
                "localhost", REQUESTER, PASSWORD);
        restAssuredExecutor.execute(() -> complete(approve, "approve").then()
                                                                      .statusCode(200),
                "localhost", COLLEAGUE, PASSWORD);
    }

    private int createLeave() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Days\":3}")
                                                        .when()
                                                        .post(LEAVES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                REQUESTER, PASSWORD, TIMEOUT_SECONDS);
        return id.get();
    }

    /**
     * The open task of this request's instance with the given name, as the administrator's Inbox lists
     * it.
     */
    private String taskFor(int leave, String name) {
        String match = "find { it.processInstanceBusinessKey == '" + leave + "' && it.name == '" + name + "' }";
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body(match, notNullValue())
                                                          .extract()
                                                          .path(match + ".id")),
                TIMEOUT_SECONDS);
        return task.get();
    }

    /** Completes the task with the one action its form offers. */
    private static Response complete(String task, String action) {
        return given().contentType("application/json")
                      .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"" + action + "\"}}")
                      .post(TASKS + "/" + task);
    }

    /** Generate twice around the deployment's .settings edit, then publish. */
    private void generateAndPublish() {
        writeIntent();
        // The first Generate scaffolds the developer-owned .settings; the deployment then adds its roles
        // to it, as a developer would, and the second Generate binds them.
        generateProject();
        addExtraRolesToSettings();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /** List, create, update and delete of the generated entity, all as the given holder. */
    private void assertFullAccess(String user) {
        int id = create(user, "by " + user);
        assertStatus(user, 200, () -> given().get(TICKETS));
        assertStatus(user, 200, () -> given().contentType("application/json")
                                             .body("{\"Id\":" + id + ",\"Title\":\"updated by " + user + "\",\"Points\":5}")
                                             .put(TICKETS + "/" + id));
        assertStatus(user, 200, () -> given().delete(TICKETS + "/" + id));
    }

    private int create(String user, String title) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Title\":\"" + title + "\",\"Points\":3}")
                                                        .when()
                                                        .post(TICKETS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                user, PASSWORD, TIMEOUT_SECONDS);
        return id.get();
    }

    private void assertStatus(String user, int status, Supplier<Response> request) {
        restAssuredExecutor.execute(() -> request.get()
                                                 .then()
                                                 .statusCode(status),
                user, PASSWORD, TIMEOUT_SECONDS);
    }

    private void addExtraRolesToSettings() {
        IResource settingsFile = repository.getResource(SETTINGS_PATH);
        JsonObject settings = JsonParser.parseString(new String(settingsFile.getContent(), StandardCharsets.UTF_8))
                                        .getAsJsonObject();
        JsonObject access = settings.has("access") ? settings.getAsJsonObject("access") : new JsonObject();
        access.add("extraRoles", roles("Owner", "User"));
        access.add("extraRolesReadOnly", roles("Auditor"));
        settings.add("access", access);
        // What a multitenant deployment lists so its tenant members can take tasks at all - and what made
        // every one of them a candidate for every task before #8111.
        JsonObject userTasks = new JsonObject();
        userTasks.add("candidateGroupsExtra", roles("ADMINISTRATOR", "Owner", "User"));
        settings.add("userTasks", userTasks);
        Gson gson = new GsonBuilder().setPrettyPrinting()
                                     .create();
        settingsFile.setContent(gson.toJson(settings)
                                    .getBytes(StandardCharsets.UTF_8));
    }

    private static JsonArray roles(String... names) {
        JsonArray array = new JsonArray();
        for (String name : names) {
            array.add(name);
        }
        return array;
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

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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
