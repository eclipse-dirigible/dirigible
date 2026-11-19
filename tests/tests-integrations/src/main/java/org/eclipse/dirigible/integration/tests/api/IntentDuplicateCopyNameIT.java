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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A Duplicate names its copy from the source (intent {@code duplicable.defaults} {@code {field}}
 * templates, {@code unique} and {@code focus}, dirigible #8663).
 *
 * <p>
 * The case: a recurring bill template whose name carries a soft duplicate check. A Duplicate copied
 * the name verbatim, so every copy was answered with the duplicate warning. The generated document
 * page's {@code duplicate()} is run over a stubbed REST client: the copy is named
 * {@code "<name> (copy)"}, numbered {@code (copy 2)}, {@code (copy 3)} while the probe finds the
 * name taken in the same company, and the copy's page is told which input to focus. The probe's
 * request is then sent to the running, generated controller, which must answer it.
 */
class IntentDuplicateCopyNameIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "dupname";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String BILLS = API + "/billtemplate/BillTemplateController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: dupname
            description: a recurring bill template copies under its own name

            entities:
              - name: Company
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }

              - name: BillTemplate
                function: Document
                duplicable:
                  defaults: { name: "{name} (copy)", active: false }
                  unique: [name, Company]
                  focus: name
                checks:
                  - { kind: duplicate, fields: [name, Company], severity: warn, message: "A bill template with this name already exists" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 100, function: DocumentTitle }
                  - { name: active, type: boolean }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }

              - name: BillTemplateItem
                function: DocumentItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: BillTemplate, kind: manyToOne, to: BillTemplate, composition: true, required: true }

            seeds:
              - name: companies
                entity: Company
                rows:
                  - { id: 1, name: Acme }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_duplicate_is_named_from_its_source_and_numbered_past_the_names_taken() {
        generateProject();
        String page = contentOf("gen/" + PROJECT + "/js/components/pages/BillTemplate/BillTemplateDocumentPage.js");

        // Nothing taken: the copy is "<name> (copy)", the literal default still applies.
        assertEquals("Rent (copy)|false|searches 1|focus Name of 9", duplicate(page, "[]"));
        // "(copy)" and "(copy 2)" taken in that company: the copy is "(copy 3)", each probe asked for the
        // name in the source's company.
        assertEquals("Rent (copy 3)|false|searches 3|focus Name of 9", duplicate(page, "['Rent (copy)', 'Rent (copy 2)']"));

        // The probe's request is one the generated controller answers.
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Name\":\"Rent (copy)\",\"Company\":1}")
                                                 .when()
                                                 .post(BILLS)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"equals\":{\"Name\":\"Rent (copy)\",\"Company\":1}}")
                                                 .when()
                                                 .post(BILLS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1)),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"equals\":{\"Name\":\"Rent (copy 2)\",\"Company\":1}}")
                                                 .when()
                                                 .post(BILLS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(0)),
                TIMEOUT_SECONDS);
    }

    /**
     * Runs the page's {@code duplicate()} for a source "Rent" of company 1, whose REST client answers a
     * probe as taken when its name is in {@code takenNames} (and it names company 1). Answers
     * {@code <posted Name>|<posted Active>|searches <n>|focus <field> of <id>}.
     */
    private static String duplicate(String pageScript, String takenNames) {
        try (Context context = Context.newBuilder("js")
                                      .option("engine.WarnInterpreterOnly", "false")
                                      .build()) {
            context.eval("js", "var pages = {}; var window = this; var document = { addEventListener: (event, callback) => callback() };"
                    + " var Alpine = { data: (name, factory) => { pages[name] = factory; } };"
                    + " var baseFormPage = () => ({ resetSaveOutcome() {}, clearPristine() {}, applyApiError(e) { __error = String(e); } });"
                    + " var printActions = () => ({}); var __error = null; var __posted = null; var __searches = 0; var __route = null;"
                    + " var __taken = " + takenNames + ";" + " window.PineconeRouter = { navigate: (route) => { __route = route; } };"
                    + " var App = { services: { api: {"
                    + "   get: (url) => Promise.resolve({ Id: 1, Name: 'Rent', Active: true, Company: 1, Uuid: 'x' }),"
                    + "   post: (url, body) => {" + "     if (url.endsWith('/search')) { __searches++; const e = body.equals;"
                    + "       return Promise.resolve(e.Company === 1 && __taken.indexOf(e.Name) >= 0 ? [{ Id: 5 }] : []); }"
                    + "     __posted = body; return Promise.resolve({ Id: 9 }); } } } };");
            context.eval("js", pageScript);
            context.eval("js", "var page = pages['BillTemplateDocumentPage'](); page.id = 1; var __done = false;"
                    + " page.duplicate().then(() => { __done = true; });");
            return context.eval("js",
                    "__error ? 'error ' + __error : !__done ? 'pending' : __posted.Name + '|' + __posted.Active + '|searches ' + __searches"
                            + " + '|focus ' + window.__duplicateFocus.field + ' of ' + window.__duplicateFocus.id")
                          .asString();
        }
    }

    private String contentOf(String fileName) {
        IResource resource = repository.getResource(PROJECT_PATH + "/" + fileName);
        assertTrue(resource.exists(), fileName + " was not generated");
        return new String(resource.getContent(), StandardCharsets.UTF_8);
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
