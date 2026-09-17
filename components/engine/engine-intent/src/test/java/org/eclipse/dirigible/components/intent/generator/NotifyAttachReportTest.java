/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * {@code attach: { report, bind }} - a notify block mailing a parameterized REPORT render, the
 * customer-statement mail. Covers the glue a schedule emits (the bound parameters as expressions
 * over the queried row), the print scaffold the render resolves through, and the parse rules that
 * refuse a mail whose report is not scoped to its recipient.
 */
class NotifyAttachReportTest {

    private static final String MODEL = """
            name: ar
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: language, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: email, type: string }
                  - { name: openBalance, type: decimal }
                  - { name: periodStart, type: date }
                  - { name: periodEnd, type: date }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
            reports:
              - name: CustomerStatement
                source: SalesInvoice
                dimensions: [issuedOn]
                measures: ["sum(total)"]
                parameters:
                  - { name: fromDate, target: issuedOn, op: ge }
                  - { name: toDate, target: issuedOn, op: le }
                  - { name: customer, target: Customer.name, op: eq, initial: "-" }
            """;

    private static final String STATEMENT_SCHEDULE = """
            schedules:
              - name: monthly-statements
                cron: "0 0 7 1 * ?"
                entity: Customer
                where:
                  - { field: openBalance, op: gt, value: 0 }
                notify:
                  to: email
                  subject: "Your statement"
                  body: "Please find attached your account statement."
                  attach:
                    report: CustomerStatement
                    bind: { customer: name, fromDate: periodStart, toDate: periodEnd }
            """;

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> bindings(Map<String, Object> entry) {
        return (List<Map<String, Object>>) entry.get("attachReportBindings");
    }

