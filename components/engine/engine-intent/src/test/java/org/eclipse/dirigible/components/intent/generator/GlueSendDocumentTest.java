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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The declarative send-document-by-e-mail glue: the reusable notify block at its three embedded
 * call sites (a process step, a transition, a schedule) plus the {@code attach: print} half that
 * renders the record's own document. Asserts what the templates consume - every expression is
 * pre-rendered here, so a missing key silently generates a feature-less class.
 */
class GlueSendDocumentTest {

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
                function: Document
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: number
                    type: string
                    documentTitle: true
                    number: { series: Invoice, format: "INV{seq:07}", stampOn: create }
                  - { name: paid, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: InvoiceItem
                function: DocumentItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            processes:
              - name: InvoiceIssue
                trigger: { onCreate: Invoice }
                steps:
                  - { name: issue, kind: userTask, args: { assignee: issuer, next: mailIt } }
                  - name: mailIt
                    kind: serviceTask
                    args:
                      notify:
                        to: Customer.email
                        subject: "Invoice {number}"
                        body: "Dear {Customer.name}, your invoice is attached."
                        attach: print
                      next: end
                  - { name: end, kind: end }

            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [3]
                setStatus: 8
                label: Void
                icon: ban
                notify:
                  to: Customer.email
                  subject: "Invoice {number} was voided"
                  body: "The invoice has been cancelled."

            schedules:
              - name: dunning
                cron: "0 0 8 * * *"
                entity: Invoice
                where: [ { field: paid, op: eq, value: 0 } ]
                notify:
                  to: Customer.email
                  subject: "Reminder for invoice {number}"
                  body: "Please settle the attached invoice."
                  attach: print
                  language: bg
            """;

    @Test
    void aSendingStepBecomesADelegateThatMailsTheDocument() {
        List<Map<String, Object>> sends = GlueIntentGenerator.buildSendsForTest(IntentParser.parse(YAML));

        assertEquals(1, sends.size());
        Map<String, Object> send = sends.get(0);
        assertEquals("InvoiceIssue", send.get("process"));
        assertEquals("mailIt", send.get("step"));
        // The class name the BPMN service task binds - it must match BpmnIntentGenerator's handler.
        assertEquals("InvoiceIssueMailItSend", send.get("className"));
        assertEquals("Invoice", send.get("entity"));
        assertEquals("Id", send.get("keyProperty"));
        assertEquals("intValue", send.get("keyAccessor"));
        assertEquals("true", send.get("notify"));
        // The recipient is a one-hop relation.field, so the delegate loads the Customer by FK first.
        assertEquals("(Customer == null ? null : Customer.Email)", send.get("notifyToExpression"));
        assertEquals("\"Invoice \" + entity.Number", send.get("notifySubjectExpression"));
        assertTrue(String.valueOf(send.get("notifyBodyExpression"))
                         .contains("(Customer == null ? null : Customer.Name)"),
                "a body placeholder must resolve through the same one-hop load");
        assertEquals(1, ((List<?>) send.get("notifyRelationLoads")).size());
        assertPrintAttachment(send, "en");
    }

    @Test
    void aTransitionCarriesItsMailAfterTheFlip() {
        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(YAML))
                                                            .get(0);

        assertEquals("true", transition.get("notify"));
        assertEquals("(Customer == null ? null : Customer.Email)", transition.get("notifyToExpression"));
        assertEquals("\"Invoice \" + entity.Number + \" was voided\"", transition.get("notifySubjectExpression"));
        // The print feeder is fed with the record's key, so the key property must reach the template.
        assertEquals("Id", transition.get("keyProperty"));
        // No attach on this one: a plain-text notice.
        assertEquals("", transition.get("attach"));
    }

    @Test
    void aTransitionWithoutNotifyRendersNothingToSend() {
        String yaml = YAML.replace("""
                    notify:
                      to: Customer.email
                      subject: "Invoice {number} was voided"
                      body: "The invoice has been cancelled."
                """, "");
        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(yaml))
                                                            .get(0);

        // The template's #if($notify == "true") renders no send block at all.
        assertEquals("false", transition.get("notify"));
        assertEquals("", transition.get("attach"));
    }

    @Test
    void aScheduleNotifyCanAttachThePrintInItsOwnLanguage() {
        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(YAML))
                                                          .get(0);

        assertEquals("notify", schedule.get("action"));
        assertEquals("Id", schedule.get("keyProperty"));
        assertPrintAttachment(schedule, "bg");
    }

    @Test
    void aLifecycleNotificationCanAttachThePrintToo() {
        String yaml = YAML + """

                notifications:
                  - name: invoiceIssued
                    event: { onUpdate: Invoice }
                    to: Customer.email
                    subject: "Invoice {number}"
                    body: "Attached."
                    attach: print
                """;
        Map<String, Object> notification = GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(yaml))
                                                              .get(0);

        assertEquals("Id", notification.get("keyProperty"));
        assertEquals("-updated", notification.get("topicSuffix"));
        assertPrintAttachment(notification, "en");
    }

    @Test
    void aGenerateScheduleStillCarriesEmptyAttachmentKeys() {
        String yaml = """
                name: billing
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Reminder
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                schedules:
                  - name: monthly
                    cron: "0 0 1 1 * *"
                    entity: Customer
                    generate:
                      to: Reminder
                      map: { Customer: id }
                """;
        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(IntentParser.parse(yaml))
                                                          .get(0);

        assertEquals("generate", schedule.get("action"));
        // Present but empty: an undefined Velocity variable renders as its own name, so the notify
        // branch's #if must always have something to compare.
        assertEquals("", schedule.get("attach"));
        assertEquals("", schedule.get("attachFileNameExpression"));
    }

    @Test
    void attachingThePrintOfANonDocumentIsRejected() {
        String yaml = """
                name: billing
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                notifications:
                  - name: welcome
                    event: { onCreate: Customer }
                    to: email
                    subject: "Welcome"
                    body: "Hello"
                    attach: print
                """;
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(failure.getMessage()
                          .contains("attach: print needs [Customer] to be a document"),
                "attaching a print to an entity with no line-items child must fail at parse time: " + failure.getMessage());
    }

    @Test
    void anUnknownAttachmentKindIsRejected() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("attach: print", "attach: pdf")));

        assertTrue(failure.getMessage()
                          .contains("unsupported attach [pdf]"),
                "only the documented attach values may pass: " + failure.getMessage());
    }

    @Test
    void aSendingStepCannotAlsoSetAField() {
        // A second arg on the sending step itself (same indentation as the notify block).
        String yaml = YAML.replace("          next: end", "          setField: number\n          value: X\n          next: end");
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(failure.getMessage()
                          .contains("cannot be combined with setField"),
                "a send step's work IS the message - it must stand alone: " + failure.getMessage());
    }

    /** The four keys the templates render the attachment from. */
    private static void assertPrintAttachment(Map<String, Object> entry, String language) {
        assertEquals("print", entry.get("attach"));
        assertEquals("Invoice", entry.get("attachEntity"), "the print template + feeder are the document entity's");
        assertEquals(language, entry.get("attachLanguage"));
        // A document with a number: field names the attachment after it - the customer receives
        // INV0000042.pdf, not "Invoice 42.pdf" (which is only the fallback).
        assertTrue(String.valueOf(entry.get("attachFileNameExpression"))
                         .contains("entity.Number"),
                "the file name must prefer the document number: " + entry.get("attachFileNameExpression"));
        assertTrue(String.valueOf(entry.get("attachFileNameExpression"))
                         .endsWith("+ \".pdf\""),
                "the attachment must be named as a PDF");
    }
}
