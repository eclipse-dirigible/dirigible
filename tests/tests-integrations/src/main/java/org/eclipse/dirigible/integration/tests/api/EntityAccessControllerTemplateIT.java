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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.base.callable.CallableNoResultAndNoException;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The generated controller tells the caller what it may do (#8089):
 * {@code GET <Entity>Controller/access} answers {@code {read, write}} by the same gates every
 * endpoint applies, and the generated pages hide their write controls by it. The writes themselves
 * stay the guard - a reader is still refused every one of them.
 *
 * <p>
 * {@code AuditEntry} carries authored grants, so the generated {@code .access} also constrains its
 * controller URL to the read roles: a caller holding neither role is refused there, before the
 * controller runs. {@code Memo} carries none - its gates are the convention roles and no URL
 * constraint stands in front of it - which is where a caller without the read gate reaches
 * {@code /access} and is told {@code {false, false}} rather than refused.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EntityAccessControllerTemplateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "accessgate";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ENTRIES = API + "/auditentry/AuditEntryController";
    private static final String MEMOS = API + "/memo/MemoController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String PASSWORD = "access-gate-it-password";
    /** May read audit entries, not change them. */
    private static final String READER = "access-gate-it-reader";
    /** May change audit entries - and so read them. */
    private static final String WRITER = "access-gate-it-writer";
    /** Signed in, holding no role at all. */
    private static final String NOBODY = "access-gate-it-nobody";

    private static final String INTENT_YAML = """
            name: accessgate
            description: the controller tells the caller what it may do

            entities:
              - name: AuditEntry
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: summary, type: string }

              - name: Memo
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }

            permissions:
              - { role: AccessGateAuditor, can: [AuditEntry:read] }
              - { role: AccessGateClerk,   can: [AuditEntry:write] }

            seeds:
              - name: entries
                entity: AuditEntry
                rows:
                  - { id: 1, summary: First entry }
            """;

    /**
     * The project is generated and published once for the class; every test reads the same instance.
     */
    private static boolean published;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @BeforeEach
    void publishOnce() {
        if (published) {
            return;
        }
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        // The authored roles exist once the published .roles is synchronized.
        securityUtil.ensureUserInDefaultTenant(READER, PASSWORD, "AccessGateAuditor");
        securityUtil.ensureUserInDefaultTenant(WRITER, PASSWORD, "AccessGateClerk");
        securityUtil.ensureUserInDefaultTenant(NOBODY, PASSWORD);
        published = true;
    }

    @Test
    void aReaderIsToldItMayReadButNotWrite() {
        assertAccess(READER, ENTRIES, true, false);
    }

    @Test
    void aWriterIsToldItMayDoBoth() {
        assertAccess(WRITER, ENTRIES, true, true);
    }

    @Test
    void aCallerWithoutTheReadRoleGetsFalseFalseNotA403() {
        assertAccess(NOBODY, MEMOS, false, false);
        // The answer is the gate the reads apply, not a second opinion of it.
        expect(NOBODY, () -> given().when()
                                    .get(MEMOS)
                                    .then()
                                    .statusCode(403));
    }

    @Test
    void theWritesStill403ForTheReader() {
        expect(READER, () -> given().when()
                                    .get(ENTRIES + "/1")
                                    .then()
                                    .statusCode(200)
                                    .body("Summary", equalTo("First entry")));
        expect(READER, () -> given().contentType("application/json")
                                    .body("{\"Summary\":\"forged\"}")
                                    .when()
                                    .post(ENTRIES)
                                    .then()
                                    .statusCode(403));
        expect(READER, () -> given().contentType("application/json")
                                    .body("{\"Id\":1,\"Summary\":\"forged\"}")
                                    .when()
                                    .put(ENTRIES + "/1")
                                    .then()
                                    .statusCode(403));
        expect(READER, () -> given().when()
                                    .delete(ENTRIES + "/1")
                                    .then()
                                    .statusCode(403));
        // The writer the /access answer admits is admitted by the write itself.
        expect(WRITER, () -> given().contentType("application/json")
                                    .body("{\"Summary\":\"second entry\"}")
                                    .when()
                                    .post(ENTRIES)
                                    .then()
                                    .statusCode(200));
    }

    private void assertAccess(String user, String controller, boolean read, boolean write) {
        expect(user, () -> given().when()
                                  .get(controller + "/access")
                                  .then()
                                  .statusCode(200)
                                  .body("read", equalTo(read))
                                  .body("write", equalTo(write)));
    }

    private void expect(String user, CallableNoResultAndNoException request) {
        restAssuredExecutor.execute(request, user, PASSWORD, TIMEOUT_SECONDS);
    }

    private void generateProject() {
        writeIntent();
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
}