    private static Map<String, Object> schedule(String yaml) {
        List<Map<String, Object>> schedules = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml));
        assertEquals(1, schedules.size(), "one schedule expected");
        return schedules.get(0);
    }

    @Test
    void scheduleBindsTheReportParametersFromTheQueriedRow() {
        Map<String, Object> entry = schedule(MODEL + STATEMENT_SCHEDULE);
        assertEquals("notify", entry.get("action"));
        assertEquals("report", entry.get("attach"));
        assertEquals("CustomerStatement", entry.get("attachReport"));
        // No document is attached, so nothing in the entry claims one - a mail must never say it
        // carries a print it has no feeder for.
        assertEquals("", entry.get("attachEntity"));
        // Authored order, each value an access off the loop row (`entity` is the job template's local).
        assertEquals(List.of(Map.of("parameter", "customer", "expression", "entity.Name"),
                Map.of("parameter", "fromDate", "expression", "entity.PeriodStart"),
                Map.of("parameter", "toDate", "expression", "entity.PeriodEnd")), bindings(entry));
    }

    @Test
    void aRelationHopBindLoadsTheRelatedRowOnce() {
        String yaml = MODEL + """
                schedules:
                  - name: monthly-statements
                    cron: "0 0 7 1 * ?"
                    entity: SalesInvoice
                    notify:
                      to: Customer.email
                      subject: "Statement for {Customer.name}"
                      body: "attached"
                      attach:
                        report: CustomerStatement
                        bind: { customer: Customer.name }
                """;
        Map<String, Object> entry = schedule(yaml);
        // Null-guarded, like every other one-hop read: a customer-less invoice binds no value and the
        // report falls back to the parameter's initial instead of throwing mid-send.
        assertEquals(List.of(Map.of("parameter", "customer", "expression", "(Customer == null ? null : Customer.Name)")), bindings(entry));
        // The recipient, the subject placeholder and the binding all read the same relation, so the
        // generated job declares ONE local for it - a second declaration would not compile.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loads = (List<Map<String, Object>>) entry.get("relationLoads");
        assertEquals(1, loads.size(), "loads: " + loads);
        assertEquals("Customer", loads.get(0)
                                      .get("local"));
    }

    @Test
    void theFileNameDefaultsToTheReportAndTheRowsIdentity() {
        Map<String, Object> entry = schedule(MODEL + STATEMENT_SCHEDULE);
        // A mailbox of statements is only self-describing when each one names its recipient.
        assertEquals("\"CustomerStatement \" + \"Customer \" + entity.Id + \".pdf\"", entry.get("attachFileNameExpression"));
    }

    @Test
    void anAuthoredFileNameAndLanguageApplyToTheReportRenderToo() {
        String yaml = MODEL + """
                schedules:
                  - name: monthly-statements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: "Your statement"
                      body: "attached"
                      fileName: "Statement_{name}"
                      languageFrom: Country.language
                      attach:
                        report: CustomerStatement
                        bind: { customer: name }
                """;
        Map<String, Object> entry = schedule(yaml);
        // The same sanitizing the document attachment applies - a name is going into a mail header.
        assertEquals("\"Statement_\" + org.eclipse.dirigible.sdk.print.FileNames.part(entity.Name) + \".pdf\"",
                entry.get("attachFileNameExpression"));
        assertEquals("Country", entry.get("attachLanguageFkProperty"));
        assertEquals("Country", entry.get("attachLanguageTargetEntity"));
        assertTrue(String.valueOf(entry.get("attachLanguageExpression"))
                         .contains("attachLanguageSource.Language"),
                "language: " + entry.get("attachLanguageExpression"));
    }

    @Test
    void aStandaloneNotificationCarriesTheSameReportKeys() {
        // The notify block is one shape at four call sites, so the report attachment has to ride on all
        // of them - a lifecycle notification is the second.
        String yaml = MODEL + """
                notifications:
                  - name: statement-on-request
                    event: { onCreate: Customer }
                    to: email
                    subject: "Your statement"
                    body: "attached"
                    attach:
                      report: CustomerStatement
                      bind: { customer: name }
                """;
        List<Map<String, Object>> notifications = GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(yaml));
        assertEquals(1, notifications.size());
        assertEquals("report", notifications.get(0)
                                            .get("attach"));
        assertEquals("CustomerStatement", notifications.get(0)
                                                       .get("attachReport"));
        assertEquals(List.of(Map.of("parameter", "customer", "expression", "entity.Name")), bindings(notifications.get(0)));
    }

    @Test
    void onlyMailedReportsGetAPrintScaffold() {
        IntentModel plain = IntentParser.parse(MODEL);
        assertTrue(NotifySupport.attachedReports(plain)
                                .isEmpty(),
                "a report nothing mails needs no print template");
        assertEquals(java.util.Set.of("CustomerStatement"), NotifySupport.attachedReports(IntentParser.parse(MODEL + STATEMENT_SCHEDULE)));
    }

    @Test
    void aCrossModelSourceMailsTheStatementFromTheModelThatOwnsTheReport() {
        // The suite layout the mechanism was filed for (dirigible #7030): the report lives with the
        // invoices, the customer lives in another module, and the schedule can only be declared where
        // the report is - so its source is cross-model. With no repository the owner's facts fall back
        // to the naming convention, which is enough to assert the emitted mail plan.
        String yaml = """
                name: sales-invoices
                uses:
                  - { model: customers }
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: issuedOn, type: date }
                      - { name: total,    type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: customers }
                reports:
                  - name: CustomerStatement
                    source: SalesInvoice
                    dimensions: [issuedOn]
                    measures: ["sum(total)"]
                    parameters:
                      - { name: customer, target: Customer.name, op: like }
                schedules:
                  - name: monthly-customer-statements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    model: customers
                    where:
                      - { field: openBalance, op: gt, value: 0 }
                    notify:
                      to: email
                      subject: "Your account statement"
                      body: "Dear {name}, your statement is attached."
                      attach: { report: CustomerStatement, bind: { customer: name } }
                """;
        Map<String, Object> entry = schedule(yaml);
        assertEquals("notify", entry.get("action"));
        // The row is the OWNER's, so the job imports its gen package and queries its repository.
        assertEquals(true, entry.get("sourceCrossModel"));
        assertEquals("customers", entry.get("sourceModel"));
        assertEquals("Customer", entry.get("perspective"));
        // Every path resolves off the loop row exactly as a same-model source's does - the recipient,
        // the placeholder and the report binding.
        assertEquals("entity.Email", entry.get("toExpression"));
        assertTrue(String.valueOf(entry.get("bodyExpression"))
                         .contains("entity.Name"),
                "body: " + entry.get("bodyExpression"));
        assertEquals("report", entry.get("attach"));
        assertEquals("CustomerStatement", entry.get("attachReport"));
        assertEquals(List.of(Map.of("parameter", "customer", "expression", "entity.Name")), bindings(entry));
        // The report is this model's, so nothing about the mail crosses back: no relation load, and no
        // claim of a document print the owner alone could render.
        assertEquals(List.of(), entry.get("relationLoads"));
        assertEquals("", entry.get("attachEntity"));
    }

    private static String parseError(String yaml) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        return String.join("\n", failure.getIssues());
    }

    @Test
    void anUnboundSelectorIsRefused() {
        // The failure mode this rule exists for: `customer` unbound leaves every recipient the same
        // fixed slice, and nothing about the rendered PDF says it is the wrong customer's.
        String yaml = MODEL + """
                schedules:
                  - name: monthly-statements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: "Your statement"
                      body: "attached"
                      attach:
                        report: CustomerStatement
                        bind: { fromDate: periodStart }
                """;
        String issues = parseError(yaml);
        assertTrue(issues.contains("without binding its parameter [customer]"), issues);
        assertTrue(issues.contains("initial [-]"), issues);
    }

    @Test
    void anUnknownReportAndAnUnknownParameterAreNamed() {
        String unknownReport = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { report: Statement, bind: { customer: name } }
                """;
        assertTrue(parseError(unknownReport).contains("attach references unknown report [Statement]"), parseError(unknownReport));

        String unknownParameter = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { report: CustomerStatement, bind: { customer: name, fromdate: periodStart } }
                """;
        String issues = parseError(unknownParameter);
        assertTrue(issues.contains("attach bind [fromdate] is not a parameter of report [CustomerStatement]"), issues);
        // A case slip is the common one, so it is named rather than left to the author to spot.
        assertTrue(issues.contains("did you mean [fromDate]?"), issues);
    }

    @Test
    void aBindSourceMustResolveOnTheRecordTheMessageIsAbout() {
        String yaml = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { report: CustomerStatement, bind: { customer: fullName } }
                """;
        assertTrue(parseError(yaml).contains("attach bind [customer] [fullName] is not a field of [Customer]"), parseError(yaml));
    }

    @Test
    void attachMustNameAReportAndTheShapeIsClosed() {
        String noReport = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { bind: { customer: name } }
                """;
        assertTrue(parseError(noReport).contains("attach must name the report to render"), parseError(noReport));

        String strayKey = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { report: CustomerStatement, binds: { customer: name } }
                """;
        assertTrue(parseError(strayKey).contains("unknown key [binds]"), parseError(strayKey));
    }

    @Test
    void aReportWithNothingToBindIsRefused() {
        String yaml = """
                name: ar
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                reports:
                  - name: Revenue
                    source: SalesInvoice
                    measures: ["sum(total)"]
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: { report: Revenue }
                """;
        assertTrue(parseError(yaml).contains("which declares no parameters"), parseError(yaml));
    }

    @Test
    void theScalarAttachFormsStillWorkAndTheMessageNamesAllThree() {
        String yaml = MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                      attach: pdf
                """;
        String issues = parseError(yaml);
        assertTrue(issues.contains("has unsupported attach [pdf]"), issues);
        assertTrue(issues.contains("report: <name>"), issues);
        // And a plain-text block stays a plain-text block.
        Map<String, Object> plain = schedule(MODEL + """
                schedules:
                  - name: s
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    notify:
                      to: email
                      subject: s
                      body: b
                """);
        assertEquals("", plain.get("attach"));
        assertEquals("", plain.get("attachReport"));
        assertFalse(bindings(plain).iterator()
                                   .hasNext());
    }
}
