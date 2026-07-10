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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DSL emission + runtime coverage: for the intent features whose enforcement lives in GENERATED
 * code, assert (1) the generated artifacts CONTAIN the enforcement, and (2) the published app
 * ENFORCES it over REST.
 *
 * <p>
 * This test exists because the generation pipeline degrades silently: Velocity skips undefined
 * variables, an unknown seed-row key is dropped (and a NOT NULL FK then makes CSVIM skip every
 * row), and a stale registry template generates feature-less code - all with every pipeline step
 * returning success. A feature's test must therefore assert the OUTERMOST observable layer (the
 * generated token at minimum, the runtime behavior where reachable), never only the parsed model.
 * Covered here: {@code immutableIn} (409 on write/delete), {@code checks} (exactlyOne / itemsMin /
 * itemsSumEqual), {@code hierarchy}/{@code leafOnly}, {@code multilingual} (read-time overlay),
 * seed rows carrying a RELATION column, and aggregate totals.
 */
class IntentEmissionCoverageIT extends IntegrationTest {

    private static final String PROJECT = "emission-test";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/emission/api";

    private static final String INTENT_YAML = """
            name: emission
            description: DSL emission coverage fixture - every feature here has an enforcement assert
            languages: [en, bg]

            entities:
              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              # multilingual: the schema gains EMISSION_UNIT_LANG and every read overlays the
              # Accept-Language translation (asserted at runtime with the bg seed below).
              - name: Unit
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              # hierarchy: the self-relation forms the tree; leafOnly references below must reject
              # a parent node server-side.
              - name: Account
                hierarchy: Parent
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
                relations:
                  - { name: Parent, kind: manyToOne, to: Account }

              # The document master: immutable once POSTED (status 2), post gated by document checks.
              - name: Entry
                immutableIn: [2]
                checks:
                  - { kind: itemsMin, count: 1, status: 2, message: "Entry needs at least one line" }
                  - { kind: itemsSumEqual, over: [debit, credit], status: 2, message: "Debits must equal credits" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: debit,  type: decimal, aggregate: true }
                  - { name: credit, type: decimal, aggregate: true }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Account, kind: manyToOne, to: Account, leafOnly: true }
                  - { name: Status,  kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

              - name: EntryLine
                checks:
                  - { kind: exactlyOne, fields: [debit, credit], message: "Exactly one of debit/credit" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal }
                  - { name: credit, type: decimal }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
                  - { name: Unit,  kind: manyToOne, to: Unit }

            seeds:
              - name: entry-statuses
                entity: EntryStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: POSTED }
              - name: units
                entity: Unit
                rows:
                  - { id: 1, name: Piece }
              - name: units-bg
                entity: Unit
                language: bg
                rows:
                  - { id: 1, name: "Брой" }
              # A seed row carrying a RELATION column (Parent) - the regression for the silent
              # unknown-key drop: the emitted CSV must contain the FK column and BOTH rows must
              # import (a dropped NOT-NULL-relevant column makes CSVIM skip rows silently).
              - name: accounts
                entity: Account
                rows:
                  - { id: 1, name: Assets }
                  - { id: 2, name: Cash, Parent: 1 }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void generated_code_contains_every_feature_enforcement_and_the_published_app_enforces_it() {
        writeIntent(INTENT_YAML);
        // Drive model-to-code from the generate response's OWN plan (template + parameters per
        // entry) - the production path. Hardcoding empty parameters silently skips every
        // parameter-gated producer (e.g. javaRuntime gates the leafOnly repository class).
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            String template = String.valueOf(codeGeneration.get("templateId"));
            String modelPath = String.valueOf(codeGeneration.get("path"));
            String parameters = new Gson().toJson(codeGeneration.get("parameters"));
            generateFromModel(template, modelPath, parameters);
        }

        assertEmission();

        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        assertRuntimeEnforcement();
    }

    /** Layer 1: the enforcement TOKENS are present in the generated sources. */
    private void assertEmission() {
        String entryController = contentOf("gen/emission/api/entry/EntryController.java");
        assertTrue(entryController.contains("requireMutable"),
                "immutableIn must emit the requireMutable gate in the entity's REST controller");
        assertTrue(entryController.contains("must reference a leaf"),
                "leafOnly must emit the server-side children check in the REST controller");

        String entryRepository = contentOf("gen/emission/data/entry/EntryRepository.java");
        assertTrue(entryRepository.contains("Entry needs at least one line"),
                "checks: itemsMin must emit its authored message into the repository gate");
        assertTrue(entryRepository.contains("Debits must equal credits"),
                "checks: itemsSumEqual must emit its authored message into the repository gate");

        String lineController = contentOf("gen/emission/api/entry/EntryLineController.java");
        assertTrue(lineController.contains("Exactly one of debit/credit"),
                "checks: exactlyOne must emit its authored message into the row-level REST validation");

        String schema = contentOf("gen/emission/schema/" + PROJECT + ".schema");
        assertTrue(schema.contains("EMISSION_UNIT_LANG"), "multilingual must emit the _LANG translation table into the schema");
        String unitRepository = contentOf("gen/emission/data/settings/UnitRepository.java");
        assertTrue(unitRepository.contains("Translator"), "multilingual must emit the read-time translation overlay into the repository");

        // The seed's RELATION key (Parent: 1) must survive into the CSV as the FK column - an
        // unknown/mis-cased key is dropped silently and CSVIM then skips the rows.
        String accountsCsv = contentOf("accounts.csv");
        assertTrue(accountsCsv.contains("ACCOUNT_PARENT"), "a seed row's relation key must emit the FK column into the seed CSV");
        assertTrue(entryRepository.contains("EntryLineRepository"),
                "aggregate: true must make the master repository recompute totals from its items child");
    }

    /** Layer 2 (the outermost): the published app enforces the features over REST. */
    private void assertRuntimeEnforcement() {
        // Seeds imported COMPLETELY - both account rows incl. the one with the relation column
        // (regression: a dropped FK column made CSVIM skip every row with zero errors).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/account/AccountController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(2)),
                30);

        // multilingual read-time overlay: the bg translation replaces the seeded name.
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "bg")
                                                 .when()
                                                 .get(API + "/settings/UnitController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].Name", equalTo("Брой")));

        // leafOnly: Account 1 has a child, so referencing it must be rejected server-side.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Date\":\"2026-01-15\",\"Account\":1}")
                                                 .when()
                                                 .post(API + "/entry/EntryController")
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(400)));

        // A valid DRAFT entry on the leaf account.
        AtomicInteger created = new AtomicInteger();
        restAssuredExecutor.execute(() -> created.set(given().contentType("application/json")
                                                             .body("{\"Date\":\"2026-01-15\",\"Account\":2}")
                                                             .when()
                                                             .post(API + "/entry/EntryController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("Id")));
        int entryId = created.get();

        // checks: itemsMin - carrying the gate status with no lines must be rejected.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + entryId + ",\"Date\":\"2026-01-15\",\"Account\":2,\"Status\":2}")
                                                 .when()
                                                 .put(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(400)));

        // checks: exactlyOne - a line with BOTH sides must be rejected at the row level.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Entry\":" + entryId + ",\"Debit\":100,\"Credit\":100}")
                                                 .when()
                                                 .post(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(400)));

        // One debit line only -> sums unequal -> the itemsSumEqual gate must reject POSTED.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Entry\":" + entryId + ",\"Debit\":100}")
                                                 .when()
                                                 .post(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + entryId + ",\"Date\":\"2026-01-15\",\"Account\":2,\"Status\":2}")
                                                 .when()
                                                 .put(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(400)));

        // Balance the entry -> POSTED is accepted...
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Entry\":" + entryId + ",\"Credit\":100}")
                                                 .when()
                                                 .post(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + entryId + ",\"Date\":\"2026-01-15\",\"Account\":2,\"Status\":2}")
                                                 .when()
                                                 .put(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(200));

        // ...and immutableIn now enforces: user writes and deletes on the POSTED record are 409.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + entryId
                                                         + ",\"Date\":\"2026-01-15\",\"Account\":2,\"Status\":2,\"Note\":\"tamper\"}")
                                                 .when()
                                                 .put(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(409));
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String yaml) {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    /** Run a language template against a generated model through the real generation service. */
    private void generateFromModel(String templateModule, String modelFile, String parametersJson) {
        String payload = "{\"template\":\"" + templateModule + "\",\"parameters\":" + parametersJson + "}";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(payload)
                                                 .when()
                                                 .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + PROJECT
                                                         + "?path=" + modelFile)
                                                 .then()
                                                 .statusCode(201));
    }

    @AfterEach
    void cleanup() {
        // Unpublish leniently - the run may fail before publish ever happened.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }

}
