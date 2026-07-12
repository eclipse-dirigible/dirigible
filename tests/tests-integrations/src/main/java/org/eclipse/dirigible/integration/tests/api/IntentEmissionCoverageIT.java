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
import static org.hamcrest.Matchers.nullValue;
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
 * Covered here: {@code immutableWhen} / {@code immutable} (409 on write/delete), {@code checks}
 * (exactlyOne / itemsMin / itemsSumEqual), {@code hierarchy}/{@code leafOnly}, {@code multilingual}
 * (read-time overlay), seed rows carrying a RELATION column, aggregate totals, and the personal
 * (my) surface ({@code identity}/{@code personal}/{@code sensitive}: scoped reads, forced owner,
 * stripped fields).
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

              # Append-only (immutable: true): e.g. the snapshot stored when a document is sent -
              # user writes and deletes are rejected from the moment a record is created.
              - name: Snapshot
                immutable: true
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: payload, type: string, length: 500 }

              # The document master: immutable once POSTED (status 2), post gated by document checks.
              - name: Entry
                immutableWhen: "Status == 2"
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

              # identity/personal/sensitive: Person maps the logged-in user (the IT runs as
              # admin - the seed below maps it); Claim is the personal entity with a sensitive
              # field; ClaimLine inherits the personal scope through its composition parent.
              - name: Person
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 200 }
                  - { name: email, type: string, required: true, unique: true, length: 320 }

              - name: Claim
                label: "{note} ({Person.name})"
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                  - { name: rate, type: decimal, sensitive: true }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }

              - name: ClaimLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: day,    type: date }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim, composition: true }

            # collection-driven generation: the monthly job creates one Claim per Person and,
            # under each, one ClaimLine per working day of the month (amount defaulted).
            schedules:
              - name: monthly-claims
                cron: "0 0 4 1 * *"
                entity: Person
                generate:
                  to: Claim
                  map: { Person: id }
                  defaults: { note: monthly }
                  children:
                    - to: ClaimLine
                      parent: Claim
                      forEach: { days: workingDays }
                      dayField: day
                      defaults: { amount: 8 }

            processes:
              # assignee: personal - the confirm task lands in exactly the owner's Inbox (the IT
              # runs as admin, mapped by the Person seed below).
              - name: ClaimConfirm
                trigger: { onCreate: Claim }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: personal } }
                  - { name: end, kind: end }

            seeds:
              - name: people
                entity: Person
                rows:
                  - { id: 1, name: Admin, email: admin }
                  - { id: 2, name: Other, email: other@example.com }
              - name: claims
                entity: Claim
                rows:
                  - { id: 1, note: mine,    rate: 50, Person: 1 }
                  - { id: 2, note: foreign, rate: 70, Person: 2 }
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
                "immutableWhen must emit the requireMutable gate in the entity's REST controller");
        String snapshotController = contentOf("gen/emission/api/snapshot/SnapshotController.java");
        assertTrue(snapshotController.contains("requireMutable") && snapshotController.contains("append-only"),
                "immutable: true must emit the unconditional append-only gate in the REST controller");
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

        // personal: the ADDITIONAL scoped controller exists, resolves the current user through the
        // identity entity's repository, and scrubs the sensitive field from responses.
        String claimMy = contentOf("gen/emission/api/claim/ClaimMyController.java");
        assertTrue(claimMy.contains("eq(\"Email\", username)"), "personal must emit the identity match against the logged-in username");
        assertTrue(claimMy.contains("entity.Rate = null"), "sensitive must emit the response scrub in the personal controller");
        assertTrue(claimMy.contains("entity.Person = me"), "personal must force the owner FK server-side on create");
        String lineMy = contentOf("gen/emission/api/claim/ClaimLineMyController.java");
        assertTrue(lineMy.contains("requireMyParent"),
                "a composition child must inherit the personal scope as an ancestor-ownership guard");

        // assignee: personal - the BPMN assigns the task to the start-time-resolved owner and the
        // trigger listener seeds that variable from the identity mapping.
        String bpmn = contentOf("ClaimConfirm.bpmn");
        assertTrue(bpmn.contains("flowable:assignee=\"${__personalUser}\""),
                "assignee: personal must emit a per-user flowable:assignee, not a candidate group");
        String claimTrigger = contentOf("gen/events/ClaimConfirmTrigger.java");
        assertTrue(claimTrigger.contains("__personalUser"),
                "the trigger listener must seed the __personalUser variable from the identity mapping");

        // personal UI (phase B): the my pages exist, the form never mentions the sensitive field,
        // and the SPA routes + sidebar carry the personal surface.
        String myList = contentOf("gen/emission/js/components/pages/my/ClaimMyListPage.js");
        assertTrue(myList.contains("ClaimMyController"), "the my list page must talk to the scoped controller only");
        String myForm = contentOf("gen/emission/views/my/Claim-form.html");
        assertTrue(!myForm.contains("form.Rate"), "the personal form must not render the sensitive field at all");
        assertTrue(!myForm.contains("form.Person"), "the personal form must not render the owner FK control");
        String myLineForm = contentOf("gen/emission/js/components/pages/my/ClaimLineMyFormPage.js");
        assertTrue(myLineForm.contains("ClaimLineMyController"), "a personal child gets its own my form page");
        // Regression guard (#6263): the personal pages must call the shared shell service object
        // App.services.api. The bug emitted App.api - which does not exist - so every personal list
        // and form load, save and delete threw "Cannot read properties of undefined (reading 'get')"
        // at runtime while emission stayed green (the assert above only checked the controller name,
        // which is present regardless of the API object). Assert the object, not just the path.
        String myFormPage = contentOf("gen/emission/js/components/pages/my/ClaimMyFormPage.js");
        for (String personalPage : new String[] {myList, myFormPage, myLineForm}) {
            assertTrue(personalPage.contains("App.services.api"), "a personal page must call the shared API service App.services.api");
            assertTrue(!personalPage.contains("App.api."),
                    "a personal page must not call the nonexistent App.api object (regression #6263)");
        }
        String spaIndex = contentOf("gen/emission/index.html");
        assertTrue(spaIndex.contains("/my/Claim"), "the SPA must route the personal pages");
        String myPerspective = contentOf("gen/emission/perspectives/my/Claim/perspective.extension");
        assertTrue(myPerspective.contains("application-personal-perspectives"),
                "the personal perspective must register on the My Shell's extension point");

        // collection-driven generation: the job creates the parent AND its per-working-day children.
        String job = contentOf("gen/events/MonthlyClaimsJob.java");
        assertTrue(job.contains("savedTarget"), "the scheduled generation must save the parent and keep its id for the children");
        assertTrue(job.contains("getDayOfWeek"), "a days child must iterate the working days of the month");
        assertTrue(job.contains("ClaimLineRepository"), "the child rows must be saved through the child's repository");

        // label: the repository recomputes the stored display Name on every write path.
        String claimRepository = contentOf("gen/emission/data/claim/ClaimRepository.java");
        assertTrue(claimRepository.contains("computeName"), "label must emit the display-name computation into the repository");
        assertTrue(claimRepository.contains("related.Name"),
                "a one-hop label token must load the related record and read its display property");
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
                                                 .statusCode(400));

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
                                                 .statusCode(400));

        // checks: exactlyOne - a line with BOTH sides must be rejected at the row level.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Entry\":" + entryId + ",\"Debit\":100,\"Credit\":100}")
                                                 .when()
                                                 .post(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(400));

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
                                                 .statusCode(400));

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

        // ...and immutableWhen now enforces: user writes and deletes on the POSTED record are 409.
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

        // immutable: true (append-only): a snapshot can be created, then never edited or deleted.
        AtomicInteger snapshot = new AtomicInteger();
        restAssuredExecutor.execute(() -> snapshot.set(given().contentType("application/json")
                                                              .body("{\"Payload\":\"sent-invoice snapshot\"}")
                                                              .when()
                                                              .post(API + "/snapshot/SnapshotController")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + snapshot.get() + ",\"Payload\":\"tamper\"}")
                                                 .when()
                                                 .put(API + "/snapshot/SnapshotController/" + snapshot.get())
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(API + "/snapshot/SnapshotController/" + snapshot.get())
                                                 .then()
                                                 .statusCode(409));

        // personal: the my-surface lists ONLY the current user's rows, with the sensitive field
        // stripped; a foreign record is a 404 (indistinguishable from missing).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimMyController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1))
                                                 .body("[0].Note", equalTo("mine"))
                                                 .body("[0].Rate", nullValue()),
                30);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimMyController/2")
                                                 .then()
                                                 .statusCode(404));
        // The power surface is unaffected: all rows, sensitive values included.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(2))
                                                 .body("[0].Rate", equalTo(50.0F)));

        // Writes force the owner and ignore the sensitive field, whatever the client sends.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Note\":\"spoofed\",\"Person\":2,\"Rate\":999}")
                                                 .when()
                                                 .post(API + "/claim/ClaimMyController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Person", equalTo(1))
                                                 .body("Rate", nullValue())
                                                 // label: the stored display name computed on write - "{note} ({Person.name})".
                                                 .body("Name", equalTo("spoofed (Admin)")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Note\":\"edited\",\"Person\":2,\"Rate\":999}")
                                                 .when()
                                                 .put(API + "/claim/ClaimMyController/1")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimController/1")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Note", equalTo("edited"))
                                                 .body("Person", equalTo(1))
                                                 .body("Rate", equalTo(50.0F))
                                                 .body("Name", equalTo("edited (Admin)")));

        // The personal-assignee task landed in the owner's (admin's) Inbox - assigned, not just
        // claimable (the trigger + BPMN chain resolved the identity mapping at start time).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/tasks?type=assigned")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(org.hamcrest.Matchers.containsString("Confirm")),
                30);

        // The composition child guards through its parent: the foreign claim's lines are a 404,
        // creating a line under a foreign claim is a 404, under my own claim it works.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimLineMyController?Claim=2")
                                                 .then()
                                                 .statusCode(404));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Claim\":2,\"Amount\":10}")
                                                 .when()
                                                 .post(API + "/claim/ClaimLineMyController")
                                                 .then()
                                                 .statusCode(404));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Claim\":1,\"Amount\":10}")
                                                 .when()
                                                 .post(API + "/claim/ClaimLineMyController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimLineMyController?Claim=1")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1)));

        // My Shell (phase C): the shell page is served and aggregates the published personal
        // perspective through the application-personal-perspectives extension point.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/my/index.html")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-personal-perspectives")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(org.hamcrest.Matchers.containsString("emission-test-my-Claim")),
                30);
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
