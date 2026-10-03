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

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A settlement's allocation order is TOTAL (#7556). Ordered by {@code order:} alone, documents
 * sharing that value - every invoice issued on the same day - came back in whatever order the
 * database returned them, and a payment could pay the newer invoice in full while leaving the older
 * one partly open. The order is now the authored fields followed by the document's primary key, so
 * ties fall back to creation order on both sides of the settlement.
 */
class GlueSettlementOrderTest {

    private static final String YAML = """
            name: settle
            entities:
              - name: Invoice
                fields:
                  - { name: invoiceId, type: integer, primaryKey: true, generated: true }
                  - { name: date,      type: date }
                  - { name: number,    type: string, length: 20 }
                  - { name: total,  type: decimal, precision: 18, scale: 2 }
                  - { name: paid,   type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Payment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: date,      type: date }
                  - { name: number,    type: string, length: 20 }
                  - { name: amount,    type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: InvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - { name: Payment, kind: manyToOne, to: Payment, required: true }
            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: ORDER,
                  match: [Customer] }
            """;

    @Test
    void aSingleOrderFieldIsBrokenByEachSidesPrimaryKey() {
        Map<String, Object> settlement = settlement("date");

        assertEquals(List.of("Date", "InvoiceId"), settlement.get("invoiceOrder"),
                "same-day invoices must settle in creation order, never in the order the database returns them");
        assertEquals(List.of("Date", "Id"), settlement.get("paymentOrder"),
                "same-day payments must be drawn in creation order - by the payment's own key, not the invoice's");
    }

    @Test
    void anOrderListSortsByEveryFieldBeforeThePrimaryKey() {
        Map<String, Object> settlement = settlement("[date, number]");

        assertEquals(List.of("Date", "Number", "InvoiceId"), settlement.get("invoiceOrder"));
        assertEquals(List.of("Date", "Number", "Id"), settlement.get("paymentOrder"));
    }

    @Test
    void anOrderThatAlreadyEndsInThePrimaryKeyIsNotSortedTwice() {
        Map<String, Object> settlement = settlement("[date, invoiceId]");

        assertEquals(List.of("Date", "InvoiceId"), settlement.get("invoiceOrder"));
    }

    @Test
    void everyOrderFieldMustBeAFieldOfTheInvoice() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml("[date, dueOn]")));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("order [dueOn] is not a field of [Invoice]")),
                ex.getIssues()
                  .toString());
    }

    @Test
    void anEmptyOrderIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml("[]")));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("settlement [autoSettle] must name at least one order field")),
                ex.getIssues()
                  .toString());
    }

    private static Map<String, Object> settlement(String order) {
        List<Map<String, Object>> cleanups = GlueIntentGenerator.buildSettlementCleanupsForTest(IntentParser.parse(yaml(order)));
        assertEquals(1, cleanups.size());
        return cleanups.get(0);
    }

    private static String yaml(String order) {
        return YAML.replace("order: ORDER", "order: " + order);
    }
}
