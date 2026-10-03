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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A settlement sizes an allocation from the invoice's allocation ROWS, never from its {@code paid}
 * roll-up column (#7559). The column is maintained asynchronously and lags the rows, so a payment
 * event racing it sized an allocation the junction's capacity guard - which re-sums the rows - then
 * refused, failing the listener mid-loop. The rows the settlement sums are the rows the paid
 * roll-up counts: its {@code where:} clauses travel on the settlement descriptor, so the
 * settlement, the roll-up and the guard read one authored definition.
 */
class GlueSettlementOpenAmountTest {

    private static final String YAML = """
            name: settle
            entities:
              - name: Invoice
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: date,  type: date }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                  - { name: paid,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: AllocationStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: InvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - { name: Payment, kind: manyToOne, to: Payment, required: true }
                  - { name: Status, kind: manyToOne, to: AllocationStatus, function: EntityStatus, init: ACTIVE }
            seeds:
              - name: allocationStatuses
                entity: AllocationStatus
                rows:
                  - { id: 1, name: ACTIVE }
                  - { id: 2, name: CANCELLED, stage: cancelled }
            rollups:
              - name: invoicePaid
                entity: InvoicePayment
                via: Invoice
                field: paid
                op: sum
                of: amount
                capacity: total
                where:
                  - { field: Status, op: ne, value: CANCELLED }
            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: date,
                  match: [Customer] }
            """;

    private static final String WHERE = "    where:\n      - { field: Status, op: ne, value: CANCELLED }\n";

    @SuppressWarnings("unchecked")
    @Test
    void theInvoicesRowsAreNarrowedByThePaidRollupsFilter() {
        List<Map<String, Object>> filter = (List<Map<String, Object>>) settlement(YAML).get("invoiceRowsFilter");

        assertEquals(1, filter.size(), "a cancelled allocation consumes nothing the paid roll-up does not count: " + filter);
        assertEquals("ne", filter.get(0)
                                 .get("op"));
        assertEquals("Status", filter.get(0)
                                     .get("property"));
        assertEquals("2", ((Map<?, ?>) filter.get(0)
                                             .get("value")).get("text"),
                "the CANCELLED name resolves to its seed id, exactly as on the roll-up");
    }

    @Test
    void anUnfilteredPaidRollupNarrowsNothing() {
        assertFalse(settlement(YAML.replace(WHERE, "")).containsKey("invoiceRowsFilter"),
                "with no filter every allocation row consumes the invoice, as the guard counts it");
    }

    @Test
    void aFilteredRollupOfAnotherColumnIsNotTheSettlementsDefinition() {
        String yaml = YAML.replace("    field: paid\n", "    field: total\n")
                          .replace("    capacity: total\n", "");

        assertFalse(settlement(yaml).containsKey("invoiceRowsFilter"),
                "only the roll-up keeping the settlement's paid column decides which rows count");
    }

    private static Map<String, Object> settlement(String yaml) {
        List<Map<String, Object>> cleanups = GlueIntentGenerator.buildSettlementCleanupsForTest(IntentParser.parse(yaml));
        assertEquals(1, cleanups.size());
        return cleanups.get(0);
    }
}
