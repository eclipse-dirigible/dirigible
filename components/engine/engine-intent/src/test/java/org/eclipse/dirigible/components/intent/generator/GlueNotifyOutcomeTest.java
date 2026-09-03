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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The delivery-outcome half of a notify block (dirigible #7023): where the attempt is recorded, on
 * which record, and what the failure announces. A notify block is fail-soft, so without this the
 * only trace of a mail that never left was a server log line.
 */
class GlueNotifyOutcomeTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: sendOutcome, type: string, length: 128, readOnly: true }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: SendInvoice
                forEntity: Invoice
                from: [1]
                setStatus: 2
                notify:
                  to: customer.email
                  subject: "Invoice {number}"
                  body: "Attached."
                  outcome: sendOutcome
            """;

    @Test
    void theTransitionStampsTheAttemptOnTheRecordItMailedAbout() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Object> t = GlueIntentGenerator.buildTransitionsForTest(model)
                                                   .get(0);
        assertEquals("true", t.get("notify"));
        assertEquals("SendOutcome", t.get("notifyOutcomeProperty"));
        assertEquals("Invoice", t.get("notifyOutcomeEntity"));
        assertEquals("Id", t.get("notifyOutcomeKeyProperty"));
        // The RAW perspective: the failure topic is built from it, and the sanitized form is only the
        // Java package the repository lives in.
        assertEquals("Invoice", t.get("notifyOutcomePerspective"));
        assertEquals(String.valueOf(NotifySupport.OUTCOME_LENGTH), t.get("notifyOutcomeLength"));
    }

    /** A notify block that records nothing keeps the keys, empty - the templates compare them. */
    @Test
    void withoutAnOutcomeTheKeysAreEmptyRatherThanAbsent() {
        IntentModel model = IntentParser.parse(YAML.replaceAll("(?m)^\\s*outcome: sendOutcome\\R", ""));
        Map<String, Object> t = GlueIntentGenerator.buildTransitionsForTest(model)
                                                   .get(0);
        assertEquals("true", t.get("notify"));
        assertEquals("", t.get("notifyOutcomeProperty"));
        assertEquals("", t.get("notifyOutcomeEntity"));
        assertEquals("", t.get("notifyOutcomeKeyProperty"));
    }

    /**
     * A FAN-OUT stamps the ROW, not the record the rows hang off: the row carries the recipient, so the
     * row is what a delivery succeeded or failed for - one trace per recipient, which is the whole
     * reason a fan-out's outcome cannot be one aggregate line.
     */
    @Test
    void aFanOutStampsEachRow() {
        String yaml = """
                name: payroll
                entities:
                  - name: RunStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                  - name: PayrollRun
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: RunStatus, function: EntityStatus, init: 1 }
                  - name: Payslip
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: delivery, type: string, length: 128 }
                    relations:
                      - { name: payrollRun, kind: manyToOne, to: PayrollRun }
                      - { name: employee, kind: manyToOne, to: Employee }
                transitions:
                  - name: PublishRun
                    forEntity: PayrollRun
                    from: [1]
                    setStatus: 2
                    notify:
                      forEach: Payslip
                      to: employee.email
                      subject: "Your payslip"
                      body: "See attached."
                      outcome: delivery
                """;
        Map<String, Object> t = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(yaml))
                                                   .get(0);
        assertEquals("Payslip", t.get("forEach"));
        assertEquals("Delivery", t.get("notifyOutcomeProperty"));
        assertEquals("Payslip", t.get("notifyOutcomeEntity"), "the stamp belongs to the row that was mailed, not to the run");
        assertEquals("Id", t.get("notifyOutcomeKeyProperty"));
    }

    /** A schedule already runs per row, so the queried row is what the stamp lands on. */
    @Test
    void aScheduleStampsTheRowItMailed() {
        String yaml = """
                name: dunning
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: overdue, type: integer }
                      - { name: reminderOutcome, type: string, length: 128 }
                    relations:
                      - { name: customer, kind: manyToOne, to: Customer }
                schedules:
                  - name: dailyReminders
                    entity: Invoice
                    cron: "0 0 7 * * ?"
                    where: [{ field: overdue, op: gt, value: 0 }]
                    notify:
                      to: customer.email
                      subject: "Reminder"
                      body: "Please pay."
                      outcome: reminderOutcome
                """;
        List<Map<String, Object>> schedules = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml));
        Map<String, Object> s = schedules.get(0);
        assertEquals("notify", s.get("action"));
        assertEquals("ReminderOutcome", s.get("notifyOutcomeProperty"));
        assertEquals("Invoice", s.get("notifyOutcomeEntity"));
    }

    /** A generate schedule mails nothing, so it carries the empty keys the template compares. */
    @Test
    void aGenerateScheduleRecordsNothing() {
        String yaml = """
                name: renewals
                entities:
                  - name: Contract
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: due, type: integer }
                  - name: Renewal
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: contract, kind: manyToOne, to: Contract }
                schedules:
                  - name: nightlyRenewals
                    entity: Contract
                    cron: "0 0 2 * * ?"
                    where: [{ field: due, op: gt, value: 0 }]
                    generate: { name: renew, from: Contract, to: Renewal, map: { contract: id } }
                """;
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                   .get(0);
        assertEquals("generate", s.get("action"));
        assertEquals("", s.get("notifyOutcomeProperty"));
    }

    /**
     * Everything the parser refuses, each because it would otherwise truncate or misroute the trace.
     */
    @Test
    void refusesAnOutcomeItCannotStamp() {
        assertTrue(refused(YAML.replace("outcome: sendOutcome", "outcome: nosuchfield")).contains("is not a field of [Invoice]"));
        assertTrue(refused(YAML.replace("outcome: sendOutcome", "outcome: customer")).contains("is a relation of [Invoice]"));
        assertTrue(refused(YAML.replace("outcome: sendOutcome", "outcome: number")
                               .replace("- { name: number, type: string }", "- { name: number, type: integer }")).contains(
                                       "must be a string field, was [integer]"));
        // A length that truncates the reason truncates it at the DATABASE, where nothing reports it.
        assertTrue(refused(YAML.replace("length: 128", "length: 20")).contains("too short for a delivery reason"));
    }

    private static String refused(String yaml) {
        try {
            IntentParser.parse(yaml);
            throw new AssertionError("expected the intent to be refused");
        } catch (IntentValidationException expected) {
            return String.join("; ", expected.getIssues());
        }
    }
}
