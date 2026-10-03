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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
 * without displacing the convention roles - dirigible #7652.
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
    private static final long TIMEOUT_SECONDS = 90;

    private static final String PASSWORD = "extra-roles-it-password";
    private static final String OWNER = "owner-holder";
    private static final String WRITER = "full-access-holder";
    private static final String REPORT_READER = "report-reader";
    private static final String AUDITOR = "auditor-holder";
    private static final String STRANGER = "no-role-holder";

    private static final String INTENT_YAML = """
            name: extraroles
            description: every generated gate admits the deployment's roles beside its own

            entities:
              - name: Ticket
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: title,  type: string, required: true, length: 100 }
                  - { name: points, type: integer }

            reports:
              - name: TicketTotals
                source: Ticket
                dimensions: [title]
                measures: ["sum(points)"]
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
        writeIntent();
        // The first Generate scaffolds the developer-owned .settings; the deployment then adds its roles
        // to it, as a developer would, and the second Generate binds them.
        generateProject();
        addExtraRolesToSettings();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

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
