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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import io.restassured.response.Response;
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
 *
 * <p>
 * The server-side Duplicate (dirigible #8752): the page calls the generated {@code POST
 * .../{id}/duplicate} and falls back to the probe only when the controller has no such endpoint.
 * The endpoint copies the record and numbers the copy inside one unit of work that holds the source
 * row locked, so two duplicates of one record released at the same moment are numbered one after
 * the other instead of both taking the same free name.
 */
class IntentDuplicateCopyNameIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "dupname";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String BILLS = API + "/billtemplate/BillTemplateController";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long CONCURRENCY_TIMEOUT_SECONDS = 30;
    private static final String BILLS_TABLE = "DUPNAME_BILL_TEMPLATE";

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

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_duplicate_is_named_from_its_source_and_numbered_past_the_names_taken() {
        generateProject();
        String page = contentOf("gen/" + PROJECT + "/js/components/pages/BillTemplate/BillTemplateDocumentPage.js");

        // A controller without the duplicate endpoint (404): the page probes and creates itself.
        // Nothing taken: the copy is "<name> (copy)", the literal default still applies.
        assertEquals("Rent (copy)|false|searches 1|focus Name of 9", duplicate(page, "[]", false));
        // "(copy)" and "(copy 2)" taken in that company: the copy is "(copy 3)", each probe asked for the
        // name in the source's company.
        assertEquals("Rent (copy 3)|false|searches 3|focus Name of 9", duplicate(page, "['Rent (copy)', 'Rent (copy 2)']", false));
        // With the endpoint (#8752) the page sends it the defaults it computed and probes nothing - the
        // server numbers the copy.
        assertEquals("duplicate 1 {\"Name\":\"Rent (copy)\",\"Active\":false}|searches 0|focus Name of 9",
                duplicate(page, "['Rent (copy)']", true));

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
     * probe as taken when its name is in {@code takenNames} (and it names company 1), and the duplicate
     * endpoint with a 404 unless {@code endpoint}. Answers
     * {@code <posted Name>|<posted Active>|searches <n>|focus <field> of <id>}, or
     * {@code duplicate <id> <body>|searches <n>|focus <field> of <id>} when the endpoint created the
     * copy.
     */
    private static String duplicate(String pageScript, String takenNames, boolean endpoint) {
        try (Context context = Context.newBuilder("js")
                                      .option("engine.WarnInterpreterOnly", "false")
                                      .build()) {
            context.eval("js", "var pages = {}; var window = this; var document = { addEventListener: (event, callback) => callback() };"
                    + " var Alpine = { data: (name, factory) => { pages[name] = factory; } };"
                    + " var baseFormPage = () => ({ resetSaveOutcome() {}, clearPristine() {}, applyApiError(e) { __error = String(e); } });"
                    + " var printActions = () => ({}); var __error = null; var __posted = null; var __searches = 0; var __route = null;"
                    + " var __endpoint = " + endpoint + "; var __duplicated = null;" + " var __taken = " + takenNames + ";"
                    + " window.PineconeRouter = { navigate: (route) => { __route = route; } };" + " var App = { services: { api: {"
                    + "   get: (url) => Promise.resolve({ Id: 1, Name: 'Rent', Active: true, Company: 1, Uuid: 'x' }),"
                    + "   post: (url, body) => {"
                    + "     if (url.endsWith('/duplicate')) { if (!__endpoint) return Promise.reject({ httpStatus: 404 });"
                    + "       __duplicated = url.split('/').slice(-2)[0] + ' ' + JSON.stringify(body); return Promise.resolve({ Id: 9 }); }"
                    + "     if (url.endsWith('/search')) { __searches++; const e = body.equals;"
                    + "       return Promise.resolve(e.Company === 1 && __taken.indexOf(e.Name) >= 0 ? [{ Id: 5 }] : []); }"
                    + "     __posted = body; return Promise.resolve({ Id: 9 }); } } } };");
            context.eval("js", pageScript);
            context.eval("js", "var page = pages['BillTemplateDocumentPage'](); page.id = 1; var __done = false;"
                    + " page.duplicate().then(() => { __done = true; });");
            return context.eval("js",
                    "__error ? 'error ' + __error : !__done ? 'pending' : (__duplicated ? 'duplicate ' + __duplicated : __posted.Name + '|' + __posted.Active)"
                            + " + '|searches ' + __searches"
                            + " + '|focus ' + window.__duplicateFocus.field + ' of ' + window.__duplicateFocus.id")
                          .asString();
        }
    }

    /**
     * The generated endpoint (#8752), running: a duplicate is the source's copy with the defaults the
     * request carries, numbered past the names taken; two duplicates of one record released together
     * get two names; an unknown record is a 404.
     */
    @Test
    void the_server_duplicates_and_numbers_two_concurrent_copies_apart() throws Exception {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        AtomicReference<Integer> rent = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rent.set(given().contentType("application/json")
                                                          .body("{\"Name\":\"Rent\",\"Active\":true,\"Company\":1}")
                                                          .when()
                                                          .post(BILLS)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("Id")),
                TIMEOUT_SECONDS);
        String duplicateUrl = BILLS + "/" + rent.get() + "/duplicate";
        String defaults = "{\"Name\":\"Rent (copy)\",\"Active\":false}";

        // One after the other: the defaults applied, the company copied, the second numbered.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(defaults)
                                                 .when()
                                                 .post(duplicateUrl)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo("Rent (copy)"))
                                                 .body("Active", equalTo(false))
                                                 .body("Company", equalTo(1)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(defaults)
                                                 .when()
                                                 .post(duplicateUrl)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo("Rent (copy 2)")));

        // Two at once: both are held on the source row until each is waiting for it, then released.
        // Each probes only after the other committed, so they take (copy 3) and (copy 4) - not both
        // the same free name, which is what two client-side probes did.
        List<Response> racers = duplicatesReleasedTogether(rent.get(), duplicateUrl, defaults);
        Set<String> names = new java.util.TreeSet<>();
        for (Response racer : racers) {
            assertEquals(200, racer.statusCode(), racer.asString());
            names.add(racer.jsonPath()
                           .getString("Name"));
        }
        assertEquals(Set.of("Rent (copy 3)", "Rent (copy 4)"), names);

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(defaults)
                                                 .when()
                                                 .post(BILLS + "/999999/duplicate")
                                                 .then()
                                                 .statusCode(404));
    }

    /**
     * Holds the source row locked, sends two duplicates of it, waits until both are blocked on the held
     * row - so both are inside the endpoint before either has probed a name - releases it, and answers
     * both responses.
     */
    private List<Response> duplicatesReleasedTogether(int source, String url, String body) throws Exception {
        try (Connection held = dataSourcesManager.getDefaultDataSource()
                                                 .getConnection()) {
            held.setAutoCommit(false);
            try (PreparedStatement lock =
                    held.prepareStatement("SELECT * FROM \"" + BILLS_TABLE + "\" WHERE \"BILL_TEMPLATE_ID\" = ? FOR UPDATE")) {
                lock.setInt(1, source);
                try (ResultSet rows = lock.executeQuery()) {
                    assertTrue(rows.next(), "the source row must exist");
                }
            }
            return restAssuredExecutor.executeWithResult(() -> {
                List<CompletableFuture<Response>> requests = List.of(duplicateAsync(url, body), duplicateAsync(url, body));
                await().atMost(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                       .pollInterval(100, TimeUnit.MILLISECONDS)
                       .until(() -> requests.stream()
                                            .anyMatch(CompletableFuture::isDone)
                               || blockedOn(BILLS_TABLE) >= 2);
                boolean bothHeld = requests.stream()
                                           .noneMatch(CompletableFuture::isDone);
                try {
                    held.commit();
                    assertTrue(bothHeld, "both duplicates must wait for the source row's lock before either answers");
                    return List.of(requests.get(0)
                                           .get(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            requests.get(1)
                                    .get(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                } catch (Exception e) {
                    throw new IllegalStateException("a held duplicate did not answer", e);
                }
            });
        }
    }

    private static CompletableFuture<Response> duplicateAsync(String url, String body) {
        return CompletableFuture.supplyAsync(() -> given().contentType("application/json")
                                                          .body(body)
                                                          .when()
                                                          .post(url));
    }

    /** How many sessions wait for a row lock while running a statement on the table. */
    private int blockedOn(String table) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            String product = connection.getMetaData()
                                       .getDatabaseProductName();
            String query = product.toLowerCase(Locale.ROOT)
                                  .contains("postgres")
                                          ? "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE ?"
                                          : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL"
                                                  + " AND UPPER(EXECUTING_STATEMENT) LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, "%" + table + "%");
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getInt(1) : 0;
                }
            }
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
