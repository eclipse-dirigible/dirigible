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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The marked-up alternative of a notify block (dirigible #7488): {@code html:} beside {@code body},
 * at every call site, with the authored markup kept literal and every interpolated value escaped.
 */
class GlueNotifyHtmlTest {

    private static final String ESCAPE = "org.eclipse.dirigible.sdk.mail.Html.escape(";

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
                  - { name: name, type: string }
                  - { name: email, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: paid, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: Reminder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice }

            processes:
              - name: InvoiceIssue
                trigger: { onCreate: Invoice }
                steps:
                  - name: mailIt
                    kind: serviceTask
                    args:
                      notify:
                        to: Customer.email
                        subject: "Invoice {number}"
                        body: "Dear {Customer.name}, invoice {number}."
                        html: "<p>Dear <b>{Customer.name}</b>, invoice {number}.</p>"
                      next: end
                  - { name: end, kind: end }

            notifications:
              - name: invoiceCreated
                event: { onCreate: Invoice }
                to: Customer.email
                subject: "Invoice {number}"
                body: "Invoice {number} created."
                html: "<p>Invoice <b>{number}</b> created.</p>"

            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [3]
                setStatus: 8
                notify:
                  to: Customer.email
                  subject: "Invoice {number} was voided"
                  body: "Invoice {number} was voided: {recordUrl}"
                  html: '<a href="{recordUrl}">Invoice {number}</a> was voided.'
              - name: RemindAll
                forEntity: Invoice
                from: [1]
                setStatus: 2
                notify:
                  forEach: Reminder
                  to: email
                  subject: "Reminder"
                  body: "A reminder."
                  html: "<p>Invoice <b>{record.number}</b> is open.</p>"

            schedules:
              - name: dunning
                cron: "0 0 8 * * ?"
                entity: Invoice
                where: [ { field: paid, op: eq, value: 0 } ]
                notify:
                  to: Customer.email
                  subject: "Reminder for invoice {number}"
                  body: "Please settle invoice {number}."
                  html: "<p>Please settle invoice <b>{number}</b>.</p>"
            """;

    @Test
    void aTransitionRendersTheMarkupWithEveryValueEscaped() {
        Map<String, Object> t = transition("VoidInvoice");
        String html = (String) t.get("notifyHtmlExpression");
        assertEquals(
                "\"<a href=\\\"\" + " + ESCAPE + "recordUrl) + \"\\\">Invoice \" + " + ESCAPE + "entity.Number) + \"</a> was voided.\"",
                html, "the markup stays literal, the deep link and the field are both values and both escaped");
        assertEquals("\"Invoice \" + entity.Number + \" was voided: \" + recordUrl", t.get("notifyBodyExpression"),
                "the plain part is exactly what it was");
    }

    @Test
    void aNotificationCarriesTheAlternativeBesideTheBody() {
        Map<String, Object> n = GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(YAML))
                                                   .get(0);
        assertEquals("\"<p>Invoice <b>\" + " + ESCAPE + "entity.Number) + \"</b> created.</p>\"", n.get("htmlExpression"));
    }

    @Test
    void aSendingStepReadsTheSameRelationLoadAsTheBody() {
        Map<String, Object> s = GlueIntentGenerator.buildSendsForTest(IntentParser.parse(YAML))
                                                   .get(0);
        assertEquals("\"<p>Dear <b>\" + " + ESCAPE + "(Customer == null ? null : Customer.Name)) + \"</b>, invoice \" + " + ESCAPE
                + "entity.Number) + \".</p>\"", s.get("notifyHtmlExpression"));
    }

    @Test
    void aScheduleCarriesTheAlternativeToo() {
        Map<String, Object> s = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(YAML))
                                                   .get(0);
        assertEquals("\"<p>Please settle invoice <b>\" + " + ESCAPE + "entity.Number) + \"</b>.</p>\"", s.get("htmlExpression"));
    }

    /**
     * Inside a fan-out the anchor is quoted by the markup alone, so the row send must still receive it.
     */
    @Test
    void aFanOutPassesTheAnchorWhenOnlyTheMarkupQuotesIt() {
        Map<String, Object> t = transition("RemindAll");
        assertEquals("true", t.get("notifyRecordScoped"));
        assertEquals("\"<p>Invoice <b>\" + " + ESCAPE + "source.Number) + \"</b> is open.</p>\"", t.get("notifyHtmlExpression"));
    }

    @Test
    void withoutHtmlNothingIsAdded() {
        String plain = YAML.replaceAll("(?m)^\\s*html: .*\\R", "");
        IntentParser.parse(plain);
        for (Map<String, Object> t : GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(plain))) {
            assertFalse(t.containsKey("notifyHtmlExpression"), t.toString());
        }
        assertFalse(GlueIntentGenerator.buildSendsForTest(IntentParser.parse(plain))
                                       .get(0)
                                       .containsKey("notifyHtmlExpression"));
        assertFalse(GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(plain))
                                       .get(0)
                                       .containsKey("htmlExpression"));
        assertFalse(GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(plain))
                                       .get(0)
                                       .containsKey("htmlExpression"));
    }

    @Test
    void htmlWithoutBodyIsRefusedNamingTheMissingKey() {
        String issues = issues(YAML.replace("    body: \"Invoice {number} created.\"\n", ""));
        assertTrue(issues.contains("notification [invoiceCreated] declares html without body"), issues);
    }

    @Test
    void aBlankHtmlIsRefused() {
        String issues = issues(YAML.replace("<p>Please settle invoice <b>{number}</b>.</p>", " "));
        assertTrue(issues.contains("schedule [dunning] notify declares a blank html"), issues);
    }

    @Test
    void theRecordScopeInHtmlFollowsTheBodyRules() {
        String issues = issues(YAML.replace("<p>Invoice <b>{record.number}</b> is open.</p>", "<p>{record.nope}</p>"));
        assertTrue(issues.contains("record-scoped placeholder [{record.nope}]: [nope] is not a field of the anchor record [Invoice]"),
                issues);
        issues = issues(YAML.replace("<p>Invoice <b>{number}</b> created.</p>", "<p>{record.number}</p>"));
        assertTrue(issues.contains("uses the record. scope in [{record.number}] without a forEach"), issues);
    }

    private static Map<String, Object> transition(String name) {
        List<Map<String, Object>> transitions = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(YAML));
        return transitions.stream()
                          .filter(t -> name.equals(t.get("name")))
                          .findFirst()
                          .orElseThrow();
    }

    private static String issues(String yaml) {
        try {
            IntentParser.parse(yaml);
            throw new AssertionError("expected the intent to be refused");
        } catch (IntentValidationException expected) {
            return String.join("; ", expected.getIssues());
        }
    }
}
