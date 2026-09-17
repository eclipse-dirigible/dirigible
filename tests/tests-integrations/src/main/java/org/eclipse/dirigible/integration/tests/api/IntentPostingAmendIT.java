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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;

/**
 * The amend loop of #7071, walked as a user walks it: issue a document, reject it, edit it, issue
 * it again - and read what the ledger says afterwards.
 *
 * <p>
 * The rewrite was pinned only by assertions over the emitted handler (issue #7162), which compiles
 * the generated Java and runs none of it. The defect it exists for is invisible in exactly that
 * way: the handler answered the second issue with its idempotency test ("have I handled this
 * source?"), did nothing, and left a DRAFT journal entry carrying the amounts of a document that no
 * longer said them - no second entry, no error, and a ledger 60.00 short when the accountant posted
 * it.
 *
 * <p>
 * Three outcomes the handler distinguishes, and each is a runtime one:
 *
 * <ol>
 * <li>an amended source REWRITES its post in place - the same entry, the new amounts, never a
 * second entry;
 * <li>a redelivery of the same content is a no-op, so the loop is not simply "rewrite on every
 * event" - read off the line rows' IDENTITY, the only thing that tells a no-op from a rewrite that
 * re-derives the same content;
 * <li>the rewrite stops once the created document has left the status the posting created it in -
 * somebody has acted on it, so the divergence is reported and left to a correcting entry.
 * </ol>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentPostingAmendIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "amend";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String DOCS = API + "/doc/DocController";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final String LINES = API + "/entry/EntryLineController";
    private static final String TRANSITIONS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final long TIMEOUT_SECONDS = 90;
    /** The posting handler runs off the source's {@code -transitioned} topic, after the commit. */
    private static final long POSTING_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: amend
            description: posting amend fixture - a rejected, edited and re-issued document rewrites its post

            entities:
              - name: DocStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              # The source. Issue posts it, Reject sends it back for the edit, Issue posts it
              # again - the documented amend path, and the one that raises the moment twice.
              - name: Doc
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }

              # The created document. Its OWN status is what bounds the rewrite: while it still
              # holds the init: the posting created it with, it is the posting's to correct.
              - name: Entry
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
                  - { name: Doc,    kind: manyToOne, to: Doc }

              - name: EntryLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }

            transitions:
              - { name: IssueDoc,  forEntity: Doc,   from: [1], setStatus: 2, label: Issue }
              - { name: RejectDoc, forEntity: Doc,   from: [2], setStatus: 1, label: Reject }
              # What "somebody has acted on the entry" is, in this fixture: the accountant posts it.
              - { name: PostEntry, forEntity: Entry, from: [1], setStatus: 2, label: Post }

            postings:
              - name: docPosting
                event: { onTransition: Doc, when: "Status == 2" }
                creates: Entry
                backReference: Doc
                map: { date: date }
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }

            seeds:
              - name: doc-statuses
                entity: DocStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Issued }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /**
     * The generated handlers log through the client SDK, which prefixes their names with {@code app.};
     * the appender goes on in {@code @BeforeEach} because Spring re-initializes logback while it starts
     * the context and drops anything attached earlier.
     */
    private LogsAsserter postingLogs;

    @BeforeEach
    void attachLogsAsserter() {
        postingLogs = new LogsAsserter("app.gen.events", Level.ERROR);
    }

    @Test
    void a_rejected_edited_and_re_issued_document_rewrites_the_post_it_already_has() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int doc = create(DOCS, "{\"Date\":\"2026-02-03\",\"Amount\":1200}");
        transition("IssueDoc", doc);

        // The post the first issue derived.
        int entry = onlyEntryOf(doc);
        awaitLines(entry, 1200.0f);

        // The amend path: rejected, edited, issued again. The document keeps its identity and so must
        // its post - the second issue raises the posting moment a second time.
        transition("RejectDoc", doc);
        update(DOCS + "/" + doc, "{\"Id\":" + doc + ",\"Date\":\"2026-02-03\",\"Amount\":1260,\"Status\":1}");
        transition("IssueDoc", doc);

        // The post says what the document says NOW - rewritten in place...
        awaitLines(entry, 1260.0f);
        // ...and it is still the only one: an amend is not a second posting.
        assertEquals(entry, onlyEntryOf(doc), "an amended source must rewrite ITS post, never open a second one");

        // A redelivery - the same loop with nothing edited - changes nothing at all. Neither the line
        // count nor the amounts can tell that apart from a rewrite: the rewrite keeps the SAME header
        // and re-derives the very same two lines, so both readings are already true before the handler
        // has looked at the event and stay true whichever branch it takes. The claim is therefore made
        // on the line IDS, the one thing the rewrite path cannot keep - it deletes the rows it replaces
        // and inserts the derived ones (#7298).
        List<Integer> lines = lineIdsOf(entry);
        transition("RejectDoc", doc);
        transition("IssueDoc", doc);
        // Read once the handler has demonstrably got PAST that event: a second document issued
        // afterwards travels the same topic to the same durable subscriber, so its own post existing
        // means the redelivery was consumed before it. Without that fence the assertion would run
        // while the event was still in flight and pass on a rewrite-on-every-event handler too.
        int fence = create(DOCS, "{\"Date\":\"2026-02-04\",\"Amount\":10}");
        transition("IssueDoc", fence);
        awaitLines(onlyEntryOf(fence), 10.0f);
        assertEquals(entry, onlyEntryOf(doc));
        awaitLines(entry, 1260.0f);
        assertEquals(lines, lineIdsOf(entry), "a redelivery must leave the post it already wrote alone, its rows included");

        // Past the created document's own lifecycle the rewrite stops: the accountant posts the entry,
        // and from there a divergence is reported rather than silently overwritten - unwinding a
        // document somebody has acted on is a correcting entry's job.
        transition("PostEntry", entry);
        transition("RejectDoc", doc);
        update(DOCS + "/" + doc, "{\"Id\":" + doc + ",\"Date\":\"2026-02-03\",\"Amount\":1900,\"Status\":1}");
        transition("IssueDoc", doc);
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(POSTING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage("was NOT rewritten", Level.ERROR));
        // The refusal is the whole claim: the posted entry still carries the amounts it was posted
        // with, and no correcting entry was invented behind the accountant's back.
        assertEquals(entry, onlyEntryOf(doc));
        awaitLines(entry, 1260.0f);
    }

    /**
     * The single entry the source carries, asserting there is exactly one.
     *
     * <p>
     * The rows are picked out here rather than by a query parameter: the generated list endpoint
     * filters by a COMPOSITION foreign key, and the back-reference to the source is a plain
     * association, so {@code ?Doc=} is simply ignored and the whole table comes back.
     */
    private int onlyEntryOf(int doc) {
        String ofDoc = "findAll { it.Doc == " + doc + " }";
        AtomicInteger entry = new AtomicInteger();
        restAssuredExecutor.execute(() -> entry.set(given().when()
                                                           .get(ENTRIES)
                                                           .then()
                                                           .statusCode(200)
                                                           .body(ofDoc, hasSize(1))
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt(ofDoc + "[0].Id")),
                POSTING_TIMEOUT_SECONDS);
        return entry.get();
    }

    /**
     * The ids of the entry's line rows, ascending - the identity only a genuine no-op keeps.
     *
     * <p>
     * The rewrite path keeps the header ({@code targetRepository.update(target)}) and replaces the
     * lines, so a redelivery that rewrote instead of returning early leaves the entry id, the line
     * count and both amounts exactly as they were, and is visible only in these ids (#7298).
     */
    private List<Integer> lineIdsOf(int entry) {
        AtomicReference<List<Integer>> ids = new AtomicReference<>();
        restAssuredExecutor.execute(() -> ids.set(given().when()
                                                         .get(LINES + "?Entry=" + entry)
                                                         .then()
                                                         .statusCode(200)
                                                         .body("", hasSize(2))
                                                         .extract()
                                                         .jsonPath()
                                                         .getList("Id", Integer.class)),
                POSTING_TIMEOUT_SECONDS);
        return ids.get()
                  .stream()
                  .sorted()
                  .toList();
    }

    /** The balanced pair the posting derives: one debit line and one credit line, both the amount. */
    private void awaitLines(int entry, float amount) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + entry)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("findAll { it.Debit == " + amount + " }.size()", equalTo(1))
                                                 .body("findAll { it.Credit == " + amount + " }.size()", equalTo(1)),
                POSTING_TIMEOUT_SECONDS);
    }

    private void transition(String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(TRANSITIONS + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private void update(String path, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(path)
                                                 .then()
                                                 .statusCode(200));
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
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
