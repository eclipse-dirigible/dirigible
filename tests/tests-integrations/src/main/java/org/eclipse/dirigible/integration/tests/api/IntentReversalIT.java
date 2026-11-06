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
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.awaitility.Awaitility;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;

/**
 * A {@code reversals:} action (issue #7886) reverses a posted journal entry at runtime, through the
 * published application: the generated create-from compiles, creates the negated twin with every
 * line on its own side, links the two both ways past the original's {@code immutableWhen}, nets the
 * pair to zero, and refuses a second reversal, the reversal of a reversal, a draft, and a twin
 * dated into a CLOSED period.
 *
 * <p>
 * An entry an {@code onCreate} posting drafted for a payment, once rejected or reversed, is history
 * (#7959): the payment's next edit drafts a new entry from what it says now.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentReversalIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "reversal";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String REVERSE = EVENTS + "/ReverseEntryGenerate/run";
    private static final String POST = EVENTS + "/PostEntryTransition/run";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final String LINES = API + "/entry/EntryItemController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    private static final String ACCOUNTS = API + "/account/AccountController";
    private static final String JOURNALS = API + "/journal/JournalController";
    private static final String JOURNAL_LINES = API + "/journal/JournalLineController";
    private static final String CLOSE_YEAR = EVENTS + "/CloseYearGenerate";
    private static final String YEAR_BALANCE = "/services/java/" + PROJECT + "/gen/yearbalance/api/reports/YearBalanceController";
    /**
     * Numbers read as the decimals they are written as - an amount compared exactly, never as a float.
     */
    private static final RestAssuredConfig EXACT = RestAssuredConfig.config()
                                                                    .jsonConfig(JsonConfig.jsonConfig()
                                                                                          .numberReturnType(NumberReturnType.BIG_DECIMAL));

    private static final String INTENT_YAML = """
            name: reversal
            description: manual Reverse of a posted entry - the negated twin, linked both ways

            entities:
              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: FiscalPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 100 }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }

              - name: Entry
                immutableWhen: "Status == POSTED"
                immutableInPeriod: { period: FiscalPeriod, date: date }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date, required: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status,     kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - { name: Storno,     kind: manyToOne, to: Entry }
                  - { name: ReversedBy, kind: manyToOne, to: Entry }
                  - { name: Payment,    kind: manyToOne, to: Payment }

              - name: Account
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, required: true, length: 20 }
                relations:
                  - { name: ClosingAccount, kind: manyToOne, to: Account }

              - name: FiscalYear
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 20 }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }

              # The ledger a year close books into (#8329): its reversal keeps the original's date, so
              # reversing a closing entry reopens the year it closed.
              - name: Journal
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date, required: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status,     kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - { name: Year,       kind: manyToOne, to: FiscalYear }
                  - { name: Storno,     kind: manyToOne, to: Journal }
                  - { name: ReversedBy, kind: manyToOne, to: Journal }

              - name: JournalLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Journal, kind: manyToOne, to: Journal, composition: true, required: true }
                  - { name: Account, kind: manyToOne, to: Account, required: true }

              - name: EntryItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                  - { name: rate,   type: decimal, precision: 18, scale: 6 }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }

            seeds:
              - name: entry-statuses
                entity: EntryStatus
                rows:
                  - { id: 1, name: DRAFT,     stage: draft }
                  - { id: 2, name: POSTED,    stage: live }
                  - { id: 3, name: CANCELLED, stage: cancelled }
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }

            transitions:
              - name: PostEntry
                forEntity: Entry
                from: [DRAFT]
                setStatus: POSTED
              # The accountant rejects a draft the posting made.
              - name: RejectEntry
                forEntity: Entry
                from: [DRAFT]
                setStatus: CANCELLED
              - name: PostJournal
                forEntity: Journal
                from: [DRAFT]
                setStatus: POSTED

            reports:
              - name: YearBalance
                kind: balance
                source: JournalLine
                date: Journal.date
                debit: debit
                credit: credit
                dimensions: [Account.id, Account.ClosingAccount]

            # The year close (#8329): one journal whose lines take every result account's balance off it,
            # and one counter line per closing account receiving the opposite.
            generates:
              - name: closeYear
                from: FiscalYear
                to: Journal
                label: Close year
                map: { Year: id, date: endDate }
                defaults: { note: Year close }
                items:
                  fromReport: YearBalance
                  parameters: { fromDate: startDate, toDate: endDate }
                  to: JournalLine
                  where:
                    - { field: Account.ClosingAccount, op: ne, value: null }
                  map: { Account: Account.id, debit: closingCredit, credit: closingDebit }
                  counter: { Account: Account.ClosingAccount, debit: closingDebit, credit: closingCredit }
                undo: reverse

            postings:
              - name: paymentPosting
                event: { onCreate: Payment }
                creates: Entry
                backReference: Payment
                map: { date: date }
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }

            reversals:
              - name: reverseEntry
                forEntity: Entry
                when: "Status == POSTED"
                label: Reverse
                link: Storno
                backLink: ReversedBy
                negate: [debit, credit]
                defaults: { date: now }
                refuse:
                  - { when: "ReversedBy != null", message: "This entry is already reversed" }
                  - { when: "Storno != null", message: "A reversal cannot be reversed - reverse the original entry" }
              - name: reverseJournal
                forEntity: Journal
                when: "Status == POSTED"
                link: Storno
                backLink: ReversedBy
                negate: [debit, credit]
                refuse:
                  - { when: "ReversedBy != null", message: "This journal is already reversed" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /** The generated handlers log through the client SDK, under {@code app.}; attached per test. */
    private LogsAsserter postingLogs;

    @BeforeEach
    void attachLogsAsserter() {
        postingLogs = new LogsAsserter("app.gen.events", Level.ERROR);
    }

    @Test
    void a_posted_entry_is_reversed_by_its_negated_twin_linked_both_ways() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        YearMonth month = YearMonth.now();
        int period = create(API + "/fiscalperiod/FiscalPeriodController", "{\"Name\":\"" + month + "\",\"StartDate\":\"" + month.atDay(1)
                + "\",\"EndDate\":\"" + month.atEndOfMonth() + "\",\"Status\":1}");
        LocalDate now = LocalDate.now();
        String today = now.toString();

        int original = postedEntry(today, "accrual");

        // A draft is not offered the action, and refused it when called anyway.
        int draft = create(ENTRIES, "{\"Date\":\"" + today + "\",\"Note\":\"draft\"}");
        reverse(draft, 409, "current status is [1]");

        // Reverse the posted entry: the twin is a DRAFT dated today, pointing at the original.
        int twin = reverse(original);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + twin)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Storno", equalTo(original))
                                                 .body("Status", equalTo(1))
                                                 .body("Date", equalTo(today))
                                                 .body("Note", equalTo("accrual"))
                                                 .body("ReversedBy", nullValue()),
                TIMEOUT_SECONDS);
        // Every line is copied onto its own side, the amount negated; the rate is copied as it stands.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + twin)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("Debit", equalTo(java.util.Arrays.asList(-100.0f, null)))
                                                 .body("Credit", equalTo(java.util.Arrays.asList(null, -100.0f)))
                                                 .body("Rate", equalTo(List.of(1.5f, 1.5f)))
                                                 .body("Note", equalTo(List.of("expense", "liability"))),
                TIMEOUT_SECONDS);
        // A typo in the master filter is the caller's mistake: a 400 naming the parameter, like an
        // unparsable $limit, and never a 500 carrying Hibernate's coercion message (dirigible #8237).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=abc")
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("Invalid value for query parameter [Entry]: abc"))
                                                 .body("message", not(containsString("coercing"))),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "?$limit=abc")
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("Invalid value for query parameter [$limit]: abc")),
                TIMEOUT_SECONDS);
        // The original stays as it was, and names its reversal - written past its immutableWhen.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + original)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ReversedBy", equalTo(twin))
                                                 .body("Status", equalTo(2)),
                TIMEOUT_SECONDS);
        // The pair nets to zero on both sides once the twin is posted.
        post(twin);
        assertEquals(0.0, sum(original, "Debit") + sum(twin, "Debit"), 0.0001);
        assertEquals(0.0, sum(original, "Credit") + sum(twin, "Credit"), 0.0001);

        // A second Reverse of the same entry and a Reverse of the reversal are refused by name.
        reverse(original, 409, "This entry is already reversed");
        reverse(twin, 409, "A reversal cannot be reversed - reverse the original entry");

        // A twin dated into a CLOSED period is refused by the period lock, and nothing of it remains.
        int another = postedEntry(today, "rent");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + period + ",\"Name\":\"" + month + "\",\"StartDate\":\"" + month.atDay(1)
                                                         + "\",\"EndDate\":\"" + month.atEndOfMonth() + "\",\"Status\":2}")
                                                 .when()
                                                 .put(API + "/fiscalperiod/FiscalPeriodController/" + period)
                                                 .then()
                                                 .statusCode(200));
        reverse(another, 400, "The FiscalPeriod covering this date is closed");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + another)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ReversedBy", nullValue()),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Storno", not(hasItem(another))),
                TIMEOUT_SECONDS);
        // Reopened: the methods share the database, and the other one books into this month.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + period + ",\"Name\":\"" + month + "\",\"StartDate\":\"" + month.atDay(1)
                                                         + "\",\"EndDate\":\"" + month.atEndOfMonth() + "\",\"Status\":1}")
                                                 .when()
                                                 .put(API + "/fiscalperiod/FiscalPeriodController/" + period)
                                                 .then()
                                                 .statusCode(200));
    }

    /**
     * The payment's entry is the LIVE one (#7959): an edit after the accountant rejected it, or after a
     * posted twin reversed it, drafts a new entry from the edited figures; rejections accumulate, and a
     * live draft is rewritten in place. A posted entry nobody reversed is still left alone.
     */
    @Test
    void an_edit_redrafts_the_entry_a_rejection_or_a_posted_reversal_retired() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        String today = LocalDate.now()
                                .toString();

        // Rejected, edited: a new draft. Rejected again, edited again: another. The live draft is then
        // rewritten by the next edit, never duplicated.
        int payment = create(PAYMENTS, "{\"Date\":\"" + today + "\",\"Amount\":10}");
        transition("RejectEntry", draftOf(payment, 10.0f));
        updatePayment(payment, today, 12);
        transition("RejectEntry", draftOf(payment, 12.0f));
        updatePayment(payment, today, 13);
        int live = draftOf(payment, 13.0f);
        updatePayment(payment, today, 14);
        awaitLines(live, 14.0f);
        assertEquals(3, entriesOf(payment).size(), "one entry per rejection round, and the live draft rewritten in place");

        // Posted, reversed by a twin that is posted too (the twin carries the payment's back-reference),
        // edited: a new draft.
        int reversed = create(PAYMENTS, "{\"Date\":\"" + today + "\",\"Amount\":20}");
        int original = draftOf(reversed, 20.0f);
        post(original);
        post(reverse(original));
        updatePayment(reversed, today, 25);
        draftOf(reversed, 25.0f);

        // Posted and not reversed, edited: reported, not redrafted.
        int posted = create(PAYMENTS, "{\"Date\":\"" + today + "\",\"Amount\":30}");
        int entry = draftOf(posted, 30.0f);
        post(entry);
        updatePayment(posted, today, 35);
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage("Payment [" + posted + "] changed after Entry [" + entry + "]", Level.ERROR));
        assertEquals(List.of(entry), entriesOf(posted));
        awaitLines(entry, 30.0f);
    }

    /**
     * A year close generated from a balance report's rows (#8329): the closing journal takes every
     * result account's year balance off it - so the result accounts' trial balance is zero once it is
     * posted - and books the opposite on their closing account, one counter line for both. A second run
     * is refused while that journal stands; undo books its reversal, and once the reversal is posted
     * the year's balances are what they were and the year can be closed again.
     */
    @Test
    void a_year_is_closed_from_its_balance_report_and_the_undo_reopens_it() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int profitAndLoss = create(ACCOUNTS, "{\"Code\":\"899\"}");
        int expense = create(ACCOUNTS, "{\"Code\":\"600\",\"ClosingAccount\":" + profitAndLoss + "}");
        int revenue = create(ACCOUNTS, "{\"Code\":\"700\",\"ClosingAccount\":" + profitAndLoss + "}");
        int cash = create(ACCOUNTS, "{\"Code\":\"501\"}");
        int year = create(API + "/fiscalyear/FiscalYearController",
                "{\"Name\":\"2020\",\"StartDate\":\"2020-01-01\",\"EndDate\":\"2020-12-31\"}");
        postedJournal("2020-03-01", expense, "100.25", cash);
        postedJournal("2020-06-01", cash, "300.10", revenue);
        postedJournal("2020-09-01", expense, "50.00", cash);
        // After the year: not closed with it.
        postedJournal("2021-01-15", expense, "7.00", cash);

        int closing = closeYear(year, 200);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(JOURNALS + "/" + closing)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Year", equalTo(year))
                                                 .body("Date", equalTo("2020-12-31"))
                                                 .body("Note", equalTo("Year close"))
                                                 .body("Status", equalTo(1)),
                TIMEOUT_SECONDS);
        // One line per result account, its year balance taken off; one counter line on the closing account.
        assertLine(closing, expense, "0", "150.25");
        assertLine(closing, revenue, "300.10", "0");
        assertLine(closing, profitAndLoss, "150.25", "300.10");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(JOURNAL_LINES + "?Journal=" + closing)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(3)),
                TIMEOUT_SECONDS);

        transition("PostJournal", closing);
        // The result accounts' trial balance over the year is zero; the closing account holds the profit.
        assertBalance(expense, "2020-12-31", "150.25", "150.25");
        assertBalance(revenue, "2020-12-31", "300.10", "300.10");
        assertBalance(profitAndLoss, "2020-12-31", "150.25", "300.10");

        // A second run is refused while the closing journal stands.
        closeYear(year, 409);

        // Undo books the journal's reversal: a negated twin linked both ways.
        AtomicReference<String> undone = new AtomicReference<>();
        restAssuredExecutor.execute(() -> undone.set(given().contentType("application/json")
                                                            .body("{\"id\":" + year + "}")
                                                            .when()
                                                            .post(CLOSE_YEAR + "/undo")
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .asString()),
                TIMEOUT_SECONDS);
        JsonPath reversals = JsonPath.from(undone.get());
        assertEquals(1, reversals.getList("")
                                 .size(),
                "the undo should answer the one reversal it booked, got: " + undone.get());
        assertEquals(closing, reversals.getInt("[0].Storno"));
        int twin = reversals.getInt("[0].Id");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + year + "}")
                                                 .when()
                                                 .post(CLOSE_YEAR + "/undo")
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString("is already reversed by [" + twin + "]")),
                TIMEOUT_SECONDS);
        // Not posted yet, the reversal retires nothing: the year stays closed.
        closeYear(year, 409);

        transition("PostJournal", twin);
        assertBalance(expense, "2020-12-31", "150.25", "0");
        assertBalance(revenue, "2020-12-31", "0", "300.10");
        assertBalance(profitAndLoss, "2020-12-31", "0", "0");
        // Reopened: the year closes again, to the same lines.
        int again = closeYear(year, 200);
        assertLine(again, expense, "0", "150.25");
        assertLine(again, profitAndLoss, "150.25", "300.10");
    }

    /** A POSTED journal of one debit line and one credit line of {@code amount}. */
    private void postedJournal(String date, int debitAccount, String amount, int creditAccount) {
        int journal = create(JOURNALS, "{\"Date\":\"" + date + "\"}");
        create(JOURNAL_LINES, "{\"Journal\":" + journal + ",\"Account\":" + debitAccount + ",\"Debit\":" + amount + "}");
        create(JOURNAL_LINES, "{\"Journal\":" + journal + ",\"Account\":" + creditAccount + ",\"Credit\":" + amount + "}");
        transition("PostJournal", journal);
    }

    /** Runs the close; the journal it answers on 200, else 0. */
    private int closeYear(int year, int status) {
        AtomicInteger journal = new AtomicInteger();
        restAssuredExecutor.execute(() -> {
            var response = given().contentType("application/json")
                                  .body("{\"id\":" + year + "}")
                                  .when()
                                  .post(CLOSE_YEAR + "/run")
                                  .then()
                                  .statusCode(status);
            if (status == 200) {
                // The run answers its JSON as text, the way every generated create-from does.
                journal.set(JsonPath.from(response.extract()
                                                  .asString())
                                    .getInt("Id"));
            } else {
                response.body(containsString("already generated Journal"));
            }
        }, TIMEOUT_SECONDS);
        return journal.get();
    }

    /** The journal's one line on the account carries exactly these amounts. */
    private void assertLine(int journal, int account, String debit, String credit) {
        restAssuredExecutor.execute(() -> given().config(EXACT)
                                                 .when()
                                                 .get(JOURNAL_LINES + "?Journal=" + journal)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Account == " + account + " }", hasSize(1))
                                                 .body("find { it.Account == " + account + " }.Debit",
                                                         comparesEqualTo(new BigDecimal(debit)))
                                                 .body("find { it.Account == " + account + " }.Credit",
                                                         comparesEqualTo(new BigDecimal(credit))),
                TIMEOUT_SECONDS);
    }

    /** The account's closing debit and credit in the balance report over 2020-01-01..{@code to}. */
    private void assertBalance(int account, String to, String debit, String credit) {
        String row = "find { it['Account Id'] == " + account + " }";
        restAssuredExecutor.execute(() -> given().config(EXACT)
                                                 .when()
                                                 .get(YEAR_BALANCE + "?fromDate=2020-01-01&toDate=" + to)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(row + "['Closing Debit']", comparesEqualTo(new BigDecimal(debit)))
                                                 .body(row + "['Closing Credit']", comparesEqualTo(new BigDecimal(credit))),
                TIMEOUT_SECONDS);
    }

    /** The payment's one DRAFT entry, once its lines carry {@code amount} on both sides. */
    private int draftOf(int payment, float amount) {
        String drafts = "findAll { it.Payment == " + payment + " && it.Status == 1 }";
        AtomicInteger entry = new AtomicInteger();
        restAssuredExecutor.execute(() -> entry.set(given().when()
                                                           .get(ENTRIES + "?$limit=1000")
                                                           .then()
                                                           .statusCode(200)
                                                           .body(drafts, hasSize(1))
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt(drafts + "[0].Id")),
                TIMEOUT_SECONDS);
        awaitLines(entry.get(), amount);
        return entry.get();
    }

    private List<Integer> entriesOf(int payment) {
        AtomicReference<List<Integer>> entries = new AtomicReference<>();
        restAssuredExecutor.execute(() -> entries.set(given().when()
                                                             .get(ENTRIES + "?$limit=1000")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .jsonPath()
                                                             .getList("findAll { it.Payment == " + payment + " }.Id", Integer.class)));
        return entries.get();
    }

    private void awaitLines(int entry, float amount) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + entry)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("findAll { it.Debit == (" + amount + ") }.size()", equalTo(1))
                                                 .body("findAll { it.Credit == (" + amount + ") }.size()", equalTo(1)),
                TIMEOUT_SECONDS);
    }

    private void updatePayment(int payment, String date, int amount) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + payment + ",\"Date\":\"" + date + "\",\"Amount\":" + amount + "}")
                                                 .when()
                                                 .put(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200));
    }

    private void transition(String name, int entry) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + entry + "}")
                                                 .when()
                                                 .post(EVENTS + "/" + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    /** A POSTED entry dated {@code date}: a debit line and a credit line of 100 each. */
    private int postedEntry(String date, String note) {
        int entry = create(ENTRIES, "{\"Date\":\"" + date + "\",\"Note\":\"" + note + "\"}");
        create(LINES, "{\"Entry\":" + entry + ",\"Debit\":100,\"Rate\":1.5,\"Note\":\"expense\"}");
        create(LINES, "{\"Entry\":" + entry + ",\"Credit\":100,\"Rate\":1.5,\"Note\":\"liability\"}");
        post(entry);
        return entry;
    }

    private void post(int entry) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + entry + "}")
                                                 .when()
                                                 .post(POST)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private int reverse(int entry) {
        AtomicReference<String> answer = new AtomicReference<>();
        restAssuredExecutor.execute(() -> answer.set(given().contentType("application/json")
                                                            .body("{\"id\":" + entry + "}")
                                                            .when()
                                                            .post(REVERSE)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .asString()),
                TIMEOUT_SECONDS);
        Matcher id = Pattern.compile("\"Id\"\\s*:\\s*(\\d+)")
                            .matcher(answer.get());
        assertTrue(id.find(), "the reversal should answer the created twin, got: " + answer.get());
        return Integer.parseInt(id.group(1));
    }

    private void reverse(int entry, int status, String message) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + entry + "}")
                                                 .when()
                                                 .post(REVERSE)
                                                 .then()
                                                 .statusCode(status)
                                                 .body(containsString(message)),
                TIMEOUT_SECONDS);
    }

    private double sum(int entry, String side) {
        AtomicReference<List<Float>> values = new AtomicReference<>();
        restAssuredExecutor.execute(() -> values.set(given().when()
                                                            .get(LINES + "?Entry=" + entry)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getList(side, Float.class)),
                TIMEOUT_SECONDS);
        return values.get()
                     .stream()
                     .filter(java.util.Objects::nonNull)
                     .mapToDouble(Float::doubleValue)
                     .sum();
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
