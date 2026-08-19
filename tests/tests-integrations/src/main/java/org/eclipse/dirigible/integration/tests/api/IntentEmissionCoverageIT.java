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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
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
 * Covered here: {@code immutableWhen} / {@code immutable} (409 on write/delete, and the lock
 * inherited by a composition child - a line of a locked document is refused while a child that
 * declared {@code locksWithMaster: false} keeps its writes), {@code checks} (exactlyOne / itemsMin
 * / itemsSumEqual), {@code hierarchy}/{@code leafOnly}, {@code multilingual} (the read-time overlay
 * on an entity read, and its SQL counterpart on a report grouping by that nomenclature - the two
 * must agree on the same value in the same language), seed rows carrying a RELATION column,
 * aggregate totals, first-class {@code number:} stamping from an authored {@code .numbers} series
 * declaration, {@code transitions} (the guarded on-demand status flip: allowed-status 200,
 * wrong-status/guard 409), {@code lifecycle} (the declarative state machine: the graph walked
 * through its transitions, an unmodeled flip and a create filed mid-lifecycle both refused through
 * the plain REST surface no transition guard covers), {@code postings} with {@code reverses} (post
 * on a transition; red-storno reversal on void - negated amounts, storno link, fail-soft), the
 * {@code notify} block with {@code attach: print} (send the document itself by e-mail - on a
 * transition and on a process step; the fail-soft contract), {@code calculatedActionOnCreate} on a
 * to-one RELATION (the FK resolved server-side by a hand-written {@code custom/} action: assigned
 * in the repository, and at runtime both defaulted when omitted and left alone when the caller
 * supplied one), the event-driven {@code generates} (posting the source mints the whole document
 * with nobody clicking, and a click afterwards returns that same document - the at-most-once
 * back-reference guard), and the personal (my) surface
 * ({@code identity}/{@code personal}/{@code sensitive}: scoped reads, forced owner, stripped
 * fields).
 */
@Tag("slow")
class IntentEmissionCoverageIT extends IntegrationTest {

    private static final String PROJECT = "emission-test";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/emission/api";
    /** A standalone report owns its own gen folder, named after the report file. */
    private static final String REPORT_API = "/services/java/" + PROJECT + "/gen/claimsbyunit/api/reports";
    /** What a {@code type: text} field's column is sized to (EdmIntentGenerator's TEXT_LENGTH). */
    private static final int TEXT_COLUMN_LENGTH = 4000;

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
                  - { name: packPrice, type: decimal }
                  - { name: unitPrice, type: decimal }

              # hierarchy: the self-relation forms the tree; leafOnly references below must reject
              # a parent node server-side.
              - name: Account
                hierarchy: Parent
                # related: the reverse of an incoming to-one association. Account is the TARGET of
                # Entry.Account, so its own record page lists the entries booked against it -
                # read-only, because an Entry has its own lifecycle, its own page and its own
                # process. Declared HERE, on the referenced side, which is the only side that can
                # know: a referencing model may be generated later and never sees this one.
                related:
                  - entity: Entry
                    label: Journal Entries
                    show: [date, debit, credit, Status]
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
                relations:
                  - { name: Parent, kind: manyToOne, to: Account }

              # calculatedActionOnCreate on a to-one RELATION: the FK is resolved SERVER-SIDE at
              # insert by a hand-written action, for the "default read off ANOTHER record" case no
              # other hook covers (init: is a literal seed id; dependsOn is a UI-only cascade that
              # never fires on a server-side create). Tariff carries a `base` flag the action looks
              # the default up by - deliberately NOT the first row, so the assertion below cannot
              # pass on an accidental default landing in the column.
              - name: Tariff
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
                  - { name: base, type: boolean }

