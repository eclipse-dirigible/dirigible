/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The days-past-due escalation ladder of a schedule (issue #7276) - the half of dunning that picks
 * WHICH level an overdue document is at, so the weekly tick advances First -&gt; Second -&gt; Final
 * as the document ages instead of re-sending one flat wording forever.
 *
 * <p>
 * Each refusal here is a way the ladder would read as working and not be: a {@code notify}-only
 * escalation has no record to tell a level already sent from one still due; a key without the level
 * finds the FIRST reminder of a document forever, so the seeded second and final notices are never
 * applied - the exact symptom reported; and an {@code {escalation.<field>}} the ladder does not
 * declare renders as its own literal text, which for a dunning mail means those characters reach
 * the customer.
 */
class ScheduleEscalateIntentTest {

    /**
     * The dunning shape of codbex `sales-invoices`: a level ladder, a reminder history, a weekly run.
     */
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

    @Test
    void aDunningLadderParses() {
        IntentModel model = IntentParser.parse(DUNNING);

        assertEquals(1, model.getSchedules()
                             .size());
        assertEquals("ReminderLevel", model.getSchedules()
                                           .get(0)
                                           .getEscalate()
                                           .getLadder());
        assertEquals("Level", model.getSchedules()
                                   .get(0)
                                   .getEscalate()
                                   .getInto());
    }

    @Test
    void anEscalationWithoutAGenerateIsRejected() {
        // Nothing records which level went out, so the top level reached would be re-sent every tick -
        // the behaviour the ladder exists to replace.
        String yaml = DUNNING.replaceAll("(?s)\n {4}generate:.*?\n {4}notify:", "\n    notify:");

        assertIssue(yaml, "has no generate");
    }

    @Test
    void anEscalationWhoseLevelIsNotInTheNaturalKeyIsRejected() {
        String yaml = DUNNING.replace("unique: [SalesInvoice, Level]", "unique: [SalesInvoice]");

        assertIssue(yaml, "is not part of the generate unique: key");
    }

    @Test
    void anEscalationOntoAPropertyTheGenerateAlsoAssignsIsRejected() {
        String yaml = DUNNING.replace("map: { SalesInvoice: id }", "map: { SalesInvoice: id, Level: id }");

        assertIssue(yaml, "is also assigned by the generate's map or defaults");
    }

    @Test
    void anEscalationLadderThatIsNotAnEntityIsRejected() {
        String yaml = DUNNING.replace("ladder: ReminderLevel", "ladder: DunningLevel");

        assertIssue(yaml, "ladder [DunningLevel] is not an entity of this model");
    }

    @Test
    void aNonIntegerThresholdIsRejected() {
        String yaml = DUNNING.replace("{ name: daysAfterDue, type: integer }", "{ name: daysAfterDue, type: string }");

        assertIssue(yaml, "the threshold is a whole number of days");
    }

    @Test
    void aNonDateSinceIsRejected() {
        // The ladder counts whole days; a string "due date" would not even compile into the subtraction.
        String yaml = DUNNING.replace("{ name: dueOn, type: date }", "{ name: dueOn, type: string }");

        assertIssue(yaml, "the ladder counts whole days");
    }

    @Test
    void anIntoThatDoesNotPointAtTheLadderIsRejected() {
        String yaml = DUNNING.replace("- { name: Level, kind: manyToOne, to: ReminderLevel }",
                "- { name: Level, kind: manyToOne, to: SalesInvoice }");

        assertIssue(yaml, "points at [SalesInvoice], not at the ladder [ReminderLevel]");
    }

    @Test
    void anEscalationPlaceholderOnASchedulesWithoutALadderIsRejected() {
        // Without a ladder the placeholder degrades to its own literal text - the characters
        // {escalation.name} would be mailed to the customer.
        String yaml = DUNNING.replaceAll("(?s)\n {4}escalate:.*?\n {4}generate:", "\n    generate:")
                             .replace("unique: [SalesInvoice, Level]", "unique: [SalesInvoice]");

        assertIssue(yaml, "declares no escalate: ladder");
    }

    @Test
    void anEscalationPlaceholderTheLadderDoesNotDeclareIsRejected() {
        String yaml = DUNNING.replace("{escalation.wording}", "{escalation.tone}");

        assertIssue(yaml, "is not a field of the escalate ladder [ReminderLevel]");
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
