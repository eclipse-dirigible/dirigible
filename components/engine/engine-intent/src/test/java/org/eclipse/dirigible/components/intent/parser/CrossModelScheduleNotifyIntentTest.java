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
 * A schedule whose SOURCE lives in another model may mail (dirigible #7030) - the
 * customer-statement case, where the model that owns the report is not the one that owns the
 * customer. Everything about the source row is resolved at generation time against the owner's
 * {@code .model}; what only the owner can supply is refused here.
 */
class CrossModelScheduleNotifyIntentTest {

    private static final String MODEL = """
            name: sales-invoices
            uses:
              - { model: customers }
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: total,    type: decimal, precision: 15, scale: 2 }
                  - { name: issuedOn, type: date }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, model: customers }
            reports:
              - name: CustomerStatement
                source: SalesInvoice
                dimensions: [issuedOn]
                measures: ["sum(total)"]
                parameters:
                  - { name: customer, target: Customer.name, op: like }
            """;

    private static String schedule(String notify) {
        return MODEL + """
                schedules:
                  - name: monthly-customer-statements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    model: customers
                    where:
                      - { field: openBalance, op: gt, value: 0 }
                    notify:
                %s
                """.formatted(notify);
    }

    @Test
    void theStatementMailParsesAgainstACrossModelSource() {
        IntentModel model = IntentParser.parse(schedule("""
                      to: email
                      subject: "Your account statement"
                      body: "Dear {name}, your statement is attached."
                      attach: { report: CustomerStatement, bind: { customer: name } }\
                """));

        assertEquals("Customer", model.getSchedules()
                                      .get(0)
                                      .getEntity());
        assertEquals("customers", model.getSchedules()
                                       .get(0)
                                       .getModel());
        assertEquals("CustomerStatement", model.getSchedules()
                                               .get(0)
                                               .getNotify()
                                               .getReportAttachment()
                                               .report());
    }

    @Test
    void aRelationHopOffTheSourceRowIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(schedule("""
                      to: account.email
                      subject: "Your account statement"
                      body: "Dear {name}."
                      attach: { report: CustomerStatement, bind: { customer: name } }\
                """)));

        assertTrue(ex.getMessage()
                     .contains("hops through a relation of the cross-model source [Customer]"),
                ex.getMessage());
    }

    @Test
    void aRecordDeepLinkIntoAnotherApplicationIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(schedule("""
                      to: email
                      subject: "Your account statement"
                      body: "Open it here: {recordUrl}"
                      attach: { report: CustomerStatement, bind: { customer: name } }\
                """)));

        assertTrue(ex.getMessage()
                     .contains("{recordUrl}"),
                ex.getMessage());
    }

    @Test
    void attachingTheSourcesOwnPrintIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(schedule("""
                      to: email
                      subject: "Your account statement"
                      body: "Dear {name}."
                      attach: print\
                """)));

        assertTrue(ex.getMessage()
                     .contains("the print of the cross-model source [Customer]"),
                ex.getMessage());
    }

    @Test
    void recordingTheDeliveryOutcomeOnTheOwnersRowIsRefused() {
        // The stamp (#7023) writes through the row's own repository and announces on its own failure
        // topic - both generated where the row lives, so it cannot be authored from here.
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(schedule("""
                      to: email
                      subject: "Your account statement"
                      body: "Dear {name}."
                      outcome: statementMail
                      attach: { report: CustomerStatement, bind: { customer: name } }\
                """)));

        assertTrue(ex.getMessage()
                     .contains("declares outcome [statementMail] on the cross-model source [Customer]"),
                ex.getMessage());
    }

    @Test
    void anUnboundFixedReportParameterIsStillRefused() {
        // The report rules are entity-independent, so lifting the source restriction must not lose
        // them: an unbound parameter with an initial mails every customer the same slice.
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
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
                      - { name: customer, target: Customer.name, op: like, initial: ACME }
                schedules:
                  - name: monthly-customer-statements
                    cron: "0 0 7 1 * ?"
                    entity: Customer
                    model: customers
                    notify:
                      to: email
                      subject: "Your account statement"
                      body: "Dear {name}."
                      attach: { report: CustomerStatement }
                """));

        assertTrue(ex.getMessage()
                     .contains("without binding its parameter [customer]"),
                ex.getMessage());
    }
}
