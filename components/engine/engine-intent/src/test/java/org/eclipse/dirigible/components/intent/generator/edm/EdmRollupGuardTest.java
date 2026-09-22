/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The capacity guard a capacity-bearing sum roll-up stamps on its CHILD entity: the synchronous
 * check in the child's generated repository that refuses a write overdrawing the parent.
 *
 * The child is always local (it owns the event the roll-up binds to), so the guard is emitted from
 * this model whether the parent is local or owned by another module - which is what lets both sides
 * of an allocation be guarded (#7410). Only the parent's coordinates differ.
 */
class EdmRollupGuardTest {

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> guardsOf(String yaml, String childEntity) {
        Map<String, Object> json = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "test");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) json.get("model")).get("entities");
        for (Map<String, Object> entity : entities) {
            if (childEntity.equals(entity.get("name"))) {
                return (List<Map<String, Object>>) entity.get("rollupGuards");
            }
        }
        return null;
    }

    /** The single guard of a child that carries exactly one. */
    private static Map<String, Object> guardOf(String yaml, String childEntity) {
        List<Map<String, Object>> guards = guardsOf(yaml, childEntity);
        if (guards == null) {
            return null;
        }
        assertEquals(1, guards.size(), "expected exactly one guard on " + childEntity);
        return guards.get(0);
    }

    private static final String LOCAL = """
            name: sales-invoices
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: total,   type: decimal }
                  - { name: paid,    type: decimal }
                  - { name: balance, type: decimal }
              - name: SalesInvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice }
            rollups:
              - { name: invoicePaid, entity: SalesInvoicePayment, via: SalesInvoice, field: paid,
                  op: sum, of: amount, capacity: total, balance: balance }
            """;

    @Test
    void aLocalParentIsAddressedByItsPlainNameInThisProjectsGenFolder() {
        Map<String, Object> guard = guardOf(LOCAL, "SalesInvoicePayment");
        assertNotNull(guard, "a capacity-bearing roll-up must stamp its child with a guard");
        assertEquals("SalesInvoice", guard.get("parentEntity"));
        assertEquals("SalesInvoice", guard.get("parentPerspective"));
        assertEquals("Total", guard.get("capacityField"));
        assertEquals("Amount", guard.get("ofField"));
        assertEquals("SalesInvoice", guard.get("fkProperty"));
        // Empty, so the DAO imports the parent from this project's own gen folder - a local guard must
        // render exactly as it did before the cross-model direction was opened.
        assertEquals("", guard.get("parentGenFolder"));
    }

    private static final String CROSS_MODEL_PARENT = """
            name: sales-invoices
            uses:
              - { model: customer-payments }
            entities:
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: customer-payments }
            rollups:
              - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, via: CustomerPayment,
                  field: allocated, op: sum, of: amount, capacity: amount, balance: unapplied }
            """;

    @Test
    void aCrossModelParentCarriesTheOwnersGenFolder() {
        // The payment side of the allocation: the link rows are local, the pot is owned by
        // customer-payments. Without the owner's gen folder the DAO would address a type in its own
        // package and fail the whole client-Java batch.
        Map<String, Object> guard = guardOf(CROSS_MODEL_PARENT, "SalesInvoiceCustomerPayment");
        assertNotNull(guard, "a capacity-bearing roll-up on a foreign parent must still guard its local child");
        assertEquals("CustomerPayment", guard.get("parentEntity"));
        assertEquals("customer-payments", guard.get("parentGenFolder"));
        assertEquals("Amount", guard.get("capacityField"));
        assertEquals("CustomerPayment", guard.get("fkProperty"));
    }

    @Test
    void aRollupWithoutACapacityStampsNoGuard() {
        Map<String, Object> guard =
                guardOf(CROSS_MODEL_PARENT.replace(", capacity: amount, balance: unapplied", ""), "SalesInvoiceCustomerPayment");
        assertNull(guard, "only a capacity-bearing roll-up installs a guard");
    }

    private static final String JUNCTION = """
            name: sales-invoices
            uses:
              - { model: customer-payments }
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: total,   type: decimal }
                  - { name: paid,    type: decimal }
                  - { name: balance, type: decimal }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice,    kind: manyToOne, to: SalesInvoice }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: customer-payments }
            rollups:
              - { name: invoicePaid, entity: SalesInvoiceCustomerPayment, via: SalesInvoice, field: paid,
                  op: sum, of: amount, capacity: total, balance: balance }
              - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, via: CustomerPayment,
                  field: allocated, op: sum, of: amount, capacity: amount, balance: unapplied }
            """;

    /**
     * The case the feature exists for: an allocation may exceed neither the invoice's payable nor the
     * payment's amount. Both capacities used to collapse into ONE guard, and the roll-up whose
     * declaration lost the race generated its sum, balance and handlers with no guard behind them and
     * no warning at Generate (#7448).
     */
    @Test
    void aJunctionGuardedOnBothParentsCarriesBothGuards() {
        List<Map<String, Object>> guards = guardsOf(JUNCTION, "SalesInvoiceCustomerPayment");
        assertNotNull(guards, "a junction with two capacity-bearing roll-ups must carry guards");
        assertEquals(2, guards.size(), "one guard per capacity-bearing roll-up: " + guards);

        Map<String, Object> invoiceGuard = guards.get(0);
        assertEquals("SalesInvoice", invoiceGuard.get("parentEntity"));
        assertEquals("SalesInvoice", invoiceGuard.get("fkProperty"));
        assertEquals("Total", invoiceGuard.get("capacityField"));
        assertEquals("", invoiceGuard.get("parentGenFolder"));

        Map<String, Object> paymentGuard = guards.get(1);
        assertEquals("CustomerPayment", paymentGuard.get("parentEntity"));
        assertEquals("CustomerPayment", paymentGuard.get("fkProperty"));
        assertEquals("Amount", paymentGuard.get("capacityField"));
        assertEquals("customer-payments", paymentGuard.get("parentGenFolder"));
    }

    /**
     * Declaration order decides nothing but the order of the emitted checks - both are still emitted.
     */
    @Test
    void theOrderOfTheDeclarationsDoesNotDecideWhichGuardSurvives() {
        List<Map<String, Object>> guards = guardsOf(JUNCTION, "SalesInvoiceCustomerPayment");
        List<Map<String, Object>> swapped = guardsOf(SWAPPED_JUNCTION, "SalesInvoiceCustomerPayment");
        assertEquals(2, swapped.size(), "one guard per capacity-bearing roll-up, whatever the order: " + swapped);
        assertEquals(guards.get(0), swapped.get(1));
        assertEquals(guards.get(1), swapped.get(0));
    }

    private static final String SWAPPED_JUNCTION = JUNCTION.replace("""
              - { name: invoicePaid, entity: SalesInvoiceCustomerPayment, via: SalesInvoice, field: paid,
                  op: sum, of: amount, capacity: total, balance: balance }
              - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, via: CustomerPayment,
                  field: allocated, op: sum, of: amount, capacity: amount, balance: unapplied }
            """, """
              - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, via: CustomerPayment,
                  field: allocated, op: sum, of: amount, capacity: amount, balance: unapplied }
              - { name: invoicePaid, entity: SalesInvoiceCustomerPayment, via: SalesInvoice, field: paid,
                  op: sum, of: amount, capacity: total, balance: balance }
            """);

    /**
     * The same relation and capacity named twice describes ONE check; emitting it twice would refuse
     * nothing extra and duplicate the parent's import, which does not compile.
     */
    @Test
    void twoRollupsOverTheSameRelationAndCapacityShareOneGuard() {
        String yaml = LOCAL + """
                  - { name: invoicePaidAgain, entity: SalesInvoicePayment, via: SalesInvoice, field: balance,
                      op: sum, of: amount, capacity: total }
                """;
        List<Map<String, Object>> guards = guardsOf(yaml, "SalesInvoicePayment");
        assertEquals(1, guards.size(), "the identical guard is emitted once: " + guards);
    }
}
