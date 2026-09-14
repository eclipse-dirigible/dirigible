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
import org.junit.jupiter.api.Test;

/**
 * The glue a dunning schedule emits (issue #7276): a tick that BOTH writes a
 * {@code PaymentReminder} and mails it, and picks the level from a days-past-due ladder.
 *
 * <p>
 * The two halves are one entry, not two jobs: the mail is what the generate's natural key gates, so
 * the same (invoice, level) is never sent twice, and the record beside it is what makes the
 * reminder history a record of what actually went out rather than only of the manual clicks.
 */
class GlueScheduleEscalationTest {

    private static final String DUNNING = """
            name: billing
            entities:
              - name: ReminderLevel
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: daysAfterDue, type: integer }
                  - { name: wording, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: dueOn, type: date }
                  - { name: contactEmail, type: string }
              - name: PaymentReminder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: sentOn, type: date }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Level, kind: manyToOne, to: ReminderLevel }
            schedules:
              - name: overdue-invoice-reminders
                cron: "0 0 8 * * MON"
                entity: SalesInvoice
                where:
                  - { field: dueOn, op: lt, value: CURRENT_DATE }
                escalate:
                  ladder: ReminderLevel
                  after: daysAfterDue
                  since: dueOn
                  into: Level
                generate:
                  to: PaymentReminder
                  unique: [SalesInvoice, Level]
                  map: { SalesInvoice: id }
                  defaults: { sentOn: now }
                notify:
                  to: contactEmail
                  subject: "Invoice {number} - {escalation.name}"
                  body: "{escalation.wording}"
            """;

    @SuppressWarnings("unchecked")
    @Test
    void aDunningTickBothGeneratesAndNotifies() {
        Map<String, Object> schedule = dunning();

        assertEquals(Boolean.TRUE, schedule.get("generates"));
        assertEquals(Boolean.TRUE, schedule.get("notifies"));
        assertEquals("PaymentReminder", schedule.get("genToEntity"));
        // The mail plan is kept alongside the create-from, so the two run in one pass over the row.
        assertEquals("entity.ContactEmail", schedule.get("toExpression"));
        assertEquals(Boolean.TRUE, schedule.get("hasGenUnique"));
        List<Map<String, Object>> unique = (List<Map<String, Object>>) schedule.get("genUnique");
        assertTrue(unique.stream()
                         .anyMatch(u -> "Level".equals(u.get("property")) && "escalation.Id".equals(u.get("expr"))),
                "the level belongs in the key that makes each level send once: " + unique);
    }

    @SuppressWarnings("unchecked")
    @Test
    void theChosenLevelIsWrittenOntoTheGeneratedRecord() {
        List<Map<String, Object>> assignments = (List<Map<String, Object>>) dunning().get("genFieldAssignments");

        assertTrue(assignments.contains(Map.of("targetProp", "Level", "expr", "escalation.Id")), "assignments: " + assignments);
        // ...alongside what the author mapped - the escalation adds a column, it does not replace them.
        assertTrue(assignments.contains(Map.of("targetProp", "SalesInvoice", "expr", "entity.Id")), "assignments: " + assignments);
    }

    @Test
    void theLadderFactsReachTheTemplate() {
        Map<String, Object> schedule = dunning();

        assertEquals(Boolean.TRUE, schedule.get("hasEscalation"));
        assertEquals("ReminderLevel", schedule.get("escalationEntity"));
        assertEquals("DaysAfterDue", schedule.get("escalationAfterProperty"));
        assertEquals("DueOn", schedule.get("escalationSinceProperty"));
        assertEquals("Level", schedule.get("escalationIntoProperty"));
        assertEquals("Id", schedule.get("escalationKeyProperty"));
        // The local the job holds the level in IS the scope the placeholders render against.
        assertEquals("escalation", schedule.get("escalationLocal"));
    }

    @Test
    void theMessageReadsTheLevelItIsAt() {
        Map<String, Object> schedule = dunning();

        assertTrue(String.valueOf(schedule.get("subjectExpression"))
                         .contains("escalation.Name"),
                "subject: " + schedule.get("subjectExpression"));
        assertTrue(String.valueOf(schedule.get("bodyExpression"))
                         .contains("escalation.Wording"),
                "body: " + schedule.get("bodyExpression"));
    }

    @Test
    void aPlainNotifyScheduleIsUnchanged() {
        // The combined form is additive: a schedule with one action keeps the flags of that action
        // alone, so nothing authored before this renders differently.
        String yaml = DUNNING.replaceAll("(?s)\n {4}escalate:.*?\n {4}notify:", "\n    notify:")
                             .replace("Invoice {number} - {escalation.name}", "Invoice {number} is overdue")
                             .replace("{escalation.wording}", "Please settle the attached invoice.");

        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                          .get(0);

        assertEquals(Boolean.FALSE, schedule.get("generates"));
        assertEquals(Boolean.TRUE, schedule.get("notifies"));
        assertEquals("notify", schedule.get("action"));
        assertEquals(Boolean.FALSE, schedule.get("hasEscalation"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aRelationBothHalvesReachIsLoadedOnce() {
        // The recipient hops through Customer and the generate maps a field off the same relation: the
        // generated loop declares each local ONCE, so a duplicated load would not compile.
        String yaml = """
                name: billing
                entities:
                  - name: ReminderLevel
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: daysAfterDue, type: integer }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: dueOn, type: date }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: PaymentReminder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: sentTo, type: string }
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                      - { name: Level, kind: manyToOne, to: ReminderLevel }
                schedules:
                  - name: overdue-invoice-reminders
                    cron: "0 0 8 * * MON"
                    entity: SalesInvoice
                    where:
                      - { field: dueOn, op: lt, value: CURRENT_DATE }
                    escalate:
                      ladder: ReminderLevel
                      after: daysAfterDue
                      since: dueOn
                      into: Level
                    generate:
                      to: PaymentReminder
                      unique: [SalesInvoice, Level]
                      map: { SalesInvoice: id, sentTo: Customer.email }
                    notify:
                      to: Customer.email
                      subject: "Your invoice is overdue"
                      body: "Please settle it."
                """;

        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                          .get(0);

        List<Map<String, Object>> loads = (List<Map<String, Object>>) schedule.get("relationLoads");
        assertEquals(1, loads.size(), "loads: " + loads);
        assertEquals("Customer", loads.get(0)
                                      .get("local"));
    }

    private static Map<String, Object> dunning() {
        IntentModel model = IntentParser.parse(DUNNING);
        List<Map<String, Object>> schedules = GlueIntentGenerator.buildSchedulesForTest(model);
        assertEquals(1, schedules.size());
        return schedules.get(0);
    }
}