              - name: Quote
                imports: |
                  import custom.QuoteTariffAction;
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Tariff, kind: manyToOne, to: Tariff, calculatedActionOnCreate: QuoteTariffAction }

              # Append-only (immutable: true): e.g. the snapshot stored when a document is sent -
              # user writes and deletes are rejected from the moment a record is created.
              - name: Snapshot
                immutable: true
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: payload, type: string, length: 500 }

              # The document master: immutable once POSTED (status 2), post gated by document checks.
              # history: the trail has to separate what a person did (the create, the status hop)
              # from what the application did (the totals recomputed when a line was added).
              - name: Entry
                history: true
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
                  - { name: Doc,     kind: manyToOne, to: Doc }
                  - { name: Storno,  kind: manyToOne, to: Entry }
                  # the onCreate posting's back-reference (a lifecycle-less source).
                  - { name: Payment, kind: manyToOne, to: Payment }

              # postings source: PostDoc flips it POSTED (posting fires), VoidDoc flips it
              # CANCELLED (the reverses posting fires - red storno).
              #
              # lifecycle: the WHOLE legal status graph, enforced by the repository on every status
              # write - the two transitions above are exactly its two edges, and anything else (a
              # REST write jumping DRAFT straight to CANCELLED, a create filed as POSTED) is refused
              # whoever attempts it.
              - name: Doc
                lifecycle:
                  edges:
                    - { from: DRAFT,  to: [POSTED] }
                    - { from: POSTED, to: [CANCELLED] }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  # postings source-FK copy (#6533): this counterparty FK is copied onto the posted
                  # line, so an auto-posted Entry line carries the subledger dimension.
                  - { name: Party,  kind: manyToOne, to: Party }

              # postings onCreate source (#6421): a booked payment has NO status lifecycle -
              # its INSERT is the posting event.
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal }

              # postings source-FK-copy counterparty (#6533): a plain nomenclature copied by FK id.
              - name: Party
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              # entity-level unique (#6763): the business key spanning more than one column. The
              # schema gains the composite constraint over (PARTY, CODE) and a colliding write is
              # answered with the authored message rather than a server error.
              - name: PartyCode
                unique:
                  - { fields: [party, code], message: "This code is already registered for the party" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, required: true, length: 50 }
                relations:
                  - { name: party, kind: manyToOne, to: Party, required: true }

              # first-class numbering, stampOn: create - the generated DAO allocates the real number
              # at insert from the tenant series that the module's AUTHORED .numbers artefact
              # provisions at publish. The intent references the series by NAME only; the shape
              # (prefix + total width) lives in .numbers and the per-tenant settings.
              - name: Receipt
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Emission Receipt, stampOn: create } }
                  - { name: note,   type: string, length: 200 }

              - name: EntryLine
                checks:
                  - { kind: exactlyOne, fields: [debit, credit], message: "Exactly one of debit/credit" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal }
                  - { name: credit, type: decimal }
                  # #6336 on a document ITEM: the pattern must reach the item-dialog column metadata.
                  - { name: reference, type: string, length: 20, pattern: '^[A-Z]{3}-[0-9]{4}$' }
                  # conditional dependsOn (#6358): the copied Unit property is picked by the open
                  # document header's account name (a header-started classifier path).
                  - name: price
                    type: decimal
                    dependsOn:
                      relation: Unit
                      valueFrom:
                        by: Entry.Account.name
                        cases: { Assets: packPrice, Cash: unitPrice }
                        default: unitPrice
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
                  - { name: Unit,  kind: manyToOne, to: Unit }
                  # copied from Doc.Party by docPosting, and carried UNCHANGED onto the storno line (#6533).
                  - { name: Party, kind: manyToOne, to: Party }

              # A master-detail (MANAGE_MASTER) entity carrying an EntityStatus: the master
              # layout must resolve the status FK to a label lookup and render it as a badge in
              # the table column and the detail pane, exactly like the list layout does.
              - name: Campaign
                immutableWhen: "Status == 2"
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

              # locksWithMaster: false - the deliberate post-lock collection (#6700). It is the
              # negative control for the inherited lock below: notes keep their writes after the
              # campaign froze, while EntryLine (which says nothing) inherits the lock.
              - name: CampaignNote
                locksWithMaster: false
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
                  # The language this person's documents are rendered in (drives the notify languageFrom).
                  - { name: locale, type: string, length: 5 }
                  # #6336: an input-format regex must survive Generate and be enforced server-side.
                  # Deliberately NOT on `email` - that is the identity and holds the USERNAME (`admin`).
                  - { name: contactEmail, type: string, length: 320, pattern: '^[^@]+@[^@]+\\.[a-z]{2,}$' }

              # period is a month field: YYYY-MM string storage, month-picker widget on EVERY
              # writable surface (power + my), a |format label token rendering "2026 July", and
              # the schedule's `Period: now` below emitting the string shape (not LocalDate).
              # audit: the act-as assertions below prove a delegated write carries the ACTING
              # owner while CreatedBy keeps the REAL user.
              # history: the shadow EMISSION_CLAIM_HISTORY trail. Claim carries BOTH interactions the
              # keyword has to get right - it is audited (the audit columns must stay OUT of the
              # tracked property list, they say what the history row already says) and it owns a
              # sensitive field (so the personal surface must expose no history endpoint at all).
              - name: Claim
                audit: true
                history: true
                label: "{note} ({Person.name}) {period|yyyy MMMM}"
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                  - { name: period, type: month }
                  - { name: rate, type: decimal, sensitive: true }
                  - { name: totalCost, type: decimal }
                  # visibleTo: role-scoped on EVERY surface - stripped from the responses and
                  # ignored on the writes of the power controller AND of the personal one (owning
                  # the record is not a role), kept out of the change trail, and left off the
                  # generated pages. Deliberately on the entity that also carries `sensitive:` and
                  # `history:`, where the three could collide.
                  - { name: bonus, type: decimal, visibleTo: [Payroll] }
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

              # personalReadOnly: a see-only personal surface - the owner reads their own Balance
              # rows but the my controller's writes 403 (a record the back office grants, the
              # person must never author - the self-grant guard).
              - name: Balance
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: days, type: decimal }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true, personalReadOnly: true }

              # A composition child of the see-only personal root: the child inherits the read-only
              # scope, so the parent's my FORM renders its panel for reading but must offer no Add -
              # an affordance whose every use the child's own my controller refuses.
              - name: BalanceEntry
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Balance, kind: manyToOne, to: Balance, composition: true, required: true }

              # The see-only personal surface in its DOCUMENT shape (a payslip the employee may read
              # and never author): the same personalReadOnly flag must strip the item Add/Delete, the
              # item dialog's Save and the Save/Delete footer from the my-document view too.
              - name: Payslip
                function: Document
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 20, function: DocumentTitle }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true, personalReadOnly: true }
              - name: PayslipLine
                function: DocumentItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Payslip, kind: manyToOne, to: Payslip, composition: true, required: true }

              # The dead-Create family (found live 2026-07-29): a USER-ENTERED document title
              # (function: DocumentTitle without a number series) must render as an editable input
              # on the create page, and a required relation with init: (a DB-level default) must
              # not be demanded by create validation on any layer - the database guarantees the
              # value and the create echo must carry it.
              - name: Voucher
                function: Document
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: refNumber, type: string, required: true, length: 50, function: DocumentTitle }
                  - { name: date,      type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1, required: true }
                  # The event-driven create-from's back-reference (#6711): what makes minting the
                  # voucher at-most-once, and the only way the generated code recognizes its own output.
                  - { name: Slip, kind: manyToOne, to: Slip }
              - name: VoucherLine
                function: DocumentItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Voucher, kind: manyToOne, to: Voucher, composition: true, required: true }

              # Source of the computed create-from below (#6555): a Voucher is generated from a Slip
              # with ONE synthetic line whose cells are expressions over the Slip (not a 1:1 mirror).
              # It carries a status lifecycle so the SAME create-from can also be fired by a
              # transition of the Slip instead of a click (#6711).
              - name: Slip
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

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

              # view: calendar on a NON-ITEM composition child of a personal DOCUMENT master - the
              # document surface renders it as an embedded calendar panel (power secondary panel +
              # my-document children), and the relation title resolves through a label lookup.
              - name: TicketVisit
                view: calendar
                calendar: { start: visitDate, title: Person }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: visitDate, type: date, required: true }
                relations:
                  - { name: Ticket, kind: manyToOne, to: Ticket, composition: true, required: true }
                  - { name: Person, kind: manyToOne, to: Person }

              # view: range + a personal owner - the PERSONAL surface must render the range calendar
              # (scoped to the MyController, U3 parity). Since #6547 the calendar is an ADDITIONAL page
              # on BOTH shells: the entity keeps its own layout + form and browses at <Entity>/list,
              # while the calendar owns the landing route.
              - name: Leave
                view: range
                calendar: { start: fromDate, end: toDate, title: Person }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: fromDate, type: date, required: true }
                  - { name: toDate, type: date, required: true }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, personal: true }

              # view: slots on a DOCUMENT master (#6547) - the picker is an additional page: the document
              # layout survives it (slot-click creates a document), and the document list stays reachable
              # at /<Entity>/list.
              - name: Visit
                function: Document
                view: slots
                slots: { start: startsAt, open: "09:00", close: "17:00", step: 15 }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: startsAt, type: timestamp, required: true }
              - name: VisitItem
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
                relations:
                  - { name: Visit, kind: manyToOne, to: Visit, composition: true, required: true }

              # view: calendar on the document's LINE-ITEMS child (#6482) - the document's items PANE
              # renders as the calendar (a day-grained line belongs on one), instead of the calendar
              # markup being emitted and then filtered out of the secondary panels. The document layout
              # itself (header, totals, Print) is untouched. The personal owner makes it a personal root
              # too, so the PERSONAL document surface is covered by the same fixture.
              - name: Roster
                function: Document
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: reference, type: string, length: 40 }
                  - { name: hours,     type: decimal, precision: 18, scale: 2, aggregate: true }
                relations:
                  - { name: Owner, kind: manyToOne, to: Person, personal: true }
              - name: RosterItem
                function: DocumentItem
                view: calendar
                calendar: { start: day, title: Person, initialView: month }
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: day,   type: date, required: true }
                  - { name: hours, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Roster, kind: manyToOne, to: Roster, composition: true, required: true }
                  - { name: Person, kind: manyToOne, to: Person }

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

              # Step resilience (#6762): the tenant-provisioning shape - a flaky remote call retried
              # on a declared cycle, a produced secret consumed downstream and cleared, an exhausted
              # failure routed to a recorded {error} message.
              - name: Provision
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: title,          type: string, length: 200 }
                  - { name: generatedKey,   type: string, length: 100 }
                  - { name: failureMessage, type: string, length: 500 }

              # The non-HTTP inbound arrivals (#6537) ingest into an entity of their own: an ingested
              # record must not start a process, or the queue/file scenarios would seed extra Inbox
              # tasks the RFQ scenarios pick up by name.
              - name: Signal
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }

              # expansions: the generated child set is OWNED by the expansion, so the master's DELETE
              # has to take it with it (#6821). Nothing else would - a foreign key never becomes a
              # database constraint on this platform - so the rows would otherwise outlive the record
              # and keep counting. Asserted at runtime, both halves: the rows appear on create and are
              # gone once the master is deleted.
              - name: Retainer
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: note,      type: string, length: 200 }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                  - { name: fee,       type: decimal, required: true }
                  - { name: periods,   type: integer, readOnly: true }

              - name: RetainerPeriod
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: dueDate, type: date }
                  - { name: amount,  type: decimal }
                relations:
                  - { name: Retainer, kind: manyToOne, to: Retainer, composition: true, required: true }

              # BPM events wave 2 (abortOn): an approval whose confirm task is cancelled the moment
              # the record is voided via the CancelApproval transition (reusing the EntryStatus seeds:
              # DRAFT 1 / CANCELLED 3). Closes the orphaned-Inbox-task hole.
              - name: Approval
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }

              # document totals: a MANAGE_DOCUMENT master whose aggregate field is also carried by its
              # line-items child gets the SYNCHRONOUS recompute in the generated DAO (not an async
              # roll-up). The recompute is triggered by a LINE change, so it must persist ONLY the total
              # columns - a full-row merge reverts a concurrent edit to any other master column.
              #
              # Bill doubles as the send-document fixture: it is a printable document (header +
              # DocumentItem child), it has a counterparty to mail one hop away (Person.email) and an
              # EntityStatus for the SendBill transition to flip - so a notify block with
              # `attach: print` is authored on a transition AND on a process step (below).
              - name: Bill
                function: Document
                # SENT (status 2) freezes the document: the assertions below add the line write that
                # would otherwise rewrite the totals the sent PDF was rendered from (#6695).
                immutableWhen: "Status == 2"
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string,  length: 200 }
                  - { name: amount, type: decimal, aggregate: true }
                  # expression-calculated from the aggregate: the document-totals recompute must
                  # refresh it on every line change, or it stays stale/null until a header save
                  # (the unpaid-invoice Balance printed empty).
                  - { name: balanceDue, type: decimal, calculatedOnCreate: "Amount", calculatedOnUpdate: "Amount" }
                relations:
                  - { name: Person, kind: manyToOne, to: Person }
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
              - name: BillLine
                function: DocumentItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
                  # an item-level to-one: the print feeder must feed it per row so an items-table
                  # column can render {{Unit}} (the label, translated) or {{Unit.Name}}
                  - { name: Unit, kind: manyToOne, to: Unit }
              # the RECIPIENT LIST of a bill: rows that are nobody's document - they only say who
              # gets the bill's own PDF (attach: recordPrint, one document to many recipients).
              - name: BillRecipient
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Bill,   kind: manyToOne, to: Bill, required: true }
                  - { name: Person, kind: manyToOne, to: Person }

              # keyed cross-entity aggregate: a signed ledger summed per (Person, Unit) into a
              # materialised total row keyed by the same two FKs. Ledger.amount is SENSITIVE and
              # LedgerTotal is personal-rooted, so the parser must also auto-scrub LedgerTotal.total
              # (the rollup leak class, one entity further out - `total` is NOT authored sensitive).
              - name: Ledger
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, sensitive: true }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }
                  - { name: Unit,   kind: manyToOne, to: Unit }
                checks:
                  # guard, outcome block (the default): the write FAILS when the recomputed keyed sum
                  # would fall below the minimum, and only while the config key says "true".
                  - kind: guard
                    aggregate: ledgerTotal
                    minimum: 0
                    message: Insufficient balance
                    enabledBy: EMISSION_BLOCK_NEGATIVE_LEDGER
              - name: LedgerTotal
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }
                  - { name: Unit,   kind: manyToOne, to: Unit }

              # the two NON-BLOCKING guard outcomes on one source: the write is persisted either way,
              # `task` stamps the boolean marker a process decision branches on (park it on a hold
              # step), `reject` files the record with the rejected status (EntryStatus seed 3).
              - name: Booking
                fields:
                  - { name: id,              type: integer, primaryKey: true, generated: true }
                  - { name: days,            type: decimal }
                  - { name: withinAllowance, type: boolean }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true }
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                checks:
                  - kind: guard
                    aggregate: bookingAllowance
                    minimum: 0
                    outcome: task
                    marker: withinAllowance
                    message: Over the allowance
                  - kind: guard
                    aggregate: bookingAllowance
                    minimum: 0
                    outcome: reject
                    setStatus: 3
                    message: No allowance left
              - name: BookingAllowance
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: remaining, type: decimal }
                relations:
                  - { name: Person, kind: manyToOne, to: Person, required: true }

              # manyToMany: materialised into the intermediate entity CourseTag - a real link table
              # with both foreign keys, edited as a detail grid of the course. The keyword used to
              # parse and generate NOTHING, which is why the link table + a round-tripping link row
              # are asserted here and not only in the parser's unit test.
              - name: Course
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                relations:
                  - { name: tags, kind: manyToMany, to: Tag }
              - name: Tag
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

            aggregates:
              - name: ledgerTotal
                of: Ledger
                op: sum
                sum: amount
                by: [Person, Unit]
                into: LedgerTotal
                field: total
              - name: bookingAllowance
                of: Booking
                op: sum
                sum: days
                by: [Person]
                into: BookingAllowance
                field: remaining

            # auto-sensitive derivation: totalCost sums the SENSITIVE ClaimLine.cost into the
            # personal-rooted Claim - the parser must mark the target sensitive automatically
            # (the leak class where the leaf is scrubbed but its total travels the my wire).
            # totalCost is NOT authored sensitive on purpose.
            rollups:
              - { name: claimCost, entity: ClaimLine, via: Claim, field: totalCost, op: sum, of: cost }

            expansions:
              - name: retainer-periods
                from: Retainer
                into: RetainerPeriod
                unit: month
                between: { start: startDate, end: endDate }
                map: { dueDate: period }
                spread: { total: fee, into: amount, round: 2 }
                count: periods

            # collection-driven generation: the monthly job creates one Claim per Person and,
            # under each, one ClaimLine per working day of the month (amount defaulted).
            schedules:
              - name: monthly-claims
                cron: "0 0 4 1 * *"
                entity: Person
                generate:
                  to: Claim
                  map: { Person: id }
                  defaults: { note: monthly, Period: now }
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

              # step resilience (#6762): the flaky call succeeds on its LAST declared attempt (the
              # delegate counts invocations, so GeneratedKey == KEY-3 pins the R<count+1> cycle),
              # the produced secret flows through `uses` into the writer and is cleared once it
              # completes, and the doomed call after the hold exhausts its single retry and routes
              # its FINAL attempt's message into the record via {error}.
              - name: ProvisionFlow
                trigger: { onCreate: Provision }
                vars:
                  - { name: apiKey, clearAfter: storeKey }
                steps:
                  - name: flakyCall
                    kind: serviceTask
                    args:
                      delegate: custom.FlakyProvisioner
                      produces: [apiKey]
                      retry: { count: 2, every: PT2S }
                      onError: recordFailure
                      next: storeKey
                  - name: storeKey
                    kind: serviceTask
                    args: { delegate: custom.ProvisionKeyWriter, uses: [apiKey], next: hold }
                  - name: hold
                    kind: userTask
                    args: { assignee: operator, next: doomedCall }
                  - name: doomedCall
                    kind: serviceTask
                    args:
                      delegate: custom.DoomedProvisioner
                      retry: { count: 1, every: PT2S }
                      onError: recordFailure
                      next: end
                  - name: recordFailure
                    kind: serviceTask
                    args: { setField: failureMessage, value: "{error}", next: end }
                  - { name: end, kind: end }

              # a SENDING step: the serviceTask's whole work is the mail about the trigger record.
              # No attach here (the transition below covers the attachment), and the Bills this test
              # creates carry no Person - so at runtime the delegate takes its no-recipient no-op
              # path, which is exactly what must not stall a flow.
              - name: BillFlow
                trigger: { onCreate: Bill }
                steps:
                  - name: mailBill
                    kind: serviceTask
                    args:
                      notify:
                        to: Person.email
                        subject: "Bill {note}"
                        body: "Dear {Person.name}, your bill totals {amount}."
                      next: shareBill
                  # the MIRROR of the per-row fan-out: the rows are only the recipient list, and the
                  # document is the BILL's - rendered once (and not at all when nobody is invited,
                  # which is the path this test's Bills take) and attached to every message.
                  - name: shareBill
                    kind: serviceTask
                    args:
                      notify:
                        forEach: BillRecipient
                        to: Person.email
                        subject: "Bill {record.note}"
                        # {recordUrl} in a fan-out links the ROW - what THIS message is about -
                        # while {record.note} above reads the anchor. Two different scopes, both
                        # explicit.
                        body: "Dear {Person.name}, the bill is attached. Your copy: {recordUrl}"
                        attach: recordPrint
                      next: end
                  - { name: end, kind: end }

            # The glue EVENT AXIS (#6537): a notification and an integration bound to a process STEP
            # rather than to an entity lifecycle event. The emitter inserted at the step boundary
            # publishes the RFQ on the step topic, and these ordinary listeners consume it - so the
            # emitter sits in the middle of the RfqFlow scenarios below: were it broken, the review
            # task would never appear and the timeout/expire/wait assertions would fail. The recipient
            # is a LITERAL so the send is really attempted and cannot succeed (no SMTP on this
            # instance) - the flow must run through the step regardless. The integration's URL comes
            # from a configuration key that is not set here, which is the listener's documented no-op.
            notifications:
              - name: rfqReviewPending
                event: { onStepReached: { process: RfqFlow, step: review } }
                to: ops@example.com
                subject: "RFQ {title} awaits review"
                body: "A reviewer must handle it."

            integrations:
              - name: pushRfqReplied
                event: { onStepCompleted: { process: RfqFlow, step: markReplied } }
                method: POST
                url: "@config:EMISSION_RFQ_WEBHOOK"

            # The non-HTTP inbound arrivals (#6537): the same JSON record, saved through the same
            # repository, arriving on a queue or dropped as a file into a polled folder.
            inbound:
              - { name: signalHook,  path: /signal, create: Signal }
              - { name: signalQueue, source: { queue: emission-signals }, create: Signal }
              - { name: signalDrop,  source: { folder: target/inbox-emission, cron: "0/2 * * * * ?" }, create: Signal }

            # The departure half (#6767): the same record leaving on a queue, as a DECLARED envelope
            # rather than the row as stored. The guard keeps an internal note off the wire, which is
            # the assertion that the `when` of the event axis reaches a publisher at all.
            outbound:
              - name: publishSignal
                event: { onCreate: Signal, when: "note != internal" }
                to: { queue: emission-signals-out }
                payload:
                  type: "signal.raised"
                  version: 1
                  messageId: "{uuid}"
                  tenantId: "{tenant}"
                  note: note

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
              # The transition the event-driven create-from below listens for (#6711): posting the slip
              # is what mints the voucher - the test never calls the create-from to get one.
              - name: PostSlip
                forEntity: Slip
                from: [DRAFT]
                setStatus: POSTED
                label: Post
                icon: check
              # send-document: the transition mails AFTER the flip commits, with the bill's own print
              # rendered to PDF and attached. The recipient is a LITERAL so the runtime always gets past
              # the recipient check and actually attempts the attachment - which cannot succeed on this
              # instance (no print template in the CMS, no SMTP configured). That is the point: the flip
              # must still return 200 with the status written. Fail-soft is the contract.
              - name: SendBill
                forEntity: Bill
                from: [1]
                setStatus: 2
                label: Send
                icon: mail
                notify:
                  to: ops@example.com
                  subject: "Bill {note}"
                  body: "Please find the bill attached. Open it here: {recordUrl}"
                  attach: print
                  # languageFrom: the counterparty decides the language the attached print renders in.
                  languageFrom: Person.locale

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
                  # Party: source-FK copy (#6533) - the debit line carries Doc.Party as its dimension.
                  - { debit: "Amount", Party: Party }
                  - { credit: "Amount" }
              - name: docStorno
                event: { onTransition: Doc, when: "Status == 3" }
                reverses: docPosting
                storno: Storno
              # onCreate (#6421): the lifecycle-less Payment posts on its INSERT - no status guard.
              - name: paymentPosting
                event: { onCreate: Payment }
                creates: Entry
                backReference: Payment
                map: { date: date }
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }

            # Computed create-from item lines (#6555): the list form of generates.items builds a fixed
            # synthetic line from EXPRESSIONS over the source Slip - a Calc amount, a {} interpolated
            # string, and a when guard - instead of mirroring a source child 1:1. The target items child
            # (VoucherLine) is resolved automatically. Enforcement lives in the generated Generate.java.
            #
            # It is ALSO event-driven (#6711): posting a Slip mints the Voucher with nobody clicking,
            # while `button: true` keeps the click as the second trigger - so both halves must share one
            # at-most-once guard (the Voucher.Slip back-reference the map writes). The status is named,
            # not numbered.
            generates:
              - name: voucher-from-slip
                from: Slip
                to: Voucher
                event: { onTransition: Slip, when: "Status == POSTED" }
                button: true
                map:
                  Slip: id
                defaults:
                  refNumber: "AUTO"
                  date: now
                items:
                  - note: "Slip {label}"
                    amount: "Total * 2"
                    when: "Total != 0"
              # Prompted create-from (#6685): a per-record action that collects the input the source
              # cannot derive (here: a manual line's note + amount) before creating a composition
              # child. The prompted values are posted with the source id and set on the target after
              # map/defaults; a missing required input answers 400 before anything is written.
              - name: add-voucher-line
                from: Voucher
                to: VoucherLine
                label: Add Manual Line
                map:
                  Voucher: id
                prompt:
                  - { field: note }
                  - { field: amount, required: true }

            # The roles the model issues - and the ones a `visibleTo:` field may name.
            permissions:
              - { role: Payroll, description: May see what people are paid }

            reports:
              # A report grouping by a MULTILINGUAL nomenclature: its label column must be translated
              # for the caller's Accept-Language exactly as the entity list page next to it is, or the
              # two disagree on the same value (dirigible #6544).
              - name: ClaimsByUnit
                source: Claim
                dimensions: [Unit]
                measures: ["count(*)"]

            seeds:
              - name: people
                entity: Person
                rows:
                  - { id: 1, name: Admin, email: admin }
                  - { id: 2, name: Other, email: other@example.com }
              # The base tariff is row 2, NOT row 1: a calculated FK that resolved to 1 could be a
              # coincidence (a first row, a stray default), one that resolves to 2 can only be the
              # action having run and matched on `base`.
              - name: tariffs
                entity: Tariff
                rows:
                  - { id: 1, name: Promotional, base: false }
                  - { id: 2, name: Standard,    base: true }
              - name: claims
                entity: Claim
                rows:
                  - { id: 1, note: mine,    rate: 50, Person: 1, Unit: 1 }
                  - { id: 2, note: foreign, rate: 70, Person: 2, Unit: 1 }
              - name: parties
                entity: Party
                rows:
                  - { id: 1, name: Acme }
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
    @Autowired
    private DataSourcesManager dataSourcesManager;

    /**
     * The module's series declaration - AUTHORED next to app.intent (like .roles), never generated; the
     * .numbers synchronizer provisions it per tenant at publish. Prefix ER- in a total width of 8 →
     * {@code ER-00001}.
     */
    private static final String NUMBERS_JSON = "{\"series\": [{\"name\": \"Emission Receipt\", \"prefix\": \"ER-\", \"size\": 8}]}";

    /**
     * The hand-written half of a calculated action: the contract is that the developer authors the
     * class under {@code custom/} (never {@code gen/}, which is wiped on regeneration) and the
     * generated repository calls it. Written here as the fixture's own source so the IT exercises the
     * REAL arrangement - an action resolved as a Spring bean that reads another table - rather than
     * asserting a call to a class that does not exist. A generated repository referencing a missing
     * class would fail the whole client-Java batch and with it every REST assertion in this gate.
     *
     * <p>
     * Returning an already-set value unchanged is part of the documented contract, not defensive
     * coding: the action runs on EVERY create, so without it a caller's explicit pick would be
     * overwritten. Both directions are asserted at runtime.
     */
    private static final String TARIFF_ACTION_JAVA = """
            package custom;

            import java.util.List;

            import org.eclipse.dirigible.components.data.store.java.repository.Criteria;
            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.db.CalculatedField;

            import gen.emission.data.quote.QuoteEntity;
            import gen.emission.data.tariff.TariffEntity;
            import gen.emission.data.tariff.TariffRepository;

            @Component
            public class QuoteTariffAction implements CalculatedField<Object, Integer> {

                @Override
                public Integer calculate(Object entity) {
                    QuoteEntity quote = (QuoteEntity) entity;
                    if (quote.Tariff != null) {
                        return quote.Tariff;
                    }
                    List<TariffEntity> base = new TariffRepository().findAll(Criteria.create()
                                                                                    .eq("Base", true));
                    return base.isEmpty() ? null : base.get(0).Id;
                }
            }
            """;

    /**
     * The flaky remote call of the resilience scenario: fails on its first two invocations and succeeds
     * on the third, producing the declared {@code apiKey} step data with the attempt count baked in -
     * so the record asserting {@code KEY-3} pins the declared retry cycle to exactly one initial
     * attempt plus {@code count: 2} further ones.
     */
    private static final String FLAKY_PROVISIONER_JAVA = """
            package custom;

            import java.util.concurrent.atomic.AtomicInteger;

            import org.flowable.engine.delegate.DelegateExecution;
            import org.flowable.engine.delegate.JavaDelegate;

            public class FlakyProvisioner implements JavaDelegate {

                private static final AtomicInteger ATTEMPTS = new AtomicInteger();

                @Override
                public void execute(DelegateExecution execution) {
                    int attempt = ATTEMPTS.incrementAndGet();
                    if (attempt < 3) {
                        throw new IllegalStateException("schema provisioning failed (attempt " + attempt + ")");
                    }
                    execution.setVariable("apiKey", "KEY-" + attempt);
                }
            }
            """;

    /**
     * The consumer of the produced step data: writes the {@code apiKey} process variable onto the
     * record through the generated repository's targeted write (the sanctioned system-column path).
     */
    private static final String PROVISION_KEY_WRITER_JAVA = """
            package custom;

            import org.flowable.engine.delegate.DelegateExecution;
            import org.flowable.engine.delegate.JavaDelegate;

            import gen.emission.data.provision.ProvisionRepository;

            public class ProvisionKeyWriter implements JavaDelegate {

                @Override
                public void execute(DelegateExecution execution) {
                    Object key = execution.getVariable("Id");
                    Object apiKey = execution.getVariable("apiKey");
                    if (!(key instanceof Number id) || apiKey == null) {
                        return;
                    }
                    new ProvisionRepository().updateProperty(id.intValue(), "GeneratedKey", apiKey.toString());
                }
            }
            """;

    /**
     * The always-failing call: its declared {@code retry: { count: 1 }} allows exactly two attempts, so
     * the {@code {error}} message the record ends with must be the SECOND attempt's - proving the
     * conversion fires precisely on the exhausted attempt, not the first.
     */
    private static final String DOOMED_PROVISIONER_JAVA = """
            package custom;

            import java.util.concurrent.atomic.AtomicInteger;

            import org.flowable.engine.delegate.DelegateExecution;
            import org.flowable.engine.delegate.JavaDelegate;

            public class DoomedProvisioner implements JavaDelegate {

                private static final AtomicInteger ATTEMPTS = new AtomicInteger();

                @Override
                public void execute(DelegateExecution execution) {
                    throw new IllegalStateException("partner registration refused (attempt " + ATTEMPTS.incrementAndGet() + ")");
                }
            }
            """;

    @Test
    void generated_code_contains_every_feature_enforcement_and_the_published_app_enforces_it() {
        writeIntent(INTENT_YAML);
        writeProjectFile("emission.numbers", NUMBERS_JSON);
        writeProjectFile("custom/QuoteTariffAction.java", TARIFF_ACTION_JAVA);
        writeProjectFile("custom/FlakyProvisioner.java", FLAKY_PROVISIONER_JAVA);
        writeProjectFile("custom/ProvisionKeyWriter.java", PROVISION_KEY_WRITER_JAVA);
        writeProjectFile("custom/DoomedProvisioner.java", DOOMED_PROVISIONER_JAVA);
        // Generate: the models AND the code from them, in one call - the production path. The engine
        // runs each recipe with the template and parameters the project's .settings declare, so a
        // parameter-gated producer (javaRuntime gates the leafOnly repository class, say) is exercised
        // exactly as it is for a developer.
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        // The intent engine runs the model-to-code recipes itself, in the same call: assert each one
        // succeeded instead of replaying them from here.
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }

        assertEmission();

        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        assertTextColumnKeepsItsDeclaredWidth();
        assertRuntimeEnforcement();
    }

    /**
     * A {@code type: text} field is a wide VARCHAR, and it has to still be one after the generated
     * entity registers against the table. The entity layer's Hibernate mapping creates or alters
     * columns from its own annotations, so it silently narrowed a text column to the mapping's default
     * (255) until the generated entity started declaring the same length as the table.
     */
    private void assertTextColumnKeepsItsDeclaredWidth() {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, connection.getSchema(), "EMISSION_TICKET_MESSAGE", "TICKET_MESSAGE_BODY")) {
            assertTrue(columns.next(), "the text column of the generated document-item table must exist");
            String typeName = columns.getString("TYPE_NAME");
            int size = columns.getInt("COLUMN_SIZE");
            assertTrue(DataTypeUtils.isCharacterType(typeName), "a text column must be a character one, was " + typeName);
            assertEquals(TEXT_COLUMN_LENGTH, size, "a text column must keep its declared width, was " + typeName + "(" + size + ")");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read the text column's metadata", ex);
        }
    }

    /** Layer 1: the enforcement TOKENS are present in the generated sources. */
    private void assertEmission() {
        String entryController = contentOf("gen/emission/api/entry/EntryController.java");
        assertTrue(entryController.contains("requireMutable"),
                "immutableWhen must emit the requireMutable gate in the entity's REST controller");
        String snapshotController = contentOf("gen/emission/api/snapshot/SnapshotController.java");
        assertTrue(snapshotController.contains("requireMutable") && snapshotController.contains("append-only"),
                "immutable: true must emit the unconditional append-only gate in the REST controller");
        // The immutability UI pre-check (GET /{id}/mutable): the controller exposes it and the
        // generated pages gate their Edit affordances on it - the form/document pages ask the
        // endpoint (so a directly typed /edit URL opens read-only), while the browse tables use
        // the baked status check per row (no per-row API call). The PUT/DELETE 409 stays the
        // authoritative guard.
        assertTrue(entryController.contains("/{id}/mutable") && entryController.contains("isMutable("),
                "immutableWhen must emit the GET /{id}/mutable pre-check endpoint in the REST controller");
        assertTrue(snapshotController.contains("/{id}/mutable"),
                "immutable: true must emit the GET /{id}/mutable pre-check endpoint in the REST controller");
        String entryFormPage = contentOf("gen/emission/js/components/pages/Entry/EntryFormPage.js");
        assertTrue(entryFormPage.contains("/mutable"),
                "the edit form page must ask the mutable pre-check so a direct /edit URL opens read-only");
        String entryMasterPage = contentOf("gen/emission/js/components/pages/Entry/EntryMasterPage.js");
        assertTrue(entryMasterPage.contains("isRowImmutable"),
                "the browse page must gate row Edit/Delete on the baked per-row immutability check");
        assertTrue(entryController.contains("must reference a leaf"),
                "leafOnly must emit the server-side children check in the REST controller");

        // related: the read-only register of the records REFERENCING Account. The registration is
        // emitted for the REFERENCED entity (the referencing one may be generated separately, in
        // another project), and it carries the resolved coordinates the panel calls: the source's
        // controller, the foreign key it filters by, and the lookup that turns its Status FK into a
        // label. The URL is built by the generation parameters, not by the intent generator - which
        // is exactly why the .model entry carries facts and this file carries paths.
        String accountRegister = contentOf("gen/emission/js/components/pages/Account/Account.related.js");
        assertTrue(accountRegister.contains("App.registerRelated('Account'"),
                "a related declaration must register under the REFERENCED entity, got: " + accountRegister);
        assertTrue(accountRegister.contains("entity: 'Entry'") && accountRegister.contains("label: 'Journal Entries'"),
                "the register must name its source entity and its authored heading, got: " + accountRegister);
        assertTrue(accountRegister.contains("fkProperty: 'Account'"),
                "the register must filter by the source's foreign key back to this entity, got: " + accountRegister);
        assertTrue(accountRegister.contains("apiPath: '" + API + "/entry/EntryController'"),
                "the register's controller URL must resolve to the source's own controller, got: " + accountRegister);
        assertTrue(accountRegister.contains("lookup: { url: '" + API + "/settings/EntryStatusController'"),
                "a relation column must carry the lookup its label resolves through, got: " + accountRegister);
        // show: picks the columns and their order - the generated identifier and the foreign key back
        // here are not among them.
        assertTrue(
                accountRegister.indexOf("name: 'Date'") < accountRegister.indexOf("name: 'Status'")
                        && !accountRegister.contains("name: 'Id'"),
                "the register's columns must be the authored ones, got: " + accountRegister);
        // ... and the referenced entity's own page renders them.
        String accountFormPage = contentOf("gen/emission/js/components/pages/Account/AccountFormPage.js");
        assertTrue(accountFormPage.contains("App.relatedFor('Account')"),
                "the referenced entity's form page must read its registers from the runtime registry");
        String accountForm = contentOf("gen/emission/views/Account/Account-form.html");
        assertTrue(accountForm.contains("relatedPanel(r, id)") && accountForm.contains("openRow(row)"),
                "the referenced entity's form must render one related panel per register, whose only row action opens the source record");

        // #6336 pattern: the server-side regex check and the client-side HTML pattern attribute.
        String personController = contentOf("gen/emission/api/person/PersonController.java");
        assertTrue(personController.contains(".matches(") && personController.contains("does not match the required pattern"),
                "a field pattern must emit the server-side regex check in the REST controller");
        String personForm = contentOf("gen/emission/views/Person/Person-form.html");
        assertTrue(personForm.contains("pattern=\""), "a field pattern must reach the form input as an HTML pattern attribute");
        // On a document item the dialog is metadata-driven, so the regex travels as a JS literal in
        // the detail register (backslash-safe) rather than as markup.
        String linePatternRegister = contentOf("gen/emission/js/components/pages/Entry/EntryLine.detail.js");
        assertTrue(linePatternRegister.contains("pattern: '^[A-Z]{3}-[0-9]{4}$'"),
                "an item field pattern must reach the item-dialog column metadata, got: " + linePatternRegister);

        // number: stampOn: create - the generated DAO must allocate from the DECLARED series by
        // name (the shape deliberately never appears in generated code - it is tenant data).
        String receiptRepository = contentOf("gen/emission/data/receipt/ReceiptRepository.java");
        assertTrue(receiptRepository.contains("DocumentNumbers.next(\"Emission Receipt\")"),
                "number: stampOn: create must emit the insert-time allocation from the named series into the repository");

        // calculatedActionOnCreate on a to-one RELATION: the FK must be assigned from the action in the
        // generated repository, exactly as for a field. This is the whole regression - a relation IS an
        // ordinary property in the .model, but only FIELDS used to carry the calculated attributes into
        // it, so the keyword parsed, validated and reached the .intent while the emitted repository
        // contained no assignment at all: the create stored null with every pipeline step green.
        // Asserting the .model attribute alone would NOT have caught it (that layer was fine), and
        // asserting the runtime alone could pass on a coincidental default - hence all three layers,
        // here and in assertRuntimeEnforcement.
        String quoteRepository = contentOf("gen/emission/data/quote/QuoteRepository.java");
        assertTrue(quoteRepository.contains("entity.Tariff = Beans.get(QuoteTariffAction.class).calculate(entity);"),
                "calculatedActionOnCreate on a to-one relation must assign the FK from the action in the repository, got: "
                        + quoteRepository);
        assertTrue(quoteRepository.contains("import custom.QuoteTariffAction;"),
                "the entity's imports: must be decoded into the generated repository so the action resolves by simple name");
        assertTrue(contentOf("emission.model").contains("\"calculatedActionOnCreate\": \"QuoteTariffAction\""),
                "the relation's calculated action must reach the .model property every downstream template reads");

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
        // #6695: the master's immutability reaches its LINES. A child declares no immutability of its
        // own, yet its writes recompute the master's totals - so without this the lock had an
        // unguarded back door through the child's controller, which the UI never offers but REST did.
        assertTrue(lineController.contains("requireMasterMutable"),
                "a composition child of an immutable master must emit the inherited lock into its REST controller");
        assertTrue(lineController.contains("EntryRepository masterRepository"),
                "the inherited lock must consult the MASTER's repository, got: " + lineController);
        // The document's own line items are the same story through a different layout - and it is the
        // one where the child literally resums the master (BillLineRepository -> BillRepository).
        assertTrue(contentOf("gen/emission/api/bill/BillLineController.java").contains("requireMasterMutable"),
                "a document ITEM of an immutable master must emit the inherited lock into its REST controller");
        // ...and the opt-out really opts out: the settlement-style collection keeps its writes.
        String campaignNoteController = contentOf("gen/emission/api/campaign/CampaignNoteController.java");
        assertFalse(campaignNoteController.contains("requireMasterMutable"),
                "locksWithMaster: false must leave the child's REST writes unguarded");

        String schema = contentOf("gen/emission/schema/" + PROJECT + ".schema");
        // entity-level unique (#6763): the composite key has to reach the schema, which is the only
        // place it can be created. Asserted here as well as at runtime so a regression says WHICH
        // layer dropped it.
        assertTrue(schema.contains("\"PartyCode_Party_Code\""), "the composite business key must be emitted into the schema: " + schema);
        String partyCodeController = contentOf("gen/emission/api/partycode/PartyCodeController.java");
        assertTrue(partyCodeController.contains("This code is already registered for the party"),
                "the generated controller must carry the authored conflict message");
        assertTrue(schema.contains("EMISSION_UNIT_LANG"), "multilingual must emit the _LANG translation table into the schema");
        // manyToMany: the link entity is an ordinary entity from parse time on, so it must reach the
        // schema as its own table and the REST layer as its own (detail) controller. Asserting the
        // parsed model alone is exactly what let the keyword generate nothing for so long.
        assertTrue(schema.contains("EMISSION_COURSE_TAG"), "manyToMany must emit the intermediate entity's table into the schema");
        String courseTagController = contentOf("gen/emission/api/course/CourseTagController.java");
        assertTrue(courseTagController.contains("Course") && courseTagController.contains("Tag"),
                "the link controller must carry both ends of the n:m, got: " + courseTagController);
        assertHistoryEmission(schema, entryRepository);
        String unitRepository = contentOf("gen/emission/data/settings/UnitRepository.java");
        assertTrue(unitRepository.contains("Translator"), "multilingual must emit the read-time translation overlay into the repository");

        // A report reading that nomenclature's label overlays it in SQL instead - the report backend
        // never loads an entity, so the Translator cannot reach it. The join is LEFT and the column a
        // COALESCE so an untranslated row keeps its base value, and the language is a bound parameter
        // (never interpolated) that the report repository fills from the request.
        String claimsByUnitReport = contentOf("ClaimsByUnit.report");
        assertTrue(claimsByUnitReport.contains(
                "LEFT JOIN \\\"EMISSION_UNIT_LANG\\\" as Unit_LANG ON Unit_LANG.\\\"Id\\\" = Unit.\\\"UNIT_ID\\\" AND Unit_LANG.\\\"Language\\\" = :language"),
                "a translated report dimension must join the language table for the request language: " + claimsByUnitReport);
        assertTrue(claimsByUnitReport.contains("COALESCE(Unit_LANG.\\\"Name\\\", Unit.\\\"UNIT_NAME\\\")"),
                "a translated report dimension must fall back to its base value: " + claimsByUnitReport);
        String claimsByUnitRepository = contentOf("gen/claimsbyunit/data/reports/ClaimsByUnitRepository.java");
        assertTrue(claimsByUnitRepository.contains("parameter(\"language\", \"VARCHAR\", language())"),
                "the report repository must bind the language parameter its query uses");
        assertTrue(claimsByUnitRepository.contains("User.getLanguage()"),
                "the report's language must come from the caller's Accept-Language, like every entity read");

        // The seed's RELATION key (Parent: 1) must survive into the CSV as the FK column - an
        // unknown/mis-cased key is dropped silently and CSVIM then skips the rows.
        String accountsCsv = contentOf("accounts.csv");
        assertTrue(accountsCsv.contains("ACCOUNT_PARENT"), "a seed row's relation key must emit the FK column into the seed CSV");
        assertTrue(entryRepository.contains("EntryLineRepository"),
                "aggregate: true must make the master repository recompute totals from its items child");

        // A DERIVED recompute (document totals, roll-up, keyed aggregate) must persist ONLY the columns it
        // computed. Merging the whole row back silently reverts a concurrent user write to any other column
        // of that row - the lost-update family fixed for the trigger write-back in #6226 and the workflow
        // setters/writers in #6306, of which the recompute path was the last member.
        String billRepository = contentOf("gen/emission/data/bill/BillRepository.java");
        assertTrue(billRepository.contains("totals.put(\"Amount\"") && billRepository.contains("super.updateProperties(id, totals)"),
                "the document-totals recompute must persist only the total columns, not merge the whole master row");
        assertFalse(billRepository.replaceAll("\\s+", " ")
                                  .contains("recalculate(entity); return super.update(entity);"),
                "the document-totals recompute must not fall back to the full-row write");
        // recalculate() also refreshes the header's EXPRESSION-calculated fields (balanceDue derives
        // from the aggregate amount) and persists them in the same targeted write - actions are
        // deliberately not run there.
        String billHeaderRepository = contentOf("gen/emission/data/bill/BillRepository.java");
        assertTrue(billHeaderRepository.contains("totals.put(\"BalanceDue\", entity.BalanceDue)"),
                "the totals recompute must persist the refreshed calculated field: " + billHeaderRepository);
        String billLineRepository = contentOf("gen/emission/data/bill/BillLineRepository.java");
        assertTrue(billLineRepository.contains("new BillRepository().recalculate("),
                "a line change must trigger the master's document-totals recompute");
        // ...and a line written by a TARGETED primitive is still a line change (#6822). A workflow
        // setField and any glue updateProperty / updateProperties / updateDerived land in
        // updateProperties, which used to write the column and leave the header summing to something
        // else than its lines. The guard is on the written columns, so only a write that actually
        // touches an aggregated column - or the FK, which MOVES the line to another document - pays
        // for the resum.
        assertTrue(billLineRepository.contains("values.containsKey(\"Amount\")"),
                "a targeted write of an aggregated column must resum the master: " + billLineRepository);
        assertTrue(billLineRepository.contains("Object documentBefore = entity == null ? null : entity.Bill;"),
                "the targeted write must capture the document the line belonged to before it: " + billLineRepository);
        assertTrue(
                billLineRepository.contains("new BillRepository().recalculate(documentAfter)")
                        && billLineRepository.contains("new BillRepository().recalculate(documentBefore)"),
                "a line moved between documents must resum both: " + billLineRepository);
        // The event-suppressed system write is the third targeted-ish path: it suppresses the event,
        // not the arithmetic.
        String updateWithoutEvent = billLineRepository.substring(billLineRepository.indexOf("public BillLineEntity updateWithoutEvent"));
        assertTrue(updateWithoutEvent.substring(0, updateWithoutEvent.indexOf("\n    }"))
                                     .contains("new BillRepository().recalculate("),
                "updateWithoutEvent must keep the master's totals in step: " + billLineRepository);

        // The roll-up handler records every column it recomputes and persists them through the targeted
        // derived write. The derived.put assertion is load-bearing in the other direction too: an EMPTY
        // derived map would make updateDerived a no-op and silently stop maintaining the roll-up.
        String claimRollup = contentOf("gen/events/emission/ClaimLineClaimRollupOnCreate.java");
        assertTrue(claimRollup.contains("derived.put(\"TotalCost\""),
                "a roll-up must record each recomputed column into the derived map it persists");
        assertTrue(claimRollup.contains("parents.updateDerived("), "a roll-up must persist through the targeted derived write");
        assertFalse(claimRollup.contains("parents.update(parent)"), "a roll-up must not merge the whole parent row back (lost update)");

        // The keyed aggregate handler writes the aggregate column of an EXISTING target row targeted. The
        // resolved primary key in the call also proves the descriptor's targetPk reached the template: an
        // unforwarded parameter renders literally and compiles into nothing usable (the #6306 countProperty
        // class of bug).
        String ledgerAggregate = contentOf("gen/events/emission/LedgerTotalAggregateOnCreate.java");
        assertTrue(ledgerAggregate.contains("targets.updateDerived(target.Id, derived)"),
                "a keyed aggregate must persist through the targeted derived write with a RESOLVED target pk");
        assertFalse(ledgerAggregate.contains("targets.update(target)"),
                "a keyed aggregate must not merge the whole materialised row back (lost update)");

        // A grouping-key change MOVES a row between tuples. The tuple it moved into is recomputed off
        // the "-updated" event; the tuple it LEFT has no event of its own, so the DAO publishes the
        // PREVIOUS row on "-rekeyed" (only when a key actually moved) and a fourth copy of the handler
        // recomputes that former tuple - otherwise it keeps the moved row's contribution forever.
        String ledgerRepository = contentOf("gen/emission/data/ledger/LedgerRepository.java");
        assertTrue(
                ledgerRepository.contains("aggregatePrevious = findById(entity.Id)")
                        && ledgerRepository.contains("java.util.Objects.equals(aggregatePrevious.Person, entity.Person)")
                        && ledgerRepository.contains("java.util.Objects.equals(aggregatePrevious.Unit, entity.Unit)"),
                "an aggregate source must compare EVERY grouping key against the previous row on update");
        assertTrue(
                ledgerRepository.contains("if (aggregateRekeyed)")
                        && ledgerRepository.contains("-rekeyed\", Json.stringify(aggregatePrevious)"),
                "a moved grouping key must publish the PREVIOUS row on the -rekeyed topic");
        String ledgerRekey = contentOf("gen/events/emission/LedgerTotalAggregateOnRekey.java");
        assertTrue(ledgerRekey.contains("-Ledger-rekeyed"), "the rekey handler must bind the source's -rekeyed topic");
        assertTrue(ledgerRekey.contains("targets.updateDerived("),
                "the rekey handler must repair the former tuple through the targeted derived write");

        // checks: kind: guard - the aggregate precondition, one assertion per outcome. The guard
        // recomputes the keyed sum from the GUARDED entity's own store (race-free, not the async
        // aggregate target), then acts. block fails the write behind its config gate; task and reject
        // both PERSIST the row and mark it instead of throwing.
        assertTrue(ledgerRepository.contains("Criteria.create().eq(\"Person\", entity.Person).eq(\"Unit\", entity.Unit)"),
                "a guard must recompute its aggregate over the incoming row's full key-tuple");
        assertTrue(ledgerRepository.contains("throw new ValidationException(\"Insufficient balance\")"),
                "outcome block must fail the write with the authored message");
        assertTrue(ledgerRepository.contains("Configurations.get(\"EMISSION_BLOCK_NEGATIVE_LEDGER\""),
                "enabledBy must wrap the guard in a config gate, so a tenant can turn it off");

        String bookingRepository = contentOf("gen/emission/data/booking/BookingRepository.java");
        assertTrue(bookingRepository.contains("entity.WithinAllowance = guardWithin"),
                "outcome task must stamp the boolean marker the process decision branches on");
        assertTrue(bookingRepository.contains("entity.Status = 3"), "outcome reject must force the authored EntityStatus seed id");
        assertFalse(
                bookingRepository.contains("throw new ValidationException(\"Over the allowance\")")
                        || bookingRepository.contains("throw new ValidationException(\"No allowance left\")"),
                "a non-blocking outcome must NOT fail the write - that is the whole point of task/reject");

        // The master (MANAGE_MASTER) layout must resolve an EntityStatus FK exactly like the list
        // layout: a label lookup loaded on the page and a badge cell in the table (the raw-id
        // regression class: the lookup loop skipped DOCUMENT_STATUS widgets).
        String campaignMasterPage = contentOf("gen/emission/js/components/pages/Campaign/CampaignMasterPage.js");
        assertTrue(campaignMasterPage.contains("all['Status']"),
                "the master page must load the EntityStatus label lookup like any dropdown relation");
        String campaignMasterView = contentOf("gen/emission/views/Campaign/Campaign-master.html");
        assertTrue(campaignMasterView.contains("statusVariant(lookupText('Status', row.Status))"),
                "the master table must render the EntityStatus column as a resolved badge, not a raw id");

        // #6693: a record that owns detail collections is not finished at create - its children are
        // the next step of the same working session and can only be added from the record's own
        // page, so the create lands there instead of on the list. A flat record has nothing left to
        // do on its page and still returns to the list.
        String campaignFormPage = contentOf("gen/emission/js/components/pages/Campaign/CampaignFormPage.js");
        assertTrue(
                campaignFormPage.contains("App.detailsFor('Campaign').length")
                        && campaignFormPage.contains("window.PineconeRouter.navigate('/Campaign/' + encodeURIComponent(newId) + '/edit')"),
                "a create on an entity with detail children must land on the record page, got: " + campaignFormPage);
        assertTrue(campaignFormPage.contains("!this.returnToParam() && App.detailsFor"),
                "an explicit returnTo must still win - the create was launched from somewhere expecting the user back");

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
        // act-as (delegated entry): the identity resolution reads the EFFECTIVE user, so an armed
        // acting identity redirects the personal surface while audit stamping stays on getName().
        assertTrue(claimMy.contains("User.getEffectiveName()"),
                "the personal identity resolution must read the effective (act-as aware) user");
        // Auto-sensitive derivation (U5 class): totalCost is NOT authored sensitive, but it sums the
        // sensitive ClaimLine.cost into the personal-rooted Claim - the parser must propagate the
        // flag so the total is scrubbed from the personal wire exactly like the leaf value.
        assertTrue(claimMy.contains("entity.TotalCost = null"),
                "a rollup target summing a sensitive child field into a personal-rooted entity must be auto-scrubbed");
        // Same class one entity further out: the keyed aggregate materialises the sum of the sensitive
        // Ledger.amount into LedgerTotal.total, which is personal-rooted too. The aggregates: keyword
        // arrived after the rollup propagation and was not covered by it.
        String ledgerTotalMy = contentOf("gen/emission/api/ledgertotal/LedgerTotalMyController.java");
        assertTrue(ledgerTotalMy.contains("entity.Total = null"),
                "an aggregates: target summing a sensitive source field into a personal-rooted entity must be auto-scrubbed");
        // visibleTo: the role allow-list reaches the runtime as the model's per-property read AND
        // write roles, and every generated surface enforces it - the whole point of the keyword is
        // that hiding the control alone would be cosmetic. Asserted on the artefacts because the
        // ITs run as a user local basic auth answers isInRole(true) for, so the redaction cannot be
        // provoked over HTTP here; the branch itself is covered by RoleScopedFieldControllerTemplateIT.
        assertTrue(contentOf("emission.model").contains("\"roleRead\": \"Payroll\""),
                "visibleTo must reach the .model as the property's read role, which every template reads");
        String claimController = contentOf("gen/emission/api/claim/ClaimController.java");
        assertTrue(claimController.contains("if (!isInAnyRole(\"Payroll\")) {") && claimController.contains("entity.Bonus = null;"),
                "visibleTo must emit the response redaction in the power controller, got: " + claimController);
        assertTrue(claimController.contains("@Get(\"/restricted\")"),
                "visibleTo must emit the field-visibility pre-check the generated pages ask");
        assertTrue(claimController.contains("entries.removeIf(entry -> hidden.contains("),
                "a role-scoped field must be dropped from the change trail too - it carries the same values");
        assertTrue(claimMy.contains("entity.Bonus = existing.Bonus;"),
                "the personal surface must keep the stored value of a field its caller may not write");
        String claimForm = contentOf("gen/emission/views/Claim/Claim-form.html");
        assertTrue(claimForm.contains("canSee('Bonus')"),
                "the generated form must gate the role-scoped input on the caller's own visibility answer");
        String claimFormPage = contentOf("gen/emission/js/components/pages/Claim/ClaimFormPage.js");
        assertTrue(claimFormPage.contains("loadRestrictedFields()"), "the generated page must ask which fields it may render");
        // The personal surface hides it too, from the columns its list builds in JS.
        String claimMyList = contentOf("gen/emission/js/components/pages/my/ClaimMyListPage.js");
        assertTrue(claimMyList.contains("this.columns.filter(c => this.canSee(c.name))"),
                "the personal list must drop the columns its own controller withholds");
        assertFalse(contentOf("gen/emission/views/Person/Person-form.html").contains("canSee("),
                "an entity with no role-scoped field must carry no visibility gate at all");

        String lineMy = contentOf("gen/emission/api/claim/ClaimLineMyController.java");
        assertTrue(lineMy.contains("requireMyParent"),
                "a composition child must inherit the personal scope as an ancestor-ownership guard");
        assertTrue(lineMy.contains("entity.Cost = null"),
                "a sensitive field on a scope-inheriting child must be scrubbed from its personal controller");

        // personalReadOnly: the scoped controller still serves reads but its write methods 403 -
        // no repository.save on the personal surface (the power controller keeps writing).
        String balanceMy = contentOf("gen/emission/api/balance/BalanceMyController.java");
        assertTrue(balanceMy.contains("read-only on your personal surface"),
                "personalReadOnly must emit the write refusal on the personal write methods");
        assertTrue(balanceMy.contains("HttpStatus.FORBIDDEN"), "personalReadOnly write methods must refuse with 403 FORBIDDEN");
        assertTrue(!balanceMy.contains("repository.save(entity)"),
                "personalReadOnly must NOT emit a persisting create/update on the personal controller");
        String balanceMyView = contentOf("gen/emission/views/my/Balance-list.html");
        assertTrue(!balanceMyView.contains("newEntity()"), "personalReadOnly my list must not render the New button");
        // ... and no write affordance survives on the my FORM either. The child panel's Add was the
        // last one left: the child controller refuses every use of it (403), so it was an affordance
        // that always failed on exactly the surface whose point is read-without-author.
        String balanceMyForm = contentOf("gen/emission/views/my/Balance-form.html");
        // The positive anchor first: the READ half must still be there, or the assertions below would
        // pass on an empty/missing file instead of on a rendered see-only form.
        assertTrue(balanceMyForm.contains("x-for=\"child in children\"") && balanceMyForm.contains("goBack()"),
                "personalReadOnly my form must still render the child panel and the Back button");
        assertTrue(!balanceMyForm.contains("addChild(child)"), "personalReadOnly my form must not render the child Add button");
        assertTrue(!balanceMyForm.contains("save()") && !balanceMyForm.contains("deleteOpen = true"),
                "personalReadOnly my form must not render Save or Delete");
        // The guard is conditional, not blanket: a WRITABLE personal form keeps its child Add.
        String claimMyForm = contentOf("gen/emission/views/my/Claim-form.html");
        assertTrue(claimMyForm.contains("addChild(child)"), "a writable personal form must still render the child Add button");
        // Same flag, DOCUMENT shape: items add/delete, the item dialog's Save and the Save/Delete
        // footer all go; Back stays. The read surface (the items table itself) is untouched.
        String payslipMyDoc = contentOf("gen/emission/views/my/Payslip-document.html");
        assertTrue(!payslipMyDoc.contains("openItem(null)") && !payslipMyDoc.contains("deleteItem(row)"),
                "personalReadOnly my document must not render the item Add or the per-row Delete");
        assertTrue(!payslipMyDoc.contains("saveItem()"), "personalReadOnly my document must not render the item dialog's Save");
        assertTrue(!payslipMyDoc.contains("save()") && !payslipMyDoc.contains("deleteOpen = true"),
                "personalReadOnly my document must not render the Save or Delete footer buttons");
        assertTrue(payslipMyDoc.contains("openItem(row)"), "personalReadOnly my document must still open an item for reading");
        // Positive control on the document shape too - the writable personal chat keeps its composer.
        String ticketMyDoc = contentOf("gen/emission/views/my/Ticket-document.html");
        assertTrue(ticketMyDoc.contains("sendMessage(chatDraft)"), "a writable personal document must still render the chat composer");

        // assignee: personal - the BPMN assigns the task to the start-time-resolved owner and the
        // trigger listener seeds that variable from the identity mapping.
        String bpmn = contentOf("ClaimConfirm.bpmn");
        assertTrue(bpmn.contains("flowable:assignee=\"${__personalUser}\""),
                "assignee: personal must emit a per-user flowable:assignee, not a candidate group");
        String claimTrigger = contentOf("gen/events/emission/ClaimConfirmTrigger.java");
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
        String waitHandler = contentOf("gen/events/emission/RfqFlowAwaitReplyWait.java");
        assertTrue(waitHandler.contains("Process.correlateMessageEvent(carrier.ProcessId, \"RfqFlowAwaitReply\""),
                "the wait listener must correlate the message on the stamped ProcessId");
        assertTrue(waitHandler.contains("new RfqRepository().findById(entity.Rfq)"),
                "the wait listener must resolve the parked record through the via back-reference");
        String timerLoader = contentOf("gen/events/emission/LoadRfqFlowReviewExpire.java");
        assertTrue(timerLoader.contains("execution.setVariable(\"__reviewExpireDate\", due)"),
                "the expire date loader must publish the variable the boundary timer arms from");

        // The glue event axis (#6537), step half: the emitter delegate is wired INTO the flow (before
        // the observed task, after the observed service task, carrying its routing), it publishes the
        // trigger record on the step topic, and the consumers bind to that exact topic. A wrong topic
        // on either side is the silent failure mode - the app runs and nobody is ever notified.
        assertTrue(
                rfqBpmn.contains("<serviceTask id=\"rfqFlowReviewReached\"")
                        && rfqBpmn.contains("<sequenceFlow id=\"flow_rfqFlowReviewReached_review\""),
                "onStepReached must insert its emitter right before the observed task");
        assertTrue(
                rfqBpmn.contains("<serviceTask id=\"rfqFlowMarkRepliedCompleted\"")
                        && rfqBpmn.contains("<sequenceFlow id=\"flow_markReplied_rfqFlowMarkRepliedCompleted\""),
                "onStepCompleted must insert its emitter right after the observed step");
        String reachedEmitter = contentOf("gen/events/emission/RfqFlowReviewReached.java");
        assertTrue(reachedEmitter.contains("execution.getVariable(\"Id\")") && reachedEmitter.contains("new RfqRepository()"),
                "the emitter must load the trigger record by the id in the process context");
        assertTrue(reachedEmitter.contains("-step-RfqFlow-review-reached") && reachedEmitter.contains("Process.executeAfterCommit"),
                "the emitter must publish the step topic after the chain commits");
        String stepNotification = contentOf("gen/events/emission/RfqReviewPendingNotification.java");
        assertTrue(stepNotification.contains("-step-RfqFlow-review-reached"),
                "a step-bound notification must bind to the topic its emitter publishes to");
        String stepIntegration = contentOf("gen/events/emission/PushRfqRepliedIntegration.java");
        assertTrue(stepIntegration.contains("-step-RfqFlow-markReplied-completed"),
                "a step-bound integration must bind to the topic its emitter publishes to");

        // The glue event axis, inbound half: the same ingest, three arrivals.
        String consumer = contentOf("gen/events/emission/SignalQueueConsumer.java");
        assertTrue(consumer.contains("return \"emission-signals\";") && consumer.contains("ListenerKind.QUEUE"),
                "a queue source must emit a MessageHandler bound to that queue");
        assertTrue(consumer.contains("Json.parse(message, SignalEntity.class)") && consumer.contains("new SignalRepository().save(entity)"),
                "a message ingest must save the record through the repository, like the webhook does");
        String fileImport = contentOf("gen/events/emission/SignalDropFileImport.java");
        assertTrue(fileImport.contains("return \"0/2 * * * * ?\";") && fileImport.contains("Paths.get(\"target/inbox-emission\")"),
                "a folder source must emit a JobHandler polling that folder on the declared cron");
        assertTrue(fileImport.contains("SignalEntity[].class") && fileImport.contains("Files.move"),
                "a file ingest must accept a batch and move every read file out of the drop folder");

        // The glue event axis, outbound half (#6767): the publisher subscribes to the record's own
        // event topic, guards, builds the DECLARED envelope and sends it on the named channel. A
        // publisher that forwarded the row instead would put a different contract on the wire.
        String publisher = contentOf("gen/events/emission/PublishSignalPublisher.java");
        assertTrue(publisher.contains("return \"emission-test-Signal-Signal\";") && publisher.contains("ListenerKind.TOPIC"),
                "a departure must subscribe to the topic the entity's repository publishes its create on");
        assertTrue(publisher.contains("!java.util.Objects.equals(entity.Note, \"internal\")"),
                "the event axis carries a when guard, and a departure must honour it");
        assertTrue(
                publisher.contains("payload.put(\"type\", \"signal.raised\")")
                        && publisher.contains("payload.put(\"messageId\", java.util.UUID.randomUUID().toString())")
                        && publisher.contains("payload.put(\"note\", entity.Note)"),
                "the declared envelope must be built key by key, in the authored order");
        assertTrue(publisher.contains("Producer.sendToQueue(\"emission-signals-out\", body)"),
                "the departure must send on exactly the channel the intent names");
        assertFalse(publisher.contains("throw new"),
                "a failed departure is logged, never fatal - the write it reacts to is already committed");

        // abortOn (wave 2): the interrupting event subprocess + the correlating listener.
        String approvalBpmn = contentOf("ApprovalFlow.bpmn");
        assertTrue(
                approvalBpmn.contains("<subProcess id=\"ApprovalFlowAbortHandler\"") && approvalBpmn.contains("triggeredByEvent=\"true\"")
                        && approvalBpmn.contains("isInterrupting=\"true\"") && approvalBpmn.contains("<terminateEventDefinition>"),
                "abortOn must emit an interrupting, terminating event subprocess");
        String abortHandler = contentOf("gen/events/emission/ApprovalFlowAbort.java");
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
                "the personal perspective must register on the Personal Shell's extension point");
        // The Personal Shell has a single navigation group, declared as the DEFAULT of its extension
        // point, so the shell owns the placement. A baked-in groupId would be a platform constant this
        // artifact must agree with forever - and the platform-side rename that invalidated it made every
        // personal page vanish silently (#6646).
        assertFalse(contentOf("gen/emission/perspectives/my/Claim/perspective.js").contains("groupId"),
                "a personal perspective must not bake in the shell's navigation group id");

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
        assertFalse(contentOf("gen/emission/perspectives/partner/PartnerTicket/perspective.js").contains("groupId"),
                "a partner perspective must not bake in the shell's navigation group id (#6646)");
        String partnerList = contentOf("gen/emission/js/components/pages/partner/PartnerTicketPartnerListPage.js");
        assertTrue(partnerList.contains("PartnerTicketPartnerController"),
                "the partner list page must talk to the scoped partner controller");
        assertTrue(spaIndex.contains("/partner/PartnerTicket"), "the SPA must route the partner pages");
        assertTrue(partnerList.contains("exportRowsCsv(this.items"), "the partner list must export the own rows as CSV");
        String partnerListView = contentOf("gen/emission/views/partner/PartnerTicket-list.html");
        assertTrue(partnerListView.contains("defaults.export") && partnerListView.contains("printList()"),
                "the partner list toolbar must carry the Export and Print actions");

        // collection-driven generation: the job creates the parent AND its per-working-day children.
        String job = contentOf("gen/events/emission/MonthlyClaimsJob.java");
        assertTrue(job.contains("savedTarget"), "the scheduled generation must save the parent and keep its id for the children");
        assertTrue(job.contains("getDayOfWeek"), "a days child must iterate the working days of the month");
        assertTrue(job.contains("ClaimLineRepository"), "the child rows must be saved through the child's repository");
        // type-aware now: a month field is a String on the generated entity, so the default must
        // render the YYYY-MM string - the untyped LocalDate.now() would not even compile.
        assertTrue(job.contains(".Period = java.time.YearMonth.now().toString()"),
                "a month field's `now` default must render the YYYY-MM string, not LocalDate");

        // month widget: the YYYY-MM field renders the Harmonia month picker on BOTH writable
        // surfaces - the power form and the personal form (my-shell parity).
        assertTrue(contentOf("gen/emission/views/Claim/Claim-form.html").contains("x-h-month-picker"),
                "a month field must render the Harmonia month picker on the power form");
        assertTrue(contentOf("gen/emission/views/my/Claim-form.html").contains("x-h-month-picker"),
                "a month field must render the Harmonia month picker on the personal form too");

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

        // view: calendar on a NON-ITEM composition child of a Document master - the document
        // surface renders the child as an embedded calendar panel on BOTH shells (the class where
        // the calendar was realized on the master layout but silently degraded to a table - or to
        // nothing - on the document layout), and a relation title resolves via the label lookup.
        String ticketDoc2 = contentOf("gen/emission/views/Ticket/Ticket-document.html");
        assertTrue(ticketDoc2.contains("def.calendar") && ticketDoc2.contains("x-h-calendar"),
                "the power document's secondary panels must render a calendar child as an embedded calendar");
        String visitRegister = contentOf("gen/emission/js/components/pages/Ticket/TicketVisit.detail.js");
        assertTrue(visitRegister.contains("calendar: {"), "the calendar child's detail registration must carry the calendar config");
        assertTrue(visitRegister.contains("lookup: {"),
                "the calendar child's relation columns must carry their label lookups (title resolution)");
        assertTrue(myTicketPage.contains("loadChildren") && myTicketPage.contains("TicketVisitMyController"),
                "the personal document must load its non-item children through their scoped controllers");
        assertTrue(myTicketPage.contains("titleLookup"),
                "the personal document's child calendar must resolve a relation title via the label lookup");
        assertTrue(myTicketDoc.contains("onChildEventClick"), "the personal document must render the child calendar panel markup");

        // view: range/calendar + personal - the personal surface renders the calendar, reads through
        // the scoped controller, and /my/<Entity> lands on it.
        String myLeaveCalendar = contentOf("gen/emission/js/components/pages/my/LeaveMyCalendarPage.js");
        assertTrue(myLeaveCalendar.contains("LeaveMyController"), "the personal calendar must read through the scoped controller");
        String myLeaveView = contentOf("gen/emission/views/my/Leave-calendar.html");
        assertTrue(myLeaveView.contains("x-h-calendar"),
                "the personal surface of a range/calendar root must render the calendar, not a plain list");
        String shellIndex = contentOf("gen/emission/index.html");
        assertTrue(shellIndex.contains("x-template.target.app=\"./views/my/Leave-calendar.html\""),
                "/my/<Entity> must land on the personal calendar for a calendar root");
        // A calendar title that names a RELATION must resolve to the referenced label like the
        // list columns do - never render the raw FK id as the event title (power and my alike).
        String leaveCalendar = contentOf("gen/emission/js/components/pages/Leave/LeaveCalendarPage.js");
        assertTrue(leaveCalendar.contains("titleLookup"), "the power calendar must resolve a relation title through a label lookup");
        assertTrue(myLeaveCalendar.contains("titleLookup"), "the personal calendar must resolve a relation title through a label lookup");

        // #6547: the calendar is an ADDITIONAL page - the entity's own layout survives it. The range
        // root above keeps its MANAGE list + form pages (the class where declaring a calendar silently
        // deleted the entity's editing surface), the calendar owns the landing route, and the layout's
        // browse page moves to /<Entity>/list with a switch between the two on both pages.
        assertTrue(shellIndex.contains("x-route=\"/Leave\" x-template.target.app=\"./views/Leave/Leave-calendar.html\""),
                "a calendar entity must land on its calendar");
        assertTrue(shellIndex.contains("x-route=\"/Leave/list\" x-template.target.app=\"./views/Leave/Leave-manage-list.html\""),
                "the entity's own browse page must stay reachable at /<Entity>/list");
        assertTrue(shellIndex.contains("x-route=\"/Leave/create\" x-template.target.app=\"./views/Leave/Leave-form.html\""),
                "the entity's own create/edit form must still be routed");
        assertTrue(shellIndex.contains("x-route=\"/my/Leave/list\" x-template.target.app=\"./views/my/Leave-list.html\""),
                "the personal list must stay reachable at /my/<Entity>/list");
        assertTrue(contentOf("gen/emission/views/Leave/Leave-manage-list.html").contains("goCalendar()"),
                "the entity's browse page must offer the switch to its calendar");
        assertTrue(contentOf("gen/emission/views/Leave/Leave-calendar.html").contains("goList()"),
                "the calendar must offer the switch back to the entity's own browse page");
        assertTrue(contentOf("gen/emission/views/my/Leave-list.html").contains("goCalendar()"),
                "the personal list must offer the switch to the personal calendar");

        // The slot picker follows the same additive rule: a DOCUMENT master declaring view: slots keeps
        // its document layout (slot-click creates a document, not a bare form), the document list stays
        // at /<Entity>/list, and the two browse pages switch to each other.
        assertTrue(shellIndex.contains("x-route=\"/Visit\" x-template.target.app=\"./views/Visit/Visit-slots.html\""),
                "a slots entity must land on its picker");
        assertTrue(shellIndex.contains("x-route=\"/Visit/list\" x-template.target.app=\"./views/Visit/Visit-manage-list.html\""),
                "the document list of a slots entity must stay reachable at /<Entity>/list");
        assertTrue(shellIndex.contains("x-route=\"/Visit/create\" x-template.target.app=\"./views/Visit/Visit-document.html\""),
                "slot-click must create through the DOCUMENT editor, not a plain form");
        assertTrue(contentOf("gen/emission/views/Visit/Visit-slots.html").contains("goList()"),
                "the picker must offer the switch to the entity's own browse page");
        assertTrue(contentOf("gen/emission/views/Visit/Visit-manage-list.html").contains("goSlots()"),
                "the document list must offer the switch back to the picker");

        // #6482: a document whose LINE-ITEMS child declares view: calendar renders the items PANE as
        // the calendar - on the power, personal and partner document surfaces alike. This is the
        // authored-but-unconsumed class: the markup used to be emitted and then filtered out by name.
        String rosterDoc = contentOf("gen/emission/views/Roster/Roster-document.html");
        assertTrue(rosterDoc.contains("x-h-calendar=\"itemsCalCfg\""), "the document's items pane must render as a calendar");
        assertTrue(rosterDoc.contains("onItemsEventClick") && rosterDoc.contains("onItemsDateClick"),
                "the items calendar must be wired to the line dialog (edit on event, add on empty day)");
        assertTrue(!rosterDoc.contains("x-for=\"col in tableColumns\""), "the items TABLE must be replaced, not rendered alongside");
        assertTrue(rosterDoc.contains("askDeleteDraft()"), "the line dialog must offer Delete - the calendar pane has no per-row menu");
        String rosterPage = contentOf("gen/emission/js/components/pages/Roster/RosterDocumentPage.js");
        assertTrue(rosterPage.contains("window.HarmoniaCalendar.events"), "the items events must come from the shared calendar mapping");
        assertTrue(rosterPage.contains("this.itemsDef.calendar"),
                "the items calendar must be configured from the child's detail registration, not baked at generation time");
        assertTrue(contentOf("gen/emission/js/components/pages/Roster/RosterItem.detail.js").contains("calendar: {"),
                "the items child's registration must carry the calendar config the document page reads");
        // The PERSONAL document surface renders the same items calendar through its scoped controller
        // (the partner document is the mechanical mirror of this one).
        String myRosterDoc = contentOf("gen/emission/views/my/Roster-document.html");
        assertTrue(myRosterDoc.contains("x-h-calendar=\"itemsCalCfg\""),
                "the personal document's items pane must render as a calendar too");
        String myRosterPage = contentOf("gen/emission/js/components/pages/my/RosterMyDocumentPage.js");
        assertTrue(myRosterPage.contains("window.HarmoniaCalendar.events") && myRosterPage.contains("RosterItemMyController"),
                "the personal items calendar must read through the scoped items controller");

        // The app-test manifest carries the personal UI-parity metadata the runner's my flow
        // drives (wave 2): the /my route, the layout family the personal page belongs to, and
        // the relation columns that must resolve to labels on the personal list.
        String testManifest = contentOf("emission.test");
        assertTrue(testManifest.contains("\"route\": \"#/my/Claim\""),
                "the manifest's personal block must carry the /my route for the UI-parity walk");
        assertTrue(testManifest.contains("\"fkColumns\"") && testManifest.contains("\"Unit\""),
                "the manifest must name the personal list's relation columns (label resolution targets)");
        assertTrue(testManifest.contains("\"layout\": \"document-chat\""),
                "a personal chat document must be flagged so the runner drives the composer round-trip");
        assertTrue(testManifest.contains("\"route\": \"#/my/Leave\""), "the calendar root's personal block must carry its /my route");

        // transitions: the server half is a controller that guards the source status + the when
        // guard (409) and flips ONLY the status column via the targeted updateProperty; the client
        // half is a custom-action contribution carrying the endpoint.
        String transition = contentOf("gen/events/emission/CancelEntryTransition.java");
        assertTrue(transition.contains("currentStatus == 1"), "transitions must emit the allowed-statuses guard");
        assertTrue(transition.contains("Calc.eval(\"Paid\", source, 6)"), "the when guard must emit a Calc comparison");
        assertTrue(transition.contains("Response.setStatus(409)"), "a failed guard must surface as 409");
        assertTrue(transition.contains("updateProperty"), "the status flip must be the targeted single-column write");
        assertTrue(transition.contains("-transitioned"), "the flip must publish the -transitioned topic");
        String transitionExtension = contentOf("CancelEntry-transition-action.extension");
        assertTrue(transitionExtension.contains("-custom-action"),
                "the transition button must contribute to the app's custom-action extension point");

        // lifecycle: the state machine is compiled into the REPOSITORY - the one choke point every
        // status write passes through - not into the transition controllers, which would leave every
        // other writer (a REST update, a workflow setter, a glue action) free to jump anywhere.
        String docRepository = contentOf("gen/emission/data/doc/DocRepository.java");
        assertTrue(docRepository.contains("\"1>2,2>3\".split(\",\")"), "the lifecycle must emit the whole legal edge set");
        assertTrue(docRepository.contains("1=DRAFT,2=POSTED,3=CANCELLED"),
                "the seeded status names must ride along so a rejection names statuses, not positional ids");
        assertTrue(docRepository.contains("enforceLifecycle(entity);"), "a full-row update must be validated against the graph");
        assertTrue(docRepository.contains("enforceLifecycleMove(lifecyclePrevious, entity.Status);"),
                "a targeted write (transition button, workflow setter) must be validated against the graph too");
        assertTrue(docRepository.contains("enforceLifecycleStart(entity.Status);"),
                "with a declared init, a record must not be created mid-lifecycle");

        // send a document by e-mail: the notify block, at two of its call sites.
        //
        // (1) On a TRANSITION - the mail goes out after the flip, with the record's own print
        // rendered to PDF and attached. The attachment must come from the generated feeder + the
        // server-side print renderer (never a hand-rolled payload), and the whole send must be
        // wrapped so a mail failure cannot fail the already-committed transition.
        String sendBill = contentOf("gen/events/emission/SendBillTransition.java");
        assertTrue(sendBill.contains("Mail.send("), "a transition's notify must emit the actual send call");
        assertTrue(sendBill.contains("\"type\", \"attachment\"") && sendBill.contains("application/pdf"),
                "attach: print must emit a PDF attachment part");
        assertTrue(sendBill.contains("Print.render(\"Bill\",") && sendBill.contains("new BillPrintFeeder().feed(entity.Id)"),
                "the attachment must be the generated feeder's payload rendered by the server-side print engine");
        // The render language is never hardcoded: languageFrom: Person.locale loads the counterparty
        // and reads it off the record, falling back to the first entry of the tenant-resolved
        // application language set when the chain is null or blank.
        assertFalse(sendBill.contains("Print.render(\"Bill\", \"en\""),
                "the attachment language must come from the languageFrom knob, not a hardcoded literal");
        assertTrue(sendBill.contains("new gen.emission.data.person.PersonRepository().findById(entity.Person)"),
                "languageFrom must load the language source off the record's FK");
        assertTrue(sendBill.contains("attachLanguageSource.Locale"), "the language must be read off the person's locale");
        assertTrue(sendBill.contains("org.eclipse.dirigible.sdk.print.Print.defaultLanguage()"),
                "a null/blank locale must fall back to the application language set at send time");

        // The feeder resolves the LINE-ITEM to-one relations per row - an items-table column renders
        // {{Unit}} (the target's label, through the repository so the translation overlay applies) or
        // descends into {{Unit.Name}} - with one load per DISTINCT key, not one per row.
        String billFeeder = contentOf("gen/events/emission/BillPrintFeeder.java");
        assertTrue(billFeeder.contains("itemUnitCache"), "item relation lookups are cached per distinct key: " + billFeeder);
        assertTrue(billFeeder.contains("new gen.emission.data.settings.UnitRepository().findById(item.Unit)"),
                "the item's Unit is loaded through its generated repository");
        assertTrue(billFeeder.contains("itemUnitMap.put(\"__label\", itemUnit.Name)"),
                "the item relation map carries the __label the binder renders for a bare {{Unit}}");
        assertTrue(billFeeder.contains("im.put(\"Unit\", itemUnitMap)"), "the map is hung under the relation's own key on the row");
        assertTrue(sendBill.contains("catch (Exception"), "a transition's mail must be fail-soft - the status flip has already committed");
        // {recordUrl}: the notification carries the link back to the record, and the ROUTE is composed
        // in the template - the only layer that knows it. This is also the compile proof: an
        // undeclared local would fail the whole client-Java batch, taking every REST assertion below
        // with it.
        assertTrue(sendBill.contains("\"/services/web/emission-test/gen/emission/index.html#/Bill/\" + entity.Id"),
                "the transition's mail must compose the record's deep link from the project, gen folder, entity and key");

        // (2) On a PROCESS STEP - a JavaDelegate whose work IS the message: it re-loads the trigger
        // record through the generated repository, resolves the recipient, and sends. The BPMN must
        // bind THAT class (a mismatch silently leaves the step calling a non-existent custom stub),
        // and no custom/ stub may be scaffolded for it.
        String billSend = contentOf("gen/events/emission/BillFlowMailBillSend.java");
        assertTrue(billSend.contains("implements JavaDelegate"), "a sending step must emit a JavaDelegate");
        assertTrue(billSend.contains("new BillRepository().findById("),
                "the sender must re-load the trigger record through its generated repository");
        // The recipient is a one-hop relation.field, so the related record must be loaded by FK
        // before its address can be read (the same mechanism a notification's recipient uses).
        assertTrue(billSend.contains("new PersonRepository().findById(entity.Person)"),
                "a one-hop recipient must load the related record by FK before reading the address");
        assertTrue(billSend.contains("(Person == null ? null : Person.Email)"),
                "the recipient expression must be null-safe on an unset relation");
        assertTrue(billSend.contains("Mail.send("), "the sender must emit the actual send call");
        assertTrue(billSend.contains("no recipient"), "a record with nobody to mail must be a logged no-op, not a failure");
        String billBpmn = contentOf("BillFlow.bpmn");
        assertTrue(billBpmn.contains("gen.events.emission.BillFlowMailBillSend"),
                "the BPMN service task must bind the generated sender delegate");

        // (3) The FAN-OUT mirror - one document, many recipients (#6717). The rows are only the
        // recipient list, so the recipient still resolves against the ROW while the attachment is the
        // ANCHOR record's, rendered ONCE outside the loop and handed to every message.
        String shareBill = contentOf("gen/events/emission/BillFlowShareBillSend.java");
        assertTrue(shareBill.contains("Map document = rows.isEmpty() ? null : renderDocument(source);"),
                "a recordPrint fan-out must render the document once, before the loop - and not at all with no recipients");
        assertTrue(
                shareBill.contains("private Map renderDocument(BillEntity source)")
                        && shareBill.contains("new BillPrintFeeder().feed(source.Id)"),
                "the rendered document must be the ANCHOR record's, fed with the anchor's key: " + shareBill);
        assertEquals(1, shareBill.split("Print\\.render\\(", -1).length - 1,
                "one document for the whole fan-out means exactly one render call");
        assertTrue(shareBill.contains("private boolean send(BillEntity source, BillRecipientEntity entity, Map document)"),
                "the per-row send must take the anchor (a placeholder quotes it) and the rendered document");
        assertTrue(shareBill.contains("(Person == null ? null : Person.Email)"),
                "the recipient must still resolve against the ROW - the rows ARE the recipient list");
        assertTrue(shareBill.contains("\"Bill \" + source.Note"),
                "a {record.<field>} placeholder must read the anchor record, not the row: " + shareBill);
        // The deep link follows the SAME scoping rule as every other bare path: it links the ROW the
        // message is about, not the anchor whose document rides along.
        assertTrue(shareBill.contains("\"/services/web/emission-test/gen/emission/index.html#/BillRecipient/\" + entity.Id"),
                "a fan-out's {recordUrl} must link the ROW, not the anchor record: " + shareBill);
        assertTrue(billBpmn.contains("gen.events.emission.BillFlowShareBillSend"),
                "the BPMN service task must bind the generated fan-out sender delegate too");
        assertFalse(repository.getResource(PROJECT_PATH + "/custom/MailBill.java")
                              .exists(),
                "a notify serviceTask must NOT also scaffold a custom stub (it would never be invoked)");

        // postings reverses: the reversal handler negates the sibling's amount expressions on the
        // SAME side, locates the original through the empty storno link (fail-soft skip when none)
        // and stamps the link; the sibling's idempotency guard symmetrically excludes linked rows.
        String stornoPosting = contentOf("gen/events/emission/DocStornoPosting.java");
        assertTrue(stornoPosting.contains("Calc.eval(\"-(Amount)\", source, 2)"),
                "the reversal must negate the sibling's amount expression on the same side");
        assertTrue(stornoPosting.contains("nothing to reverse"), "the reversal must skip fail-soft when the source was never posted");
        assertTrue(stornoPosting.contains("target.Storno = original.Id;"), "the reversal must stamp the storno link to the original");
        String basePosting = contentOf("gen/events/emission/DocPostingPosting.java");
        assertTrue(basePosting.contains("candidate.Storno == null"), "the reversed posting's idempotency guard must exclude reversal rows");
        assertTrue(basePosting.contains("-Doc-transitioned"), "a status-triggered posting must bind the -transitioned topic");
        // source-FK copy (#6533): a to-one relation item cell copies the source FK verbatim onto the
        // line - no Calc, no negation, and it must carry through UNCHANGED onto the reversal line.
        assertTrue(basePosting.contains("item.Party = source.Party;"),
                "a to-one relation item cell must copy the source FK onto the posted line");
        assertTrue(!basePosting.contains("Calc.eval(\"Party\""), "the FK copy must not go through Calc");
        assertTrue(stornoPosting.contains("item.Party = source.Party;"),
                "the reversal must carry the copied source FK dimension UNCHANGED (not negated)");

        // postings onCreate (#6421): a source with no status lifecycle posts on its INSERT - the
        // handler binds the source's bare create topic (creates publish unsuffixed) and carries no
        // status guard.
        String createPosting = contentOf("gen/events/emission/PaymentPostingPosting.java");
        assertTrue(createPosting.contains("-Payment\";"), "an onCreate posting must bind the source's bare create topic");
        assertTrue(!createPosting.contains("-transitioned"), "an onCreate posting must not bind the -transitioned topic");
        assertTrue(!createPosting.contains("source.Status"), "an onCreate posting without a when guard must not emit a status guard");
        assertTrue(createPosting.contains("target.Payment = source.Id;"), "the onCreate posting must stamp its back-reference");

        // conditional dependsOn (#6358): the item register carries the classifier metadata (the
        // header-started by-path, the fetch URL, the cases map, the default) and the document page
        // carries the resolver that picks the copied property at runtime.
        String lineRegister = contentOf("gen/emission/js/components/pages/Entry/EntryLine.detail.js");
        assertTrue(lineRegister.contains("valueBy: { path: 'Entry.Account.Name', header: true,"),
                "the conditional dependsOn must emit the header-started classifier path");
        assertTrue(lineRegister.contains("cases: {\"Assets\":\"PackPrice\",\"Cash\":\"UnitPrice\"}"),
                "the conditional dependsOn must emit the cases map as a JS literal");
        assertTrue(lineRegister.contains("deflt: 'UnitPrice'"), "the conditional dependsOn must emit the no-match default");
        // The resolver ships with every generated document page (Ticket is the fixture's document).
        assertTrue(contentOf("gen/emission/js/components/pages/Ticket/TicketDocumentPage.js").contains("resolveDependsOnSource"),
                "the document page must carry the conditional dependsOn classifier resolver");

        // label: the repository recomputes the stored display Name on every write path.
        String claimRepository = contentOf("gen/emission/data/claim/ClaimRepository.java");
        assertTrue(claimRepository.contains("computeName"), "label must emit the display-name computation into the repository");
        assertTrue(claimRepository.contains("related.Name"),
                "a one-hop label token must load the related record and read its display property");
        // A month field is a YYYY-MM String (not a TemporalAccessor), so a |format label token
        // must parse it back to a temporal - otherwise the label degrades to the raw "2026-07".
        assertTrue(claimRepository.contains("YearMonth.parse"),
                "a |format token on a month field must parse the YYYY-MM string back to a temporal");
        // A workflow setter/writer targeted write keeps the stored display Name current: the label
        // repository OVERRIDES updateProperties to recompute it on that path too.
        assertTrue(claimRepository.contains("public int updateProperties(") && claimRepository.contains("computeName(entity)"),
                "a label entity must recompute its display Name on the targeted updateProperties write path");

        // The dead-Create family: a user-entered document title must be an editable input on the
        // document page, and a required-with-init property must not be demanded by any validation
        // layer (the DB default guarantees the value). Both halves shipped silently broken once:
        // the title rendered only as the isEdit/isPreview header span, so client validation failed
        // on a field the layout never rendered and the Create button looked dead.
        String voucherView = contentOf("gen/emission/views/Voucher/Voucher-document.html");
        assertTrue(voucherView.contains("x-model=\"form.RefNumber\""),
                "a user-entered documentTitle (no number series) must render an editable input on the document page");
        String voucherController = contentOf("gen/emission/api/voucher/VoucherController.java");
        assertTrue(voucherController.contains("The 'RefNumber' property is required"),
                "a plain required field keeps its REST create validation");
        assertFalse(voucherController.contains("The 'Status' property is required"),
                "required + init must not demand the payload value - the DB default guarantees it");
        String voucherPage = contentOf("gen/emission/js/components/pages/Voucher/VoucherDocumentPage.js");
        assertTrue(voucherPage.contains("RefNumber: [{ rule: 'required'"), "the client schema keeps required for the user-entered title");
        assertFalse(voucherPage.contains("Status: [{ rule: 'required'"), "the client schema must not require a defaulted (init:) property");

        // The ADMINISTRATION surface: one plain page per MODULE listing EVERY non-projection,
        // non-report entity (the low-level ones the business UIs hide included), registered on the
        // disjoint application-admin-perspectives point. This is the level above the Database
        // perspective - what an administrator corrects data through.
        String adminPage = contentOf("gen/emission/admin/index.html");
        assertTrue(adminPage.contains("window.AdminConfig"), "the admin surface must bake its entity metadata into the page");
        assertTrue(adminPage.contains("\"name\":\"Snapshot\""),
                "the admin surface must list the low-level append-only entity the business UI hides");
        assertTrue(adminPage.contains("\"name\":\"EntryLine\""),
                "the admin surface must list composition children as first-class entities");
        assertFalse(adminPage.contains("\"name\":\"Company\""),
                "a cross-model PROJECTION is rendered by its OWNER module, never duplicated here");
        // A relation is a combobox over the same lookup controller the business dropdowns use - a raw
        // foreign-key id input is how an administrator corrupts data.
        assertTrue(adminPage.contains("\"lookup\":{\"url\":"), "a relation column must carry its lookup URL for the combobox");
        assertTrue(adminPage.contains("loadLookups"), "the admin page must resolve relation ids to labels");
        assertTrue(adminPage.contains("\"readonly\":true"), "identity/calculated/audit columns must be marked read-only");
        String adminPerspective = contentOf("gen/emission/perspectives/admin/perspective.js");
        assertTrue(adminPerspective.contains("kind: 'ADMIN'"), "the admin perspective must declare the ADMIN kind");
        assertFalse(adminPerspective.contains("groupId"), "an admin perspective must not bake in the shell's navigation group id (#6646)");
        assertTrue(contentOf("gen/emission/perspectives/admin/perspective.extension").contains("application-admin-perspectives"),
                "the admin perspective must register on the admin extension point (never the application/my/partner ones)");

        // generates computed item lines (#6555): the create-from controller builds ONE synthetic
        // VoucherLine whose cells are expressions over the loaded `source` Slip - a Calc amount rounded
        // to the target field's scale, a {} interpolated string, and a null-safe Calc `when` guard -
        // then re-points it at the saved Voucher through the target repository. The Calc import proves
        // the numeric-expression path is wired.
        String generate = contentOf("gen/events/emission/VoucherFromSlipGenerate.java");
        assertTrue(generate.contains("import org.eclipse.dirigible.sdk.utils.Calc;"),
                "a computed item line must import Calc for its numeric expressions");
        assertTrue(generate.contains("Calc.eval(\"Total * 2\", source, 2)"),
                "a numeric item-line cell must emit a Calc expression over the source, rounded to the target scale");
        assertTrue(generate.contains("\"Slip \" + String.valueOf(source.Label)"),
                "a {field} string item-line cell must emit source interpolation");
        assertTrue(generate.contains("Calc.eval(\"Total\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0"),
                "an item-line when cell must emit a null-safe Calc row guard");
        assertTrue(generate.contains("VoucherLineEntity item") && generate.contains("item.Voucher = saved.Id;"),
                "the computed line must write into the auto-resolved target items child and re-point it at the saved master");

        // generates event: (#6711): the create-from also runs by itself. The listener binds the SOURCE's
        // -transitioned topic (the channel every status write publishes), guards on the status, and
        // delegates to the SAME create-from the button calls - it must not carry a mapping of its own, or
        // the two triggers would drift. `button: true` keeps the endpoint as the second trigger.
        String generateOnEvent = contentOf("gen/events/emission/VoucherFromSlipGenerateOnEvent.java");
        assertTrue(generateOnEvent.contains("implements MessageHandler"), "the event-driven create-from must be a message handler");
        assertTrue(generateOnEvent.contains("-Slip-transitioned"),
                "an onTransition create-from must bind the source's -transitioned topic");
        assertTrue(generateOnEvent.contains("source.Status != 2"), "the listener must guard on the status the seeded name resolved to");
        assertTrue(generateOnEvent.contains("new VoucherFromSlipGenerate().create("),
                "the listener must delegate to the create-from rather than re-implement the mapping");
        assertFalse(generateOnEvent.contains("VoucherLineEntity"), "the listener must carry no mapping of its own");
        assertTrue(generate.contains("@Post(\"/run\")"), "button: true must keep the endpoint alongside the event trigger");
        // The at-most-once guard lives in the create-from, so BOTH triggers get it.
        assertTrue(generate.contains(".eq(\"Slip\", sourceId)") && generate.contains("return existing.get(0);"),
                "an event-driven create-from must return the document already back-referencing the source");

        // generates prompt (#6685): the prompted controller takes a values map, enforces the
        // required input with a 400, and converts each posted value to the target field's Java type
        // (decimal -> BigDecimal). The action descriptor carries the prompt so the customActions
        // store opens the input dialog instead of the plain confirm.
        String promptedGenerate = contentOf("gen/events/emission/AddVoucherLineGenerate.java");
        assertTrue(promptedGenerate.contains("public java.util.Map<String, Object> values;"),
                "a prompted create-from must accept the declared inputs in the posted body");
        assertTrue(promptedGenerate.contains("missing required input [Amount]") && promptedGenerate.contains("Response.setStatus(400)"),
                "a missing required prompt input must answer 400 before anything is written");
        assertTrue(promptedGenerate.contains("new java.math.BigDecimal(String.valueOf(raw))"),
                "a decimal prompt input must convert to the generated entity's BigDecimal field");
        String promptedDescriptor = contentOf("add-voucher-line-generate-action.js");
        assertTrue(promptedDescriptor.contains("\"prompt\"") && promptedDescriptor.contains("\"promptEntity\": \"VoucherLine\""),
                "the action descriptor must carry the prompt and the target entity for the dialog's metadata lookup");
        assertTrue(promptedDescriptor.contains("\"name\": \"Amount\""),
                "prompt entries must carry the PascalCase property names the detail registration uses");

        // step resilience (#6762): the declared retry becomes a failed-job retry cycle on the
        // delegate task (R<count+1> - the R number counts TOTAL attempts), onError an error boundary
        // catching the INTENT_STEP_FAILED error the runtime conversion raises for the final failed
        // attempt, clearAfter an end-listener removing the produced secret, and the {error} setter a
        // read of the published failure-message variable.
        String provisionBpmn = contentOf("ProvisionFlow.bpmn");
        assertTrue(provisionBpmn.contains("<flowable:failedJobRetryTimeCycle>R3/PT2S</flowable:failedJobRetryTimeCycle>"),
                "retry count: 2 must emit an R3 failed-job retry cycle on the flaky delegate task");
        assertTrue(provisionBpmn.contains("<flowable:failedJobRetryTimeCycle>R2/PT2S</flowable:failedJobRetryTimeCycle>"),
                "retry count: 1 must emit an R2 failed-job retry cycle on the doomed delegate task");
        assertTrue(
                provisionBpmn.contains("errorCode=\"INTENT_STEP_FAILED\"")
                        && provisionBpmn.contains(
                                "<boundaryEvent id=\"flakyCallError\" attachedToRef=\"flakyCall\" cancelActivity=\"true\">")
                        && provisionBpmn.contains("sourceRef=\"doomedCallError\" targetRef=\"recordFailure\""),
                "onError must emit the intent error definition, a cancelling boundary per step and the routed flow");
        assertTrue(provisionBpmn.contains("${execution.removeVariable('apiKey')}"),
                "clearAfter must emit the end-listener removing the produced secret from the instance data");
        String failureSetter = contentOf("gen/events/emission/ProvisionFlowRecordFailure.java");
        assertTrue(failureSetter.contains("execution.getVariable(\"__errorMessage\")"),
                "the {error} setter must read the failure message the conversion published");
    }

    /**
     * {@code history: true} - the shadow trail, across every layer it has to reach: the schema's
     * sibling table, the repository's append on each write path (with the user/system attribution), the
     * read-only endpoint, the panel on the record's form, and the two interactions the keyword had to
     * specify - the audit columns stay out of the tracked set, and a surface that hides a sensitive
     * field is offered no history endpoint at all.
     */
    private void assertHistoryEmission(String schema, String entryRepository) {
        assertTrue(schema.contains("EMISSION_ENTRY_HISTORY") && schema.contains("EMISSION_CLAIM_HISTORY"),
                "history: true must emit the sibling _HISTORY shadow table into the schema");
        assertTrue(
                schema.contains("\"name\": \"OldValue\"") && schema.contains("\"name\": \"NewValue\"")
                        && schema.contains("\"name\": \"Source\""),
                "the shadow table must carry the delta, the actor and the write's source");

        // Every write path appends, and the SOURCE is what separates an edit somebody made from a
        // total the application recomputed. A repository that recorded only user writes would leave a
        // record silently changing between two entries in its own trail.
        assertTrue(
                entryRepository.contains("History.recordCreate(") && entryRepository.contains("History.recordUpdate(")
                        && entryRepository.contains("History.recordDelete("),
                "history: true must append on the create, update and delete paths");
        assertTrue(entryRepository.contains("History.USER") && entryRepository.contains("History.SYSTEM"),
                "the trail must attribute user writes and system writes differently");
        assertTrue(entryRepository.contains("super.findById(entity.Id)"),
                "the before-image must be read through the BASE find - the overridden one overlays translations");

        // The audit columns say exactly what the history row itself says (who and when), and the
        // primary key never changes: tracking either is noise in every single entry.
        String claimRepository = contentOf("gen/emission/data/claim/ClaimRepository.java");
        String tracked = claimRepository.substring(claimRepository.indexOf("HISTORY_PROPERTIES"));
        tracked = tracked.substring(0, tracked.indexOf(");"));
        assertTrue(tracked.contains("\"Note\"") && tracked.contains("\"Rate\"") && tracked.contains("\"Person\""),
                "the tracked set must carry the entity's own fields and foreign keys, got: " + tracked);
        assertFalse(tracked.contains("\"CreatedAt\"") || tracked.contains("\"UpdatedBy\"") || tracked.contains("\"Id\""),
                "the tracked set must exclude the audit columns and the primary key, got: " + tracked);

        // The read is read-only and power-surface only. A scoped surface strips a sensitive field from
        // its responses, so handing it a trail carrying that field's old and new values would leak
        // exactly what the scoping hides - the personal controller therefore has no history verb.
        assertTrue(contentOf("gen/emission/api/claim/ClaimController.java").contains("/{id}/history"),
                "history: true must emit the read-only history endpoint on the power controller");
        assertFalse(contentOf("gen/emission/api/claim/ClaimMyController.java").contains("history"),
                "the personal surface must expose no history endpoint - a sensitive field's deltas must not reach it");

        // The panel: the record's own form shows the trail, read-only.
        assertTrue(contentOf("gen/emission/js/components/pages/Entry/EntryFormPage.js").contains("loadHistory()"),
                "the record form must load its change trail");
        assertTrue(contentOf("gen/emission/views/Entry/Entry-form.html").contains("historyLabel(entry.Property)"),
                "the record form must render the change trail as a read-only panel");
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

        // ... and a REPORT grouping by that nomenclature agrees with the list page it sits next to:
        // the same request language, the same value. A report is raw SQL over the base tables, so
        // before the language-table overlay reached the SELECT list this row read "Piece" while the
        // list beside it read "Брой" (dirigible #6544).
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "bg")
                                                 .when()
                                                 .get(REPORT_API + "/ClaimsByUnitController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].Unit", equalTo("Брой"))
                                                 // The report backend answers generic JSON, so every
                                                 // number comes back as a float - the group is both
                                                 // claim rows, so the join did not multiply them.
                                                 .body("[0].Count", equalTo(2.0F)),
                30);
        // The fallback half: no translation for the caller's language leaves the base value - which
        // is what makes the join LEFT and the column a COALESCE rather than a plain translated read.
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "de")
                                                 .when()
                                                 .get(REPORT_API + "/ClaimsByUnitController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].Unit", equalTo("Piece")));

        // calculatedActionOnCreate on a to-one relation, end to end: a create that OMITS the FK comes
        // back carrying the one the action resolved. Tariff 2 is the row flagged `base` - not the first
        // row and not any default the column could otherwise acquire - so this can only pass if the
        // action actually ran on the server. Before the relation carried the calculated attributes into
        // the .model this returned null, silently.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Note\":\"defaulted\"}")
                                                 .when()
                                                 .post(API + "/quote/QuoteController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Tariff", equalTo(2)));

        // The other half of the contract: the action runs on EVERY create, so it must return an
        // already-set value unchanged - a caller's explicit pick always wins. Without this the feature
        // would silently overwrite whatever the caller chose.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Note\":\"explicit\",\"Tariff\":1}")
                                                 .when()
                                                 .post(API + "/quote/QuoteController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Tariff", equalTo(1)));

        // First-class numbering end-to-end: publish provisioned the authored .numbers series for
        // the tenant, and the generated DAO stamps prefix + zero-padded sequence at insert -
        // gap-free, in the DECLARED shape (an undeclared series would fail this create with 500,
        // never invent a shape). Both creates run in one executor pass, so a retry that re-runs
        // the lambda still yields two consecutive numbers - assert RELATIVELY.
        AtomicReference<String> firstNumber = new AtomicReference<>();
        AtomicReference<String> secondNumber = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            firstNumber.set(given().contentType("application/json")
                                   .body("{\"Note\":\"first\"}")
                                   .when()
                                   .post(API + "/receipt/ReceiptController")
                                   .then()
                                   .statusCode(200)
                                   .extract()
                                   .path("Number"));
            secondNumber.set(given().contentType("application/json")
                                    .body("{\"Note\":\"second\"}")
                                    .when()
                                    .post(API + "/receipt/ReceiptController")
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .path("Number"));
        });
        assertTrue(firstNumber.get()
                              .matches("ER-\\d{5}"),
                "the stamped number must render in the DECLARED shape (prefix + zero-padded to the total width): " + firstNumber.get());
        assertEquals(Integer.parseInt(firstNumber.get()
                                                 .substring(3))
                + 1,
                Integer.parseInt(secondNumber.get()
                                             .substring(3)),
                "the series must be gap-free: " + firstNumber.get() + " then " + secondNumber.get());

        // The dead-Create family at the outermost layer: creating the document WITHOUT the
        // defaulted status must succeed, and the echo must carry the DB-applied default (the
        // persisted row, not the request payload the caller sent).
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"RefNumber\":\"INV-77\",\"Date\":\"2026-01-15\"}")
                                                 .when()
                                                 .post(API + "/voucher/VoucherController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1))
                                                 .body("RefNumber", equalTo("INV-77")));
        // Omitting the user-entered title stays a validation rejection.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Date\":\"2026-01-15\"}")
                                                 .when()
                                                 .post(API + "/voucher/VoucherController")
                                                 .then()
                                                 .statusCode(400));

        // generates computed item lines (#6555) at the outermost layer: create a Slip, fire the
        // create-from, and assert the Voucher was born with ONE computed line - amount = Total * 2
        // (Calc arithmetic over the source), note = "Slip <label>" ({} interpolation). This is the
        // capability that did not exist before: a create-from that builds a COMPUTED line, not a
        // 1:1 clone of a source child.
        AtomicInteger slipId = new AtomicInteger();
        restAssuredExecutor.execute(() -> slipId.set(given().contentType("application/json")
                                                            .body("{\"Label\":\"March\",\"Total\":21}")
                                                            .when()
                                                            .post(API + "/slip/SlipController")
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .path("Id")));
        AtomicInteger generatedVoucherId = new AtomicInteger();
        // The generated create-from controller returns Json.stringify(saved) as a String, which the SDK
        // serves as text/plain - so parse the body as JSON explicitly rather than RestAssured's
        // content-type-driven .path() (which only maps a JSON/XML response).
        restAssuredExecutor.execute(() -> {
            String voucher = given().contentType("application/json")
                                    .body("{\"id\":" + slipId.get() + "}")
                                    .when()
                                    .post("/services/java/" + PROJECT + "/gen/events/emission/VoucherFromSlipGenerate/run")
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .asString();
            generatedVoucherId.set(io.restassured.path.json.JsonPath.from(voucher)
                                                                    .getInt("Id"));
        });
        restAssuredExecutor.execute(() -> {
            int voucherId = generatedVoucherId.get();
            String matching = "findAll { it.Voucher == " + voucherId + " }";
            io.restassured.path.json.JsonPath lines = given().when()
                                                             .get(API + "/voucher/VoucherLineController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .jsonPath();
            assertEquals(1, lines.getList(matching)
                                 .size(),
                    "the computed create-from must produce exactly one synthetic line");
            // amount = Total(21) * 2, computed by Calc over the source (not a copied literal).
            assertEquals(42.0f, lines.getFloat(matching + ".Amount[0]"), 0.001f);
            // note = "Slip " + the source's label, via {} interpolation.
            assertEquals("Slip March", lines.getString(matching + ".Note[0]"));
        });

        // generates event: (#6711) at the outermost layer, and the capability that did not exist:
        // NOBODY calls the create-from here. A second Slip is posted through its transition, and the
        // Voucher has to appear by itself - with its computed line, so the whole create-from ran and not
        // just an empty header.
        AtomicInteger postedSlip = new AtomicInteger();
        restAssuredExecutor.execute(() -> postedSlip.set(given().contentType("application/json")
                                                                .body("{\"Label\":\"April\",\"Total\":7}")
                                                                .when()
                                                                .post(API + "/slip/SlipController")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + postedSlip.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/emission/PostSlipTransition/run")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(2)));
        AtomicInteger autoVoucherId = new AtomicInteger();
        // The listener runs asynchronously off the -transitioned topic, so retry until it lands.
        restAssuredExecutor.execute(() -> {
            String matching = "findAll { it.Slip == " + postedSlip.get() + " }";
            io.restassured.path.json.JsonPath vouchers = given().when()
                                                                .get(API + "/voucher/VoucherController")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .jsonPath();
            assertEquals(1, vouchers.getList(matching)
                                    .size(),
                    "posting the slip must mint exactly one voucher back-referencing it");
            autoVoucherId.set(vouchers.getInt(matching + ".Id[0]"));
        }, 60);
        restAssuredExecutor.execute(() -> {
            String matching = "findAll { it.Voucher == " + autoVoucherId.get() + " }";
            io.restassured.path.json.JsonPath lines = given().when()
                                                             .get(API + "/voucher/VoucherLineController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .jsonPath();
            assertEquals(1, lines.getList(matching)
                                 .size(),
                    "the event-driven create-from must run the WHOLE create-from, computed line included");
            assertEquals(14.0f, lines.getFloat(matching + ".Amount[0]"), 0.001f);
        }, 60);
        // At-most-once, across both triggers: clicking the button for a slip the event already generated
        // for hands back the SAME voucher instead of minting a second one. Without the back-reference
        // guard an event redelivery would do the same damage silently.
        restAssuredExecutor.execute(() -> {
            String voucher = given().contentType("application/json")
                                    .body("{\"id\":" + postedSlip.get() + "}")
                                    .when()
                                    .post("/services/java/" + PROJECT + "/gen/events/emission/VoucherFromSlipGenerate/run")
                                    .then()
                                    .statusCode(200)
                                    .extract()
                                    .asString();
            assertEquals(autoVoucherId.get(), io.restassured.path.json.JsonPath.from(voucher)
                                                                               .getInt("Id"),
                    "a click after the event must return the existing voucher, not a second one");
        });

        // generates prompt (#6685) at the outermost layer: the prompted values reach the created
        // child, and a missing required input is a 400 that creates nothing. Runs against the
        // voucher the create-from above just made.
        restAssuredExecutor.execute(() -> {
            int voucherId = generatedVoucherId.get();
            given().contentType("application/json")
                   .body("{\"id\":" + voucherId + ", \"values\": {\"Note\": \"no amount\"}}")
                   .when()
                   .post("/services/java/" + PROJECT + "/gen/events/emission/AddVoucherLineGenerate/run")
                   .then()
                   .statusCode(400);
            given().contentType("application/json")
                   .body("{\"id\":" + voucherId + ", \"values\": {\"Amount\": 7.5, \"Note\": \"manual\"}}")
                   .when()
                   .post("/services/java/" + PROJECT + "/gen/events/emission/AddVoucherLineGenerate/run")
                   .then()
                   .statusCode(200);
            String matching = "findAll { it.Voucher == " + voucherId + " && it.Note == 'manual' }";
            io.restassured.path.json.JsonPath lines = given().when()
                                                             .get(API + "/voucher/VoucherLineController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .jsonPath();
            assertEquals(1, lines.getList(matching)
                                 .size(),
                    "the prompted create-from must produce exactly one manual line (and none for the rejected 400 run)");
            assertEquals(7.5f, lines.getFloat(matching + ".Amount[0]"), 0.001f,
                    "the prompted decimal must reach the created child's column");
        });

        // #6336 pattern: a malformed e-mail must be rejected by the generated controller, and a
        // well-formed one accepted - the regex authored in the intent is what actually runs.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Name\":\"Bad\",\"Email\":\"p1@example.com\",\"ContactEmail\":\"not-an-email\"}")
                                                 .when()
                                                 .post(API + "/person/PersonController")
                                                 .then()
                                                 .statusCode(400));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Name\":\"Good\",\"Email\":\"p2@example.com\",\"ContactEmail\":\"good@example.com\"}")
                                                 .when()
                                                 .post(API + "/person/PersonController")
                                                 .then()
                                                 .statusCode(200));

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

        // related, at runtime: the register's data path is the source controller's generic search,
        // filtered on the foreign key back to the open record - there is no master-filter query
        // parameter outside a composition detail, so this is the whole mechanism. Account 2 must
        // answer with the entry just booked against it, and account 1 (a parent node, referenced by
        // nothing) with none: a register that returned every row would look right on a fixture with
        // one account and be wrong everywhere else.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"equals\":{\"Account\":2}}")
                                                 .when()
                                                 .post(API + "/entry/EntryController/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", hasItem(entryId)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"equals\":{\"Account\":1}}")
                                                 .when()
                                                 .post(API + "/entry/EntryController/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(0)));

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
        AtomicInteger creditLine = new AtomicInteger();
        restAssuredExecutor.execute(() -> creditLine.set(given().contentType("application/json")
                                                                .body("{\"Entry\":" + entryId + ",\"Credit\":100}")
                                                                .when()
                                                                .post(API + "/entry/EntryLineController")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + entryId + ",\"Date\":\"2026-01-15\",\"Account\":2,\"Status\":2}")
                                                 .when()
                                                 .put(API + "/entry/EntryController/" + entryId)
                                                 .then()
                                                 .statusCode(200));

        // entity-level unique (#6763): the composite key exists in the database (the second insert
        // cannot land) AND the generated controller recognises which key was hit, so the caller is
        // told what collided instead of getting a 500. The third write flips one column, which must
        // be accepted - a constraint over (party, code) that rejected a different code would be a
        // single-column unique wearing a composite name.
        AtomicInteger partyId = new AtomicInteger();
        restAssuredExecutor.execute(() -> partyId.set(given().contentType("application/json")
                                                             .body("{\"Name\":\"Unique Co\"}")
                                                             .when()
                                                             .post(API + "/party/PartyController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Party\":" + partyId.get() + ",\"Code\":\"AB-1\"}")
                                                 .when()
                                                 .post(API + "/partycode/PartyCodeController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Party\":" + partyId.get() + ",\"Code\":\"AB-1\"}")
                                                 .when()
                                                 .post(API + "/partycode/PartyCodeController")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Party\":" + partyId.get() + ",\"Code\":\"AB-2\"}")
                                                 .when()
                                                 .post(API + "/partycode/PartyCodeController")
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
        // ...and the UI pre-check endpoint reports it, so the generated pages disable Edit up front.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryController/" + entryId + "/mutable")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("mutable", equalTo(false)));

        // ...and so are the document's LINES (#6695): a line write recomputes the master's totals, so
        // the lock has to reach the child's own controller too - otherwise the one operation the lock
        // exists to prevent stays reachable, and rewrites the totals after the ledger posted from them.
        int lineId = creditLine.get();
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Entry\":" + entryId + ",\"Debit\":50}")
                                                 .when()
                                                 .post(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + lineId + ",\"Entry\":" + entryId + ",\"Credit\":500}")
                                                 .when()
                                                 .put(API + "/entry/EntryLineController/" + lineId)
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(API + "/entry/EntryLineController/" + lineId)
                                                 .then()
                                                 .statusCode(409));
        // ...and the collection is exactly as the entry left it - the refusals wrote nothing.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryLineController?Entry=" + entryId)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(2))
                                                 .body("find { it.Id == " + lineId + " }.Credit", equalTo(100.0f)));

        // ...while the collection that declared `locksWithMaster: false` keeps its writes past the
        // master's lock - the settlement case (#6700), which is why the inherited lock has an opt-out
        // rather than being unconditional.
        AtomicInteger campaign = new AtomicInteger();
        restAssuredExecutor.execute(() -> campaign.set(given().contentType("application/json")
                                                              .body("{\"Name\":\"Spring\"}")
                                                              .when()
                                                              .post(API + "/campaign/CampaignController")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + campaign.get() + ",\"Name\":\"Spring\",\"Status\":2}")
                                                 .when()
                                                 .put(API + "/campaign/CampaignController/" + campaign.get())
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + campaign.get() + ",\"Name\":\"Summer\",\"Status\":2}")
                                                 .when()
                                                 .put(API + "/campaign/CampaignController/" + campaign.get())
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Campaign\":" + campaign.get() + ",\"Note\":\"called the sponsor\"}")
                                                 .when()
                                                 .post(API + "/campaign/CampaignNoteController")
                                                 .then()
                                                 .statusCode(200));

        // history: the whole life of the record is readable from one endpoint - the create, and the
        // status hop the user made with both sides of it recorded, so "who changed this from what"
        // has an answer. The audit columns and the key stay out: they restate what the entry carries.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryController/" + entryId + "/history")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Operation == 'CREATE' && it.Property == 'Account' }.Source",
                                                         hasItem("USER"))
                                                 .body("findAll { it.Property == 'Status' && it.NewValue == '2' }.OldValue", hasItem("1"))
                                                 .body("findAll { it.Property == 'Status' && it.NewValue == '2' }.Source", hasItem("USER"))
                                                 .body("Property", not(hasItem("Id"))));
        // A trail is only meaningful for a record that exists - an unknown id is a 404, never an empty
        // history a caller could read as "nothing ever happened here".
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryController/999999/history")
                                                 .then()
                                                 .statusCode(404));

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
        // An append-only record reports immutable from the moment it exists.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/snapshot/SnapshotController/" + snapshot.get() + "/mutable")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("mutable", equalTo(false)));

        // transitions: a fresh DRAFT entry cancels (200, status CANCELLED)...
        String transitionRun = "/services/java/" + PROJECT + "/gen/events/emission/CancelEntryTransition/run";
        AtomicInteger cancellable = new AtomicInteger();
        restAssuredExecutor.execute(() -> cancellable.set(given().contentType("application/json")
                                                                 .body("{\"Date\":\"2026-01-16\",\"Account\":2}")
                                                                 .when()
                                                                 .post(API + "/entry/EntryController")
                                                                 .then()
                                                                 .statusCode(200)
                                                                 .extract()
                                                                 .path("Id")));
        // While DRAFT the pre-check reports the record editable (the Edit affordances stay live).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryController/" + cancellable.get() + "/mutable")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("mutable", equalTo(true)));
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

        // send a document by e-mail, at its outermost layer: SendBill flips the status AND tries to
        // mail the bill's rendered PDF. This instance has no SMTP and no print template in the CMS,
        // so the send cannot succeed - and that must not matter: the transition is still a 200 with
        // the status written. (A regression here - an unguarded send - turns every mail
        // misconfiguration into a failed business transaction, which is exactly why the generated
        // controller wraps it.)
        AtomicInteger bill = new AtomicInteger();
        restAssuredExecutor.execute(() -> bill.set(given().contentType("application/json")
                                                          .body("{\"Note\":\"mailed bill\"}")
                                                          .when()
                                                          .post(API + "/bill/BillController")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("Id")));
        // A line change must refresh the header's expression-calculated field in the same recompute:
        // balanceDue (= amount) follows the aggregate the moment the line lands - it must never stay
        // null/stale until an explicit header save (the unpaid-invoice Balance printed empty).
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Amount\":250,\"Bill\":" + bill.get() + "}")
                                                 .when()
                                                 .post(API + "/bill/BillLineController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> {
            Object balanceDue = given().when()
                                       .get(API + "/bill/BillController/" + bill.get())
                                       .then()
                                       .statusCode(200)
                                       .extract()
                                       .path("BalanceDue");
            assertTrue(balanceDue instanceof Number && Math.abs(((Number) balanceDue).doubleValue() - 250.0) < 0.001,
                    "the calculated header field must follow the totals recompute, got: " + balanceDue);
        });
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + bill.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/emission/SendBillTransition/run")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(2)));
        // ...and the send froze the document, LINES INCLUDED (#6695). This is the whole point of the
        // inherited lock: a line write recomputes the header's totals, so accepting one here would
        // move the amount the mailed PDF was rendered from, on a document nobody may edit any more.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Amount\":999,\"Bill\":" + bill.get() + "}")
                                                 .when()
                                                 .post(API + "/bill/BillLineController")
                                                 .then()
                                                 .statusCode(409));
        restAssuredExecutor.execute(() -> {
            Object amount = given().when()
                                   .get(API + "/bill/BillController/" + bill.get())
                                   .then()
                                   .statusCode(200)
                                   .extract()
                                   .path("Amount");
            assertTrue(amount instanceof Number && Math.abs(((Number) amount).doubleValue() - 250.0) < 0.001,
                    "a refused line write must leave the locked document's total untouched, got: " + amount);
        });

        // postings: posting a Doc creates the balanced Entry (async handler - poll)...
        AtomicInteger doc = new AtomicInteger();
        restAssuredExecutor.execute(() -> doc.set(given().contentType("application/json")
                                                         .body("{\"Date\":\"2026-01-17\",\"Amount\":250,\"Party\":1}")
                                                         .when()
                                                         .post(API + "/doc/DocController")
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + doc.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/emission/PostDocTransition/run")
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
        // source-FK copy (#6533): the debit line copied Doc.Party as its dimension; the credit line
        // (no Party cell) carries none.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Entry == " + originalEntry.get() + " && it.Party == 1 }.size()",
                                                         equalTo(1))
                                                 .body("findAll { it.Entry == " + originalEntry.get() + " && it.Party == null }.size()",
                                                         equalTo(1)));
        // ...and reverses: voiding the Doc creates the red storno - the SAME lines negated on the
        // SAME sides, linked to the original through the storno self-relation.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + doc.get() + "}")
                                                 .when()
                                                 .post("/services/java/" + PROJECT + "/gen/events/emission/VoidDocTransition/run")
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
                                                         + " && it.Credit != null && it.Credit < 0 }.size()", equalTo(1))
                                                 // storno carry-through (#6533): the reversal's debit line
                                                 // carries the SAME Party dimension, unnegated.
                                                 .body("findAll { it.Entry == " + reversalEntry.get() + " && it.Party == 1 }.size()",
                                                         equalTo(1)));

        // lifecycle at the outermost layer: the two transitions above walked the graph (DRAFT ->
        // POSTED -> CANCELLED) and both returned 200. What the declaration adds is everything ELSE
        // being refused - through the plain REST surface, which no transition guard covers.
        //
        // (1) A move no edge declares: the voided Doc dragged back to DRAFT.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + doc.get() + ",\"Date\":\"2026-01-17\",\"Amount\":250,\"Status\":1}")
                                                 .when()
                                                 .put(API + "/doc/DocController/" + doc.get())
                                                 .then()
                                                 .statusCode(400));
        // The record is untouched - a rejected move must not half-write.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/doc/DocController/" + doc.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(3)));
        // (2) A move the graph DOES declare still goes through the same guard: an edit that leaves
        // the status where it stands is not a move at all.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + doc.get() + ",\"Date\":\"2026-01-17\",\"Amount\":260,\"Status\":3}")
                                                 .when()
                                                 .put(API + "/doc/DocController/" + doc.get())
                                                 .then()
                                                 .statusCode(200));
        // (3) Entering the lifecycle anywhere but at its declared start: a Doc filed as POSTED.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Date\":\"2026-01-19\",\"Amount\":10,\"Status\":2}")
                                                 .when()
                                                 .post(API + "/doc/DocController")
                                                 .then()
                                                 .statusCode(400));

        // postings onCreate (#6421): a booked Payment has no status lifecycle - its INSERT posts
        // the balanced Entry (async handler - poll), back-referenced through Entry.Payment.
        AtomicInteger payment = new AtomicInteger();
        restAssuredExecutor.execute(() -> payment.set(given().contentType("application/json")
                                                             .body("{\"Date\":\"2026-01-18\",\"Amount\":90}")
                                                             .when()
                                                             .post(API + "/payment/PaymentController")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("Id")));
        AtomicInteger paymentEntry = new AtomicInteger();
        restAssuredExecutor.execute(() -> paymentEntry.set(given().when()
                                                                  .get(API + "/entry/EntryController")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .body("findAll { it.Payment == " + payment.get() + " }.size()",
                                                                          equalTo(1))
                                                                  .extract()
                                                                  .path("find { it.Payment == " + payment.get() + " }.Id")),
                30);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/entry/EntryLineController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Entry == " + paymentEntry.get() + " && it.Debit != null }.size()",
                                                         equalTo(1))
                                                 .body("findAll { it.Entry == " + paymentEntry.get() + " && it.Credit != null }.size()",
                                                         equalTo(1)));

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
        AtomicInteger ownClaim = new AtomicInteger();
        restAssuredExecutor.execute(() -> ownClaim.set(given().contentType("application/json")
                                                              .body("{\"Note\":\"spoofed\",\"Person\":2,\"Rate\":999,\"Period\":\"2026-07\"}")
                                                              .when()
                                                              .post(API + "/claim/ClaimMyController")
                                                              .then()
                                                              .statusCode(200)
                                                              .body("Person", equalTo(1))
                                                              .body("Rate", nullValue())
                                                              // label: the stored display name computed on write -
                                                              // "{note} ({Person.name}) {period|yyyy MMMM}"; the month
                                                              // value formats through the pattern, never the raw 2026-07.
                                                              .body("Name", equalTo("spoofed (Admin) 2026 July"))
                                                              .extract()
                                                              .path("Id")));

        // history, the other half of the trail: the SYSTEM attribution. Creating this claim started
        // its process, and the trigger wrote the instance id back onto the record - a write no person
        // made. Without the source column that write is indistinguishable in the trail from the user's
        // own edit one row above it, which is the first thing a supervisory audit asks about.
        // (Asynchronous: the trigger reacts to the create event, hence the retry.)
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/claim/ClaimController/" + ownClaim.get() + "/history")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Property == 'ProcessId' }.Source", hasItem("SYSTEM"))
                                                 .body("findAll { it.Operation == 'CREATE' && it.Property == 'Note' }.Source",
                                                         hasItem("USER")),
                30);
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

        // ---- Act as (delegated entry): an entitled user arms an acting identity for the SESSION
        // and the personal surfaces serve THAT person's world - the manager-does-the-entry mode.
        // The override lives in the server-side session, so the sequence pins one session.
        restAssuredExecutor.execute(() -> {
            io.restassured.filter.session.SessionFilter session = new io.restassured.filter.session.SessionFilter();
            given().filter(session)
                   .when()
                   .get("/services/core/actas")
                   .then()
                   .statusCode(200)
                   .body("entitled", equalTo(true))
                   .body("actingAs", nullValue());
            given().filter(session)
                   .contentType("application/json")
                   .body("{\"username\":\"other@example.com\"}")
                   .when()
                   .put("/services/core/actas")
                   .then()
                   .statusCode(200)
                   .body("actingAs", equalTo("other@example.com"));
            // The my list now serves the ACTING person's rows - and the sensitive strip still holds.
            given().filter(session)
                   .when()
                   .get(API + "/claim/ClaimMyController")
                   .then()
                   .statusCode(200)
                   .body("findAll { it.Person != 2 }.size()", equalTo(0))
                   .body("findAll { it.Note == 'foreign' }.size()", equalTo(1))
                   .body("[0].Rate", nullValue());
            // A write goes under the ACTING identity, while the audit column keeps the REAL user -
            // the record shows whose it is AND who really entered it.
            given().filter(session)
                   .contentType("application/json")
                   .body("{\"Note\":\"delegated\",\"Rate\":123}")
                   .when()
                   .post(API + "/claim/ClaimMyController")
                   .then()
                   .statusCode(200)
                   .body("Person", equalTo(2))
                   .body("Rate", nullValue())
                   .body("CreatedBy", equalTo("admin"));
            // personalReadOnly still refuses writes - acting as the owner does not grant authoring.
            given().filter(session)
                   .contentType("application/json")
                   .body("{\"Days\":5}")
                   .when()
                   .post(API + "/balance/BalanceMyController")
                   .then()
                   .statusCode(403);
            // Disarm restores self - the my list is the real user's again.
            given().filter(session)
                   .when()
                   .delete("/services/core/actas")
                   .then()
                   .statusCode(200)
                   .body("actingAs", nullValue());
            given().filter(session)
                   .when()
                   .get(API + "/claim/ClaimMyController")
                   .then()
                   .statusCode(200)
                   .body("findAll { it.Person != 1 }.size()", equalTo(0));
        });
        // While armed, the Inbox's assignee query serves the ACTING person's personal-assigned
        // tasks - the delegated claim's confirm task, which the real user could never see before.
        // Retried (the task spawns off the create event); every step here is idempotent.
        restAssuredExecutor.execute(() -> {
            io.restassured.filter.session.SessionFilter session = new io.restassured.filter.session.SessionFilter();
            given().filter(session)
                   .contentType("application/json")
                   .body("{\"username\":\"other@example.com\"}")
                   .when()
                   .put("/services/core/actas")
                   .then()
                   .statusCode(200);
            given().filter(session)
                   .when()
                   .get("/services/inbox/tasks?type=assigned")
                   .then()
                   .statusCode(200)
                   .body("findAll { it.assignee == 'other@example.com' }.size()", greaterThanOrEqualTo(1));
            given().filter(session)
                   .when()
                   .delete("/services/core/actas")
                   .then()
                   .statusCode(200);
        }, 30);

        // Personal Shell (phase C): the shell page is served and aggregates the published personal
        // perspective through the application-personal-perspectives extension point.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/personal/index.html")
                                                 .then()
                                                 .statusCode(200));
        // Assert the perspective is INSIDE the shell's navigation group, not merely present somewhere in
        // the response: the aggregator places perspectives by group, and one it cannot place used to be
        // dropped, leaving the group rendered empty with no diagnostic anywhere (#6646).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-personal-perspectives")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("perspectives.find { it.id == 'personal' }.items.id",
                                                         hasItem("emission-test-my-Claim")),
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
        // ADMINISTRATION shell: the shell page is served, aggregates this module's admin perspective
        // through its own disjoint extension point, and the generated admin page itself serves.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/admin/index.html")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-admin-perspectives")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("perspectives.find { it.id == 'admin' }.items.id", hasItem(PROJECT + "-admin")),
                30);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/" + PROJECT + "/gen/emission/admin/index.html")
                                                 .then()
                                                 .statusCode(200));
        // The promise that makes this surface safe: an admin correction goes through the SAME power
        // controller, so an append-only record is still rejected (409) - the administrator sees the
        // reason, never a silent bypass.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + snapshot.get() + ",\"Payload\":\"admin tamper\"}")
                                                 .when()
                                                 .put(API + "/snapshot/SnapshotController/" + snapshot.get())
                                                 .then()
                                                 .statusCode(409));

        // Partner shell: served + aggregates the published partner perspective (the disjoint point).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/web/partner/index.html")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/js/platform-core/extension-services/perspectives.js?extensionPoints=application-partner-perspectives")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("perspectives.find { it.id == 'partner' }.items.id",
                                                         hasItem("emission-test-partner-PartnerTicket")),
                30);

        // personalReadOnly: the scoped read serves 200 (the owner sees their own rows), but a write
        // to the personal surface is refused 403 - the see-only guarantee at the outermost layer.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/balance/BalanceMyController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Days\":5}")
                                                 .when()
                                                 .post(API + "/balance/BalanceMyController")
                                                 .then()
                                                 .statusCode(403));

        assertManyToManyRuntime();
        assertInboundSourcesRuntime();
        assertOutboundDepartureRuntime();
        assertExpansionLifecycleRuntime();
        assertBpmEventsRuntime();
    }

    /**
     * An expansion's generated rows, over their whole life (#6821): they appear when the master is
     * created and they are gone once it is deleted. The delete half is the one that was missing - the
     * construct bound create and update only, and because a foreign key never becomes a database
     * constraint here, the rows simply survived as orphans still counted by every roll-up and report.
     * Only the runtime shows it: the emitted handler can be present and still be subscribed to a topic
     * nothing publishes to.
     */
    private void assertExpansionLifecycleRuntime() {
        String retainerApi = API + "/retainer/RetainerController";
        String periodApi = API + "/retainer/RetainerPeriodController";
        AtomicInteger retainerId = new AtomicInteger();
        restAssuredExecutor.execute(() -> retainerId.set(given().contentType("application/json")
                                                                .body("{\"Note\":\"expanded\",\"StartDate\":\"2026-01-15\",\"EndDate\":\"2026-03-15\",\"Fee\":300}")
                                                                .when()
                                                                .post(retainerApi)
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")));
        // A month span over three months yields a row per month, each carrying its share of the fee.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(periodApi + "?Retainer=" + retainerId.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(3)),
                30);

        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(retainerApi + "/" + retainerId.get())
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(periodApi + "?Retainer=" + retainerId.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(0)),
                30);
    }

    /**
     * The non-HTTP inbound arrivals end to end (#6537): a JSON record sent to the declared queue, and
     * one dropped as a file into the polled folder, both turn into rows through the entity's own
     * repository. This is the layer the declaration is about - the generated listener actually being
     * subscribed, and the generated job actually polling - which no amount of asserting the emitted
     * source can show.
     */
    private void assertInboundSourcesRuntime() {
        String signalApi = API + "/signal/SignalController";

        MessagingFacade.sendToQueue("emission-signals", "{\"Note\":\"from the queue\"}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(signalApi)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Note", hasItem("from the queue")),
                60);

        // The drop folder is relative to the running instance's working directory, exactly as the
        // intent declares it. A file is only read once it has been untouched for the generated
        // handler's stability window, so the poll below is generous.
        Path dropFolder = Paths.get("target/inbox-emission");
        try {
            Files.createDirectories(dropFolder);
            // A batch, so the array form of the payload is exercised too.
            Files.writeString(dropFolder.resolve("signals.json"), "[{\"Note\":\"from the file\"},{\"Note\":\"from the file too\"}]");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to drop the ingest file into " + dropFolder.toAbsolutePath(), ex);
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(signalApi)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Note", hasItem("from the file"))
                                                 .body("Note", hasItem("from the file too")),
                120);
        // Every read file leaves the drop folder, so the next tick cannot ingest it again.
        assertTrue(Files.exists(dropFolder.resolve("processed/signals.json")),
                "an ingested file must be moved out of the drop folder, into processed/");
    }

    /**
     * The outbound departure end to end (#6767): creating a record puts the DECLARED envelope on the
     * declared queue, and a record the guard excludes puts nothing there. Only this layer can show it -
     * the publisher being really subscribed, the envelope being really built, and the guard really
     * running - which no assertion over the emitted source reaches.
     */
    private void assertOutboundDepartureRuntime() {
        String signalApi = API + "/signal/SignalController";
        // The guarded record first: the queue is FIFO, so had it departed it would arrive BEFORE the
        // one that must, and the drain below would see it.
        createSignal(signalApi, "internal");
        createSignal(signalApi, "outbound-ok");

        String departed = null;
        for (int attempt = 0; attempt < 30 && departed == null; attempt++) {
            String message = MessagingFacade.receiveFromQueue("emission-signals-out", 2000);
            if (message == null) {
                continue;
            }
            assertFalse(message.contains("\"internal\""), "a record the when guard excludes must never depart: " + message);
            if (message.contains("outbound-ok")) {
                departed = message;
            }
        }
        assertNotNull(departed, "the created record must depart on the queue the intent names");
        assertTrue(
                departed.contains("\"type\":\"signal.raised\"") && departed.contains("\"version\":1")
                        && departed.contains("\"note\":\"outbound-ok\""),
                "the departure must carry the declared envelope, not the row: " + departed);
        assertFalse(departed.contains("\"Note\""), "the envelope replaces the record - a stored column must not leak into it: " + departed);
    }

    /** Creates a Signal through the generated REST surface, which is what raises the departure. */
    private void createSignal(String signalApi, String note) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Note\":\"" + note + "\"}")
                                                 .when()
                                                 .post(signalApi)
                                                 .then()
                                                 .statusCode(200));
    }

    /**
     * n:m end to end (#6718): the link entity materialised from {@code kind: manyToMany} is a real
     * table with a working REST surface - a link row is created against both ends and comes back
     * through the very query the master's detail grid uses. This is the layer the keyword never reached
     * while it was parsed and dropped.
     */
    private void assertManyToManyRuntime() {
        AtomicInteger tagId = new AtomicInteger();
        AtomicInteger courseId = new AtomicInteger();
        restAssuredExecutor.execute(() -> tagId.set(given().contentType("application/json")
                                                           .body("{\"Name\":\"Modeling\"}")
                                                           .when()
                                                           .post(API + "/tag/TagController")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .path("Id")));
        restAssuredExecutor.execute(() -> courseId.set(given().contentType("application/json")
                                                              .body("{\"Name\":\"Intent 101\"}")
                                                              .when()
                                                              .post(API + "/course/CourseController")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .path("Id")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Course\":" + courseId.get() + ",\"Tag\":" + tagId.get() + "}")
                                                 .when()
                                                 .post(API + "/course/CourseTagController")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/course/CourseTagController?Course=" + courseId.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(1))
                                                 .body("[0].Tag", equalTo(tagId.get())));
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

        assertStepResilienceRuntime();
    }

    /**
     * Step resilience (#6762) at the outermost layer, one instance end to end: the flaky delegate fails
     * twice and the declared {@code retry: { count: 2, every: PT2S }} re-runs it until the THIRD
     * attempt succeeds ({@code GeneratedKey == KEY-3} pins the R3 cycle exactly); the produced secret
     * flowed through {@code uses} into the writer and is GONE from the live instance data once its
     * {@code clearAfter} step completed; and after the hold task the doomed delegate exhausts its
     * single retry, the runtime conversion turns the SECOND attempt's failure into the caught BPMN
     * error, and the {@code onError} route records that exact message on the record via {@code {error}}
     * - instead of the dead-letter incident it would be without the declaration.
     */
    private void assertStepResilienceRuntime() {
        String provisionApi = API + "/provision/ProvisionController";
        AtomicInteger provision = new AtomicInteger();
        restAssuredExecutor.execute(() -> provision.set(given().contentType("application/json")
                                                               .body("{\"Title\":\"tenant A\"}")
                                                               .when()
                                                               .post(provisionApi)
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .path("Id")));

        // The retries recovered the flaky call and the produced apiKey reached the record through
        // the `uses` step. KEY-3 = one initial attempt + the two declared re-attempts, no more.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(provisionApi + "/" + provision.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("GeneratedKey", equalTo("KEY-3")),
                180);

        // clearAfter: the instance parks at the hold task with the secret already removed from its
        // live variables - it must not survive in the process data (or its history).
        AtomicReference<String> processId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> processId.set(given().when()
                                                               .get(provisionApi + "/" + provision.get())
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .path("ProcessId")));
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> variables = given().when()
                                                         .get("/services/bpm/bpm-processes/instance/" + processId.get() + "/variables")
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getList("$");
            assertTrue(variables.stream()
                                .anyMatch(variable -> "Id".equals(variable.get("name"))),
                    "the live instance must be inspectable (its context variables present), got: " + variables);
            assertTrue(variables.stream()
                                .noneMatch(variable -> "apiKey".equals(variable.get("name"))),
                    "clearAfter must remove the produced secret from the instance data, got: " + variables);
        }, 30);

        // Completing the hold releases the doomed call: retry count 1 = exactly two attempts, and
        // the recorded {error} message is the FINAL attempt's - the conversion fired on exhaustion,
        // not on the first failure, and the incident path was never taken.
        AtomicReference<String> holdTaskId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> tasks = given().when()
                                                     .get("/services/inbox/tasks?type=groups")
                                                     .then()
                                                     .statusCode(200)
                                                     .extract()
                                                     .jsonPath()
                                                     .getList("$");
            Map<String, Object> hold = tasks.stream()
                                            .filter(task -> "Hold".equals(task.get("name")))
                                            .findFirst()
                                            .orElseThrow(() -> new AssertionError(
                                                    "the resilience flow must park at the hold task, got: " + tasks));
            holdTaskId.set(String.valueOf(hold.get("id")));
        }, 90);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\"}")
                                                 .when()
                                                 .post("/services/inbox/tasks/" + holdTaskId.get())
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(provisionApi + "/" + provision.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("FailureMessage", equalTo("partner registration refused (attempt 2)")),
                180);
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
                                                 .post("/services/java/" + PROJECT + "/gen/events/emission/CancelApprovalTransition/run")
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
        writeProjectFile("app.intent", yaml);
    }

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
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
        removeDropFolder();
    }

    /** The inbound drop folder is outside the repository - clear it so a rerun starts empty. */
    private void removeDropFolder() {
        Path dropFolder = Paths.get("target/inbox-emission");
        if (!Files.isDirectory(dropFolder)) {
            return;
        }
        try (java.util.stream.Stream<Path> tree = Files.walk(dropFolder)) {
            for (Path path : tree.sorted(java.util.Comparator.reverseOrder())
                                 .toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to clear the inbound drop folder " + dropFolder.toAbsolutePath(), ex);
        }
    }

}
