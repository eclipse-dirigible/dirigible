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
 * (read-time overlay), seed rows carrying a RELATION column, aggregate totals, {@code transitions}
 * (the guarded on-demand status flip: allowed-status 200, wrong-status/guard 409), {@code postings}
 * with {@code reverses} (post on a transition; red-storno reversal on void - negated amounts,
 * storno link, fail-soft), and the personal (my) surface
 * ({@code identity}/{@code personal}/{@code sensitive}: scoped reads, forced owner, stripped
 * fields).
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
                  - { name: paid,   type: decimal }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Account, kind: manyToOne, to: Account, leafOnly: true }
                  - { name: Status,  kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  # postings back-reference + the reversal's storno self-link (reverses fixture).
                  - { name: Doc,    kind: manyToOne, to: Doc }
                  - { name: Storno, kind: manyToOne, to: Entry }

              # postings source: PostDoc flips it POSTED (posting fires), VoidDoc flips it
              # CANCELLED (the reverses posting fires - red storno).
              - name: Doc
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

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

              # A master-detail (MANAGE_MASTER) entity carrying an EntityStatus: the master
              # layout must resolve the status FK to a label lookup and render it as a badge in
              # the table column and the detail pane, exactly like the list layout does.
              - name: Campaign
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

              - name: CampaignNote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Campaign, kind: manyToOne, to: Campaign, composition: true, required: true }

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
                  - { name: totalCost, type: decimal }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }
                  # a plain dropdown relation: the personal LIST must resolve it to a label (the
                  # my-list FK-lookup emission), while the owner relation gets no lookup at all
                  - { name: Unit, kind: manyToOne, to: Unit }

              - name: ClaimLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: cost,   type: decimal, sensitive: true }
                  - { name: day,    type: date }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim, composition: true }

              # documentItemsLayout: chat - the document master's line-items child renders as a
              # conversation thread (x-h-chat bubbles + a composer) instead of the editable table;
              # the body maps to the messageBody field, author/timestamp to the child's audit columns.
              # The personal owner makes it a personal root too: the PERSONAL document must render
              # the SAME chat thread (never the generic items table), through the personal items
              # controller.
              - name: Ticket
                function: Document
                documentItemsLayout: chat
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, length: 200 }
                relations:
                  - { name: Agent, kind: manyToOne, to: Person, personal: true }
              - name: TicketMessage
                function: DocumentItem
                audit: true
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: body,     type: text, messageBody: true }
                  - { name: internal, type: boolean, messageInternal: true }
                relations:
                  - { name: Ticket, kind: manyToOne, to: Ticket, composition: true, required: true }

              # view: range + a personal owner - the PERSONAL surface must render the range
              # calendar (never the plain form+list), scoped to the MyController (U3 parity).
              - name: Leave
                view: range
                calendar: { start: fromDate, end: toDate }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: fromDate, type: date, required: true }
                  - { name: toDate, type: date, required: true }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, personal: true }

              # partner: the EXTERNAL-partner mirror of personal - PartnerTicket is owned by a Person
              # (reusing identity: email; the admin seed maps the IT user), with a sensitive field.
              - name: PartnerTicket
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, length: 200 }
                  - { name: secret,  type: decimal, sensitive: true }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, partner: true }

              # BPM events wave 1 (wait + boundary timers): an RFQ whose flow escalates a stale
              # review (timeout), expires past its validity date (expire), and after review parks
              # until a NON-internal reply arrives (wait via the child's back-reference).
              - name: Rfq
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: title,      type: string, length: 200 }
                  - { name: state,      type: string, length: 20 }
                  - { name: validUntil, type: date }
                relations:
                  - { name: replies, kind: oneToMany, to: RfqReply }
              - name: RfqReply
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: text,     type: string, length: 200 }
                  - { name: internal, type: boolean }
                relations:
                  - { name: Rfq, kind: manyToOne, to: Rfq, composition: true, required: true }

              # BPM events wave 2 (abortOn): an approval whose confirm task is cancelled the moment
              # the record is voided via the CancelApproval transition (reusing the EntryStatus seeds:
              # DRAFT 1 / CANCELLED 3). Closes the orphaned-Inbox-task hole.
              - name: Approval
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

            # auto-sensitive derivation: totalCost sums the SENSITIVE ClaimLine.cost into the
            # personal-rooted Claim - the parser must mark the target sensitive automatically
            # (the leak class where the leaf is scrubbed but its total travels the my wire).
            # totalCost is NOT authored sensitive on purpose.
            rollups:
              - { name: claimCost, entity: ClaimLine, via: Claim, field: totalCost, op: sum, of: cost }

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

              # wait + boundary timers: review escalates after 2s (non-cancelling - the task stays),
              # expires when validUntil passes (cancelling - the task is withdrawn), and once
              # reviewed the flow parks until a non-internal reply resumes it.
              - name: RfqFlow
                trigger: { onCreate: Rfq }
                steps:
                  - name: review
                    kind: userTask
                    args:
                      assignee: reviewer
                      timeout: { after: PT2S, then: escalate }
                      expire: { until: validUntil, then: markExpired }
                      next: awaitReply
                  - { name: escalate,    kind: serviceTask, args: { setField: state, value: ESCALATED, next: end } }
                  - { name: markExpired, kind: serviceTask, args: { setField: state, value: EXPIRED, next: end } }
                  - { name: awaitReply,  kind: wait, args: { onCreate: RfqReply, via: Rfq, when: "internal == false", next: markReplied } }
                  - { name: markReplied, kind: serviceTask, args: { setField: state, value: REPLIED, next: end } }
                  - { name: end, kind: end }

              # abortOn: voiding the approval (CancelApproval -> status 3) cancels the confirm task.
              - name: ApprovalFlow
                trigger: { onCreate: Approval }
                abortOn: { status: [3] }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: approver } }
                  - { name: end, kind: end }

            # transitions: the guarded on-demand status flip - Cancel is allowed only on a DRAFT
            # entry with nothing paid (Calc semantics: a null field reads as 0, so a never-paid
            # entry passes).
            transitions:
              - name: CancelEntry
                forEntity: Entry
                from: [1]
                setStatus: 3
                when: "Paid == 0"
                label: Cancel
                icon: ban
              - name: PostDoc
                forEntity: Doc
                from: [1]
                setStatus: 2
                label: Post
                icon: check
              - name: VoidDoc
                forEntity: Doc
                from: [2]
                setStatus: 3
                label: Void
                icon: ban
              - name: CancelApproval
                forEntity: Approval
                from: [1]
                setStatus: 3
                label: Cancel
                icon: ban

            # postings + reverses (red storno): a POSTED Doc posts one balanced Entry (debit +
            # credit); a VOIDED Doc posts the reversal - the SAME lines negated on the SAME sides,
            # linked to the original through Entry.Storno, fail-soft when nothing was posted.
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
                event: { onTransition: Doc, when: "Status == 3" }
                reverses: docPosting
                storno: Storno

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
                  - { id: 3, name: CANCELLED }
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
        // A workflow setter/writer persists via the TARGETED updateProperty/updateProperties write - the
        // checks-bearing repository must OVERRIDE it to still run the posting gate, so converting the
        // setter from a full-row merge to a targeted write did not silently drop the check (the
        // silent-degradation class this IT exists to catch).
        assertTrue(entryRepository.contains("public int updateProperties(") && entryRepository.contains("enforceChecks(entity)"),
                "a checks-bearing entity must enforce its document checks on the targeted updateProperties write path");

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

        // The master (MANAGE_MASTER) layout must resolve an EntityStatus FK exactly like the list
        // layout: a label lookup loaded on the page and a badge cell in the table (the raw-id
        // regression class: the lookup loop skipped DOCUMENT_STATUS widgets).
        String campaignMasterPage = contentOf("gen/emission/js/components/pages/Campaign/CampaignMasterPage.js");
        assertTrue(campaignMasterPage.contains("all['Status']"),
                "the master page must load the EntityStatus label lookup like any dropdown relation");
        String campaignMasterView = contentOf("gen/emission/views/Campaign/Campaign-master.html");
        assertTrue(campaignMasterView.contains("statusVariant(lookupText('Status', row.Status))"),
                "the master table must render the EntityStatus column as a resolved badge, not a raw id");

        // Every list surface exports/prints its rows: the manage list and the master list emit the
        // column metadata + toolbar actions wired to the shared basePage CSV/print helpers, with
        // cells resolved like the table (FK labels, formatted dates - never raw ids).
        String unitManageList = contentOf("gen/emission/js/components/pages/Settings/UnitManageListPage.js");
        assertTrue(unitManageList.contains("exportRowsCsv(this.sortedItems"),
                "the manage list must export its filtered+sorted rows as CSV");
        assertTrue(unitManageList.contains("printRows(this.sortedItems"), "the manage list must print its filtered+sorted rows");
        String unitManageView = contentOf("gen/emission/views/Settings/Unit-manage-list.html");
        assertTrue(unitManageView.contains("defaults.export") && unitManageView.contains("printList()"),
                "the manage list toolbar must carry the Export and Print actions");
        assertTrue(campaignMasterPage.contains("exportRowsCsv(this.filteredMasters"),
                "the master list must export its filtered rows as CSV");
        assertTrue(campaignMasterView.contains("defaults.export") && campaignMasterView.contains("printList()"),
                "the master toolbar must carry the Export and Print actions");

        // personal: the ADDITIONAL scoped controller exists, resolves the current user through the
        // identity entity's repository, and scrubs the sensitive field from responses.
        String claimMy = contentOf("gen/emission/api/claim/ClaimMyController.java");
        assertTrue(claimMy.contains("eq(\"Email\", username)"), "personal must emit the identity match against the logged-in username");
        assertTrue(claimMy.contains("entity.Rate = null"), "sensitive must emit the response scrub in the personal controller");
        assertTrue(claimMy.contains("entity.Person = me"), "personal must force the owner FK server-side on create");
        // Auto-sensitive derivation (U5 class): totalCost is NOT authored sensitive, but it sums the
        // sensitive ClaimLine.cost into the personal-rooted Claim - the parser must propagate the
        // flag so the total is scrubbed from the personal wire exactly like the leaf value.
        assertTrue(claimMy.contains("entity.TotalCost = null"),
                "a rollup target summing a sensitive child field into a personal-rooted entity must be auto-scrubbed");
        String lineMy = contentOf("gen/emission/api/claim/ClaimLineMyController.java");
        assertTrue(lineMy.contains("requireMyParent"),
                "a composition child must inherit the personal scope as an ancestor-ownership guard");
        assertTrue(lineMy.contains("entity.Cost = null"),
                "a sensitive field on a scope-inheriting child must be scrubbed from its personal controller");

        // assignee: personal - the BPMN assigns the task to the start-time-resolved owner and the
        // trigger listener seeds that variable from the identity mapping.
        String bpmn = contentOf("ClaimConfirm.bpmn");
        assertTrue(bpmn.contains("flowable:assignee=\"${__personalUser}\""),
                "assignee: personal must emit a per-user flowable:assignee, not a candidate group");
        String claimTrigger = contentOf("gen/events/ClaimConfirmTrigger.java");
        assertTrue(claimTrigger.contains("__personalUser"),
                "the trigger listener must seed the __personalUser variable from the identity mapping");

        // wait + boundary timers (BPM events wave 1): the catch event, the two boundary timers and
        // the loader/correlating glue must all be present - a missing piece degrades silently into a
        // process that parks forever or never times out.
        String rfqBpmn = contentOf("RfqFlow.bpmn");
        assertTrue(rfqBpmn.contains("<intermediateCatchEvent id=\"awaitReply\"") && rfqBpmn.contains("messageRef=\"RfqFlowAwaitReply\""),
                "the wait step must emit a message intermediate catch event");
        assertTrue(rfqBpmn.contains("<boundaryEvent id=\"reviewTimeout\" attachedToRef=\"review\" cancelActivity=\"false\">")
                && rfqBpmn.contains("<timeDuration>PT2S</timeDuration>"), "timeout must emit a non-cancelling boundary timer");
        assertTrue(
                rfqBpmn.contains("<boundaryEvent id=\"reviewExpire\" attachedToRef=\"review\" cancelActivity=\"true\">")
                        && rfqBpmn.contains("<timeDate>${__reviewExpireDate}</timeDate>"),
                "expire must emit a cancelling boundary timer armed from the loader variable");
        String waitHandler = contentOf("gen/events/RfqFlowAwaitReplyWait.java");
        assertTrue(waitHandler.contains("Process.correlateMessageEvent(carrier.ProcessId, \"RfqFlowAwaitReply\""),
                "the wait listener must correlate the message on the stamped ProcessId");
        assertTrue(waitHandler.contains("new RfqRepository().findById(entity.Rfq)"),
                "the wait listener must resolve the parked record through the via back-reference");
        String timerLoader = contentOf("gen/events/LoadRfqFlowReviewExpire.java");
        assertTrue(timerLoader.contains("execution.setVariable(\"__reviewExpireDate\", due)"),
                "the expire date loader must publish the variable the boundary timer arms from");

        // abortOn (wave 2): the interrupting event subprocess + the correlating listener.
        String approvalBpmn = contentOf("ApprovalFlow.bpmn");
        assertTrue(
                approvalBpmn.contains("<subProcess id=\"ApprovalFlowAbortHandler\"") && approvalBpmn.contains("triggeredByEvent=\"true\"")
                        && approvalBpmn.contains("isInterrupting=\"true\"") && approvalBpmn.contains("<terminateEventDefinition>"),
                "abortOn must emit an interrupting, terminating event subprocess");
        String abortHandler = contentOf("gen/events/ApprovalFlowAbort.java");
        assertTrue(
                abortHandler.contains("-transitioned") && abortHandler.contains("entity.Status == 3")
                        && abortHandler.contains("Process.correlateMessageEvent(entity.ProcessId, \"ApprovalFlowAbort\""),
                "the abort listener must match the status on -transitioned and correlate on the ProcessId");

        // personal UI (phase B): the my pages exist, the form never mentions the sensitive field,
        // and the SPA routes + sidebar carry the personal surface.
        String myList = contentOf("gen/emission/js/components/pages/my/ClaimMyListPage.js");
        assertTrue(myList.contains("ClaimMyController"), "the my list page must talk to the scoped controller only");
        // The personal list must resolve relation columns to labels exactly like the power list
        // (the raw-FK-id regression class) - and never fetch a lookup for the owner relation,
        // which is not rendered on the personal surface at all.
        assertTrue(myList.contains("all['Unit']"), "the my list must load the label lookup for a rendered relation column");
        assertTrue(!myList.contains("all['Person']"), "the my list must not fetch a lookup for the personal-owner relation");
        // The personal list exports/prints the OWN rows through the same shared helpers - the
        // sensitive/owner columns are already absent from its column set, so they never export.
        assertTrue(myList.contains("exportRowsCsv(this.items"), "the personal list must export the own rows as CSV");
        String myListView = contentOf("gen/emission/views/my/Claim-list.html");
        assertTrue(myListView.contains("defaults.export") && myListView.contains("printList()"),
                "the personal list toolbar must carry the Export and Print actions");
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

        // partner: the EXTERNAL-partner surface - an ADDITIONAL scoped controller (identity match +
        // forced owner FK + sensitive strip) and a perspective on the DISJOINT Partner-shell point.
        String partnerController = contentOf("gen/emission/api/partnerticket/PartnerTicketPartnerController.java");
        assertTrue(partnerController.contains("eq(\"Email\", username)"),
                "partner must emit the identity match against the logged-in username");
        assertTrue(partnerController.contains("entity.Person = me"), "partner must force the owner FK server-side on create");
        assertTrue(partnerController.contains("entity.Secret = null"), "sensitive must be stripped in the partner controller");
        String partnerPerspective = contentOf("gen/emission/perspectives/partner/PartnerTicket/perspective.extension");
        assertTrue(partnerPerspective.contains("application-partner-perspectives"),
                "the partner perspective must register on the Partner shell's DISJOINT extension point");
        assertTrue(!partnerPerspective.contains("application-personal-perspectives"),
                "the partner perspective must NOT register on the personal point (disjoint by construction)");
        String partnerList = contentOf("gen/emission/js/components/pages/partner/PartnerTicketPartnerListPage.js");
        assertTrue(partnerList.contains("PartnerTicketPartnerController"),
                "the partner list page must talk to the scoped partner controller");
        assertTrue(spaIndex.contains("/partner/PartnerTicket"), "the SPA must route the partner pages");
        assertTrue(partnerList.contains("exportRowsCsv(this.items"), "the partner list must export the own rows as CSV");
        String partnerListView = contentOf("gen/emission/views/partner/PartnerTicket-list.html");
        assertTrue(partnerListView.contains("defaults.export") && partnerListView.contains("printList()"),
                "the partner list toolbar must carry the Export and Print actions");

        // collection-driven generation: the job creates the parent AND its per-working-day children.
        String job = contentOf("gen/events/MonthlyClaimsJob.java");
        assertTrue(job.contains("savedTarget"), "the scheduled generation must save the parent and keep its id for the children");
        assertTrue(job.contains("getDayOfWeek"), "a days child must iterate the working days of the month");
        assertTrue(job.contains("ClaimLineRepository"), "the child rows must be saved through the child's repository");

        // documentItemsLayout: chat - the .model marker is resolved (body property from the child's
        // messageBody field), and the Harmonia document view + page render the items pane as an
        // x-h-chat thread with an append-message composer instead of the editable table.
        String intentModel = contentOf("emission.model");
        assertTrue(intentModel.contains("\"documentItemsLayout\": \"chat\""), "documentItemsLayout: chat must reach the .model");
        assertTrue(intentModel.contains("\"chatBodyProperty\": \"Body\""), "the chat body property must be resolved into the .model");
        assertTrue(intentModel.contains("\"chatInternalProperty\": \"Internal\""),
                "the chat internal-flag property must be resolved into the .model");
        // The thread is composed from shipped Harmonia primitives (a role="log" bubble list + a
        // textarea composer bound to chatDraft) - the x-h-chat component is a later swap-in (TODO in
        // the template), so assert the primitives that render the chat, not that directive.
        String ticketDoc = contentOf("gen/emission/views/Ticket/Ticket-document.html");
        assertTrue(ticketDoc.contains("role=\"log\""),
                "documentItemsLayout: chat must emit the conversation thread (role=log) into the document view");
        assertTrue(ticketDoc.contains("x-model=\"chatDraft\""),
                "documentItemsLayout: chat must emit the message composer into the document view");
        String ticketPage = contentOf("gen/emission/js/components/pages/Ticket/TicketDocumentPage.js");
        assertTrue(ticketPage.contains("sendMessage"), "the chat document page must emit the append-message composer handler");
        // The PERSONAL document of a chat entity renders the SAME thread + composer (never the
        // generic line-items table), writing through the personal items controller so ownership is
        // enforced server-side (the my-document chat parity class).
        String myTicketDoc = contentOf("gen/emission/views/my/Ticket-document.html");
        assertTrue(myTicketDoc.contains("role=\"log\""),
                "the personal document of a chat entity must render the conversation thread, not the items table");
        assertTrue(myTicketDoc.contains("x-model=\"chatDraft\""), "the personal document must carry the message composer");
        String myTicketPage = contentOf("gen/emission/js/components/pages/my/TicketMyDocumentPage.js");
        assertTrue(myTicketPage.contains("sendMessage") && myTicketPage.contains("TicketMessageMyController"),
                "the personal chat composer must append through the personal items controller");

        // view: range/calendar + personal - the personal surface renders the calendar (never the
        // plain form+list), reads through the scoped controller, and /my/<Entity> lands on it.
        String myLeaveCalendar = contentOf("gen/emission/js/components/pages/my/LeaveMyCalendarPage.js");
        assertTrue(myLeaveCalendar.contains("LeaveMyController"), "the personal calendar must read through the scoped controller");
        String myLeaveView = contentOf("gen/emission/views/my/Leave-calendar.html");
        assertTrue(myLeaveView.contains("x-h-calendar"),
                "the personal surface of a range/calendar root must render the calendar, not a plain list");
        String shellIndex = contentOf("gen/emission/index.html");
        assertTrue(shellIndex.contains("x-template.target.app=\"./views/my/Leave-calendar.html\""),
                "/my/<Entity> must land on the personal calendar for a calendar root");

        // transitions: the server half is a controller that guards the source status + the when
        // guard (409) and flips ONLY the status column via the targeted updateProperty; the client
        // half is a custom-action contribution carrying the endpoint.
        String transition = contentOf("gen/events/CancelEntryTransition.java");
        assertTrue(transition.contains("currentStatus == 1"), "transitions must emit the allowed-statuses guard");
        assertTrue(transition.contains("Calc.eval(\"Paid\", source, 6)"), "the when guard must emit a Calc comparison");
        assertTrue(transition.contains("Response.setStatus(409)"), "a failed guard must surface as 409");
        assertTrue(transition.contains("updateProperty"), "the status flip must be the targeted single-column write");
        assertTrue(transition.contains("-transitioned"), "the flip must publish the -transitioned topic");
        String transitionExtension = contentOf("CancelEntry-transition-action.extension");
        assertTrue(transitionExtension.contains("-custom-action"),
                "the transition button must contribute to the app's custom-action extension point");

        // postings reverses: the reversal handler negates the sibling's amount expressions on the
        // SAME side, locates the original through the empty storno link (fail-soft skip when none)
        // and stamps the link; the sibling's idempotency guard symmetrically excludes linked rows.
        String stornoPosting = contentOf("gen/events/DocStornoPosting.java");
        assertTrue(stornoPosting.contains("Calc.eval(\"-(Amount)\", source, 2)"),
                "the reversal must negate the sibling's amount expression on the same side");
        assertTrue(stornoPosting.contains("nothing to reverse"), "the reversal must skip fail-soft when the source was never posted");
        assertTrue(stornoPosting.contains("target.Storno = original.Id;"), "the reversal must stamp the storno link to the original");
        String basePosting = contentOf("gen/events/DocPostingPosting.java");
        assertTrue(basePosting.contains("candidate.Storno == null"), "the reversed posting's idempotency guard must exclude reversal rows");

        // label: the repository recomputes the stored display Name on every write path.
        String claimRepository = contentOf("gen/emission/data/claim/ClaimRepository.java");
        assertTrue(claimRepository.contains("computeName"), "label must emit the display-name computation into the repository");
        assertTrue(claimRepository.contains("related.Name"),
                "a one-hop label token must load the related record and read its display property");
        // A workflow setter/writer targeted write keeps the stored display Name current: the label
        // repository OVERRIDES updateProperties to recompute it on that path too.
        assertTrue(claimRepository.contains("public int updateProperties(") && claimRepository.contains("computeName(entity)"),
                "a label entity must recompute its display Name on the targeted updateProperties write path");
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

        // transitions: a fresh DRAFT entry cancels (200, status CANCELLED)...
        String transitionRun = "/services/java/" + PROJECT + "/gen/events/CancelEntryTransition/run";
        AtomicInteger cancellable = new AtomicInteger();
        restAssuredExecutor.execute(() -> cancellable.set(given().contentType("application/json")
                                                                 .body("{\"Date\":\"2026-01-16\",\"Account\":2}")
                                                                 .when()
                                                                 .post(API + "/entry/EntryController")
                                                                 .then()
                                                                 .statusCode(200)
                                                                 .extract()
                                                                 .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + cancellable.get() + "}")
                                                 .when()
                                                 .post(transitionRun)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(3)));
        // ...a second cancel is rejected from the wrong status (409, record untouched)...
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + cancellable.get() + "}")
                                                 .when()
                                                 .post(transitionRun)
                                                 .then()
                                                 .statusCode(409));
        // ...and the when guard rejects a DRAFT entry with something paid, leaving it DRAFT.
        AtomicInteger guarded = new AtomicInteger();
        restAssuredExecutor.execute(() -> guarded.set(given().contentType("application/json")
                                                             .body("{\"Date\":\"2026-01-16\",\"Account\":2,\"Paid\":100}")
                                                             .when()
                                                             .post(API + "/entry/EntryController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + guarded.get() + "}")
                                                 .when()
                                                 .post(transitionRun)
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryController/" + guarded.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1)));

        // postings: posting a Doc creates the balanced Entry (async handler - poll)...
        AtomicInteger doc = new AtomicInteger();
        restAssuredExecutor.execute(() -> doc.set(given().contentType("application/json")
                                                         .body("{\"Date\":\"2026-01-17\",\"Amount\":250}")
                                                         .when()
                                                         .post(API + "/doc/DocController")
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + doc.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/PostDocTransition/run")
                                                 .then()
                                                 .statusCode(200));
        AtomicInteger originalEntry = new AtomicInteger();
        restAssuredExecutor.execute(() -> originalEntry.set(given().when()
                                                                   .get(API + "/entry/EntryController")
                                                                   .then()
                                                                   .statusCode(200)
                                                                   .body("findAll { it.Doc == " + doc.get()
                                                                           + " && it.Storno == null }.size()", equalTo(1))
                                                                   .extract()
                                                                   .path("find { it.Doc == " + doc.get() + " && it.Storno == null }.Id")),
                30);
        // ...and reverses: voiding the Doc creates the red storno - the SAME lines negated on the
        // SAME sides, linked to the original through the storno self-relation.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + doc.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/VoidDocTransition/run")
                                                 .then()
                                                 .statusCode(200));
        AtomicInteger reversalEntry = new AtomicInteger();
        restAssuredExecutor.execute(() -> reversalEntry.set(given().when()
                                                                   .get(API + "/entry/EntryController")
                                                                   .then()
                                                                   .statusCode(200)
                                                                   .body("findAll { it.Doc == " + doc.get() + " && it.Storno == "
                                                                           + originalEntry.get() + " }.size()", equalTo(1))
                                                                   .extract()
                                                                   .path("find { it.Doc == " + doc.get() + " && it.Storno == "
                                                                           + originalEntry.get() + " }.Id")),
                30);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Entry == " + reversalEntry.get()
                                                         + " && it.Debit != null && it.Debit < 0 }.size()", equalTo(1))
                                                 .body("findAll { it.Entry == " + reversalEntry.get()
                                                         + " && it.Credit != null && it.Credit < 0 }.size()", equalTo(1)));

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

        // partner: the partner controller scopes to the logged-in partner (admin -> Person 1); a
        // created PartnerTicket comes back with the owner forced and the sensitive field stripped.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Subject\":\"help\",\"Secret\":99}")
                                                 .when()
                                                 .post(API + "/partnerticket/PartnerTicketPartnerController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Person", equalTo(1))
                                                 .body("Secret", nullValue()));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/partnerticket/PartnerTicketPartnerController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1)));
        // Partner shell: served + aggregates the published partner perspective (the disjoint point).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/partner/index.html")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-partner-perspectives")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(org.hamcrest.Matchers.containsString("emission-test-partner-PartnerTicket")),
                30);

        assertBpmEventsRuntime();
    }

    /**
     * BPM events wave 1, at the outermost layer: the PT2S timeout fires while the review task stays
     * claimable (non-cancelling); the parked wait ignores a guarded-out (internal) reply and resumes on
     * a matching one; and a past validity date withdraws the review task (cancelling expire). Every
     * assertion reads the state the boundary/wait BRANCH wrote - reachable only through the real
     * Flowable timer jobs and message correlation.
     */
    private void assertBpmEventsRuntime() {
        // Wave 2 abortOn first - fast, timer-free, so it is verified independently of the slow
        // timer-driven wave-1 scenarios below.
        assertAbortOnRuntime();

        String rfqApi = API + "/rfq/RfqController";
        String replyApi = API + "/rfq/RfqReplyController";

        // Scenario A: far-future validity - only the timeout can fire.
        AtomicInteger rfqA = new AtomicInteger();
        restAssuredExecutor.execute(() -> rfqA.set(given().contentType("application/json")
                                                          .body("{\"Title\":\"quote A\",\"ValidUntil\":\"9999-12-01\"}")
                                                          .when()
                                                          .post(rfqApi)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("Id")));

        // The PT2S timeout escalates the stale review (the async executor's timer job fired). The
        // poll is generous (Flowable's single async executor acquires timer jobs on a cycle, and a
        // fresh-DB cold first-timer plus the other processes sharing the executor push first-fire
        // latency well past a naive PT2S on a loaded CI box - a broken timer never fires at all, so
        // the wide window cannot mask a logic bug)...
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(rfqApi + "/" + rfqA.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("State", equalTo("ESCALATED")),
                180);

        // ...while the review task STAYS claimable (non-cancelling) - find and complete it.
        AtomicReference<String> reviewTaskId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> tasks = given().when()
                                                     .get("/services/inbox/tasks?type=groups")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            Map<String, Object> review = tasks.stream()
                                              .filter(task -> "Review".equals(task.get("name")))
                                              .findFirst()
                                              .orElseThrow(() -> new AssertionError(
                                                      "the non-cancelling timeout must leave the review task claimable, got: " + tasks));
            reviewTaskId.set(String.valueOf(review.get("id")));
        }, 30);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\"}")
                                                 .when()
                                                 .post("/services/inbox/tasks/" + reviewTaskId.get())
                                                 .then()
                                                 .statusCode(200));

        // Parked at the wait now. A guarded-out (internal) reply must NOT resume it...
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Text\":\"internal note\",\"Internal\":true,\"Rfq\":" + rfqA.get() + "}")
                                                 .when()
                                                 .post(replyApi)
                                                 .then()
                                                 .statusCode(200));
        sleep(2000);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(rfqApi + "/" + rfqA.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("State", equalTo("ESCALATED")));

        // ...and a matching (non-internal) reply resumes the flow into the markReplied branch.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Text\":\"customer answer\",\"Internal\":false,\"Rfq\":" + rfqA.get() + "}")
                                                 .when()
                                                 .post(replyApi)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(rfqApi + "/" + rfqA.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("State", equalTo("REPLIED")),
                180);

        // Scenario B: the validity date already passed - the cancelling expire withdraws the review
        // task (the date the loader re-read at task entry) and the flow continues at markExpired.
        AtomicInteger rfqB = new AtomicInteger();
        restAssuredExecutor.execute(() -> rfqB.set(given().contentType("application/json")
                                                          .body("{\"Title\":\"quote B\",\"ValidUntil\":\"2020-01-01\"}")
                                                          .when()
                                                          .post(rfqApi)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("Id")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(rfqApi + "/" + rfqB.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("State", equalTo("EXPIRED")),
                180);
        // The cancelled review task is gone - no Review task remains in the inbox.
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> tasks = given().when()
                                                     .get("/services/inbox/tasks?type=groups")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            boolean reviewLeft = tasks.stream()
                                      .anyMatch(task -> "Review".equals(task.get("name")));
            assertTrue(!reviewLeft, "the cancelling expire must withdraw the review task, got: " + tasks);
        }, 30);
    }

    /**
     * BPM events wave 2 ({@code abortOn}), at the outermost layer: create an Approval -> its confirm
     * task appears -> void it via the CancelApproval transition -> the interrupting event subprocess
     * cancels the confirm task (the orphaned-Inbox-task hole closed) and the record carries the
     * CANCELLED status. Uses no async timer (a trigger + a JMS-correlated abort), so it is fast and
     * deterministic - run it BEFORE the timer-driven wave-1 scenarios so it verifies independently.
     */
    private void assertAbortOnRuntime() {
        String approvalApi = API + "/approval/ApprovalController";
        AtomicInteger approval = new AtomicInteger();
        restAssuredExecutor.execute(() -> approval.set(given().contentType("application/json")
                                                              .body("{\"Note\":\"abort me\"}")
                                                              .when()
                                                              .post(approvalApi)
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .path("Id")));
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> tasks = given().when()
                                                     .get("/services/inbox/tasks?type=groups")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            assertTrue(tasks.stream()
                            .anyMatch(task -> "Confirm".equals(task.get("name"))),
                    "the approval confirm task must appear, got: " + tasks);
        }, 90);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + approval.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/CancelApprovalTransition/run")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(approvalApi + "/" + approval.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(3)));
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> tasks = given().when()
                                                     .get("/services/inbox/tasks?type=groups")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            boolean confirmLeft = tasks.stream()
                                       .anyMatch(task -> "Confirm".equals(task.get("name")));
            assertTrue(!confirmLeft, "abortOn must cancel the confirm task when the approval is voided, got: " + tasks);
        }, 90);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("interrupted while waiting for the guarded-out reply window", ex);
        }
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
