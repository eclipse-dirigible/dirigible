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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
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
 *
 * <p>
 * The same posting, walked through a closed fiscal period (#7703, #8243): what the period refused
 * is posted when it reopens - a red storno whose original was posted while the period was still
 * open, and a document that moved on in its flow while the period was closed - and nothing the
 * posting does not hold is: a document sent back to draft, or voided before it was ever posted.
 *
 * <p>
 * And the moment itself is the transition's, not the document's later state (#8505): a document
 * moved on before the asynchronous posting ran is posted all the same, unless it was withdrawn.
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
    private static final String PERIODS = API + "/fiscalperiod/FiscalPeriodController";
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

              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              # The lock on the created document: a post dated inside a CLOSED period waits for it to
              # reopen (#7703).
              - name: FiscalPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }

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
                immutableInPeriod: { period: FiscalPeriod, date: date }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
                  - { name: Doc,    kind: manyToOne, to: Doc }
                  - { name: Storno, kind: manyToOne, to: Entry }

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
              - { name: SendDoc,   forEntity: Doc,   from: [2], setStatus: 3, label: Send }
              - { name: VoidDoc,   forEntity: Doc,   from: [2, 3], setStatus: 4, label: Void }
              # What "somebody has acted on the entry" is, in this fixture: the accountant posts it.
              - { name: PostEntry, forEntity: Entry, from: [1], setStatus: 2, label: Post }
              # ...or rejects it, which makes it history (#7959).
              - { name: RejectEntry, forEntity: Entry, from: [1], setStatus: 5, label: Reject }
              - { name: ClosePeriod,  forEntity: FiscalPeriod, from: [1], setStatus: 2, label: Close }
              - { name: ReopenPeriod, forEntity: FiscalPeriod, from: [2], setStatus: 1, label: Reopen }

            postings:
              - name: docPosting
                event: { onTransition: Doc, when: "Status == 2" }
                creates: Entry
                backReference: Doc
                map: { date: date }
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }
              - name: docStorno
                event: { onTransition: Doc, when: "Status == 4" }
                reverses: docPosting
                storno: Storno

            seeds:
              # The stages are what tell the reopen sweep a SENT document still holds its post and a
              # Draft or Voided one does not (#8243).
              - name: doc-statuses
                entity: DocStatus
                rows:
                  - { id: 1, name: Draft,  stage: draft }
                  - { id: 2, name: Issued, stage: live }
                  - { id: 3, name: Sent,   stage: live }
                  - { id: 4, name: Voided, stage: void }
                  - { id: 5, name: Rejected, stage: cancelled }
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
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
        postingLogs = new LogsAsserter("app.gen.events", Level.INFO);
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
     * #8243, the two documents the reopen sweep (#7703) used to leave unposted for good: a red storno,
     * because the sweep skipped every source carrying ANY entry - always true of a reversal, whose
     * original is the very entry it reverses - and a document that moved on in its flow while the
     * period was closed, because the sweep offered only the status the posting fires on. Each walk
     * waits for the refusal before it moves on: a handler that ran only after the reopen would post on
     * the event and prove nothing about the sweep.
     */
    @Test
    void a_reopened_period_posts_the_storno_and_the_document_that_moved_on_it_had_refused() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int period = create(PERIODS, "{\"StartDate\":\"2025-07-01\",\"EndDate\":\"2025-07-31\",\"Status\":1}");
        // Posted while the period is open; its storno is the one the closed period refuses.
        int voided = create(DOCS, "{\"Date\":\"2025-07-10\",\"Amount\":300}");
        transition("IssueDoc", voided);
        int original = onlyEntryOf(voided);
        awaitLines(original, 300.0f);
        // Posted while the period is open, its entry rejected by the accountant (#7959) and the document
        // sent back for the correction - its re-issue is the one the closed period refuses.
        int reissued = create(DOCS, "{\"Date\":\"2025-07-14\",\"Amount\":80}");
        transition("IssueDoc", reissued);
        int rejected = onlyEntryOf(reissued);
        awaitLines(rejected, 80.0f);
        transition("RejectEntry", rejected);
        transition("RejectDoc", reissued);

        transition("ClosePeriod", period);
        transition("VoidDoc", voided);
        awaitRefusal(voided);
        // Issued into the closed month, then sent before it reopens.
        int sent = create(DOCS, "{\"Date\":\"2025-07-11\",\"Amount\":100}");
        transition("IssueDoc", sent);
        awaitRefusal(sent);
        transition("SendDoc", sent);
        transition("IssueDoc", reissued);
        awaitRefusal(reissued);
        // Neither of these holds a post any more: one went back to draft (its next issue posts it), the
        // other was voided before anything was posted, so there is nothing to book and nothing to reverse.
        int redrafted = create(DOCS, "{\"Date\":\"2025-07-12\",\"Amount\":50}");
        transition("IssueDoc", redrafted);
        awaitRefusal(redrafted);
        transition("RejectDoc", redrafted);
        int withdrawn = create(DOCS, "{\"Date\":\"2025-07-13\",\"Amount\":70}");
        transition("IssueDoc", withdrawn);
        awaitRefusal(withdrawn);
        transition("VoidDoc", withdrawn);

        transition("ReopenPeriod", period);

        // The storno, naming the original: the same lines negated on the same sides.
        awaitLines(stornoOf(voided, original), -300.0f);
        // The document that moved on, posted with what it says.
        awaitLines(onlyEntryOf(sent), 100.0f);
        // The re-issued document: its rejected entry is history, so it does not stand for the document
        // and the sweep posts it afresh - as its re-issue would have in an open period.
        awaitLines(entryOtherThan(reissued, rejected), 80.0f);
        // The sweep reports what it posted once it is done, so its count is the fence: it offered the
        // two documents the posting holds and has no live entry for, and the other two are left
        // unposted.
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(POSTING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage(
                          "Posting docPosting: FiscalPeriod [" + period + "] reopened - posted [2] Doc document(s)", Level.INFO));
        assertUnposted(redrafted);
        assertUnposted(withdrawn);
    }

    /**
     * #8505: the posting runs on a listener, after the transition committed, and it used to judge its
     * {@code when:} against the RE-LOADED document - so a document something had already moved on
     * (issued, then sent, paid or settled before the listener ran) was never posted, and nothing
     * offered it again. The transition decides now, read off the event.
     *
     * <p>
     * The race is staged deterministically. Each document is issued into a CLOSED period, so the
     * posting that runs on its Issue writes nothing (#7703), and is then moved on - sent, or voided.
     * The period is DELETED rather than reopened, so no reopen sweep offers anything, and the Issue's
     * {@code -transitioned} event - the row as the transition left it, in status 2 - is delivered
     * again: exactly what a listener that fell behind receives. The sent document is posted; the voided
     * one is not - withdrawn, it is the reversal's to answer for.
     */
    @Test
    void the_transition_decides_the_post_not_the_status_the_document_moved_on_to() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int period = create(PERIODS, "{\"StartDate\":\"2026-04-01\",\"EndDate\":\"2026-04-30\",\"Status\":1}");
        transition("ClosePeriod", period);
        int sent = create(DOCS, "{\"Date\":\"2026-04-02\",\"Amount\":210}");
        transition("IssueDoc", sent);
        awaitRefusal(sent);
        transition("SendDoc", sent);
        int voided = create(DOCS, "{\"Date\":\"2026-04-03\",\"Amount\":40}");
        transition("IssueDoc", voided);
        awaitRefusal(voided);
        transition("VoidDoc", voided);
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(PERIODS + "/" + period)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))),
                TIMEOUT_SECONDS);

        deliverIssued(sent, "2026-04-02", 210);
        awaitLines(onlyEntryOf(sent), 210.0f);

        deliverIssued(voided, "2026-04-03", 40);
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(POSTING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage("Doc [" + voided + "] was withdrawn (status [4]) before its posting ran",
                          Level.INFO));
        assertUnposted(voided);
    }

    /**
     * Delivers the {@code -transitioned} event of the document's Issue - the row in status 2 - as the
     * transition endpoint publishes it.
     */
    private void deliverIssued(int doc, String date, int amount) {
        MessagingFacade.sendToTopic(PROJECT + "-Doc-Doc-transitioned",
                "{\"Id\":" + doc + ",\"Date\":\"" + date + "\",\"Amount\":" + amount + ",\"Status\":2}");
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

    /** The single entry the document carries besides the given one. */
    private int entryOtherThan(int doc, int other) {
        String others = "findAll { it.Doc == " + doc + " && it.Id != " + other + " }";
        AtomicInteger entry = new AtomicInteger();
        restAssuredExecutor.execute(() -> entry.set(given().when()
                                                           .get(ENTRIES)
                                                           .then()
                                                           .statusCode(200)
                                                           .body(others, hasSize(1))
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt(others + "[0].Id")),
                POSTING_TIMEOUT_SECONDS);
        return entry.get();
    }

    /** The red storno of the document's original entry, asserting there is exactly one. */
    private int stornoOf(int doc, int original) {
        String storno = "findAll { it.Doc == " + doc + " && it.Storno == " + original + " }";
        AtomicInteger entry = new AtomicInteger();
        restAssuredExecutor.execute(() -> entry.set(given().when()
                                                           .get(ENTRIES)
                                                           .then()
                                                           .statusCode(200)
                                                           .body(storno, hasSize(1))
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt(storno + "[0].Id")),
                POSTING_TIMEOUT_SECONDS);
        return entry.get();
    }

    private void assertUnposted(int doc) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Doc == " + doc + " }", hasSize(0)));
    }

    /** The posting said it left the document for the closed period to reopen. */
    private void awaitRefusal(int doc) {
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(POSTING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage("Doc [" + doc + "] stays unposted until it reopens", Level.WARN));
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

    /**
     * The balanced pair the posting derives: one debit line and one credit line, both the amount. The
     * amount is parenthesised so a storno's negative one reads as a number, not as a subtraction.
     */
    private void awaitLines(int entry, float amount) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + entry)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("findAll { it.Debit == (" + amount + ") }.size()", equalTo(1))
                                                 .body("findAll { it.Credit == (" + amount + ") }.size()", equalTo(1)),
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
