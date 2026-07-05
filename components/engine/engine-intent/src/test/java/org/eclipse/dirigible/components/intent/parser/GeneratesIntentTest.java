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

import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * Parse + validation coverage for the {@code generates} (create-from) block.
 */
class GeneratesIntentTest {

    private static final String SAME_MODEL = """
            name: sales
            entities:
              - name: Quote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: note, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: QuoteItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Quote, kind: manyToOne, to: Quote, composition: true, required: true }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: note, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            generates:
              - name: order-from-quote
                from: Quote
                to: Order
                label: "Create Order"
                icon: file-plus
                map:
                  Customer: Customer
                  Note: note
                defaults:
                  Note: "from quote"
                items:
                  from: QuoteItem
                  to: OrderItem
                  map:
                    Amount: amount
            """;

    @Test
    void parsesASameModelGenerateWithItems() {
        IntentModel model = IntentParser.parse(SAME_MODEL);
        assertEquals(1, model.getGenerates()
                             .size());
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals("order-from-quote", g.getName());
        assertEquals("Quote", g.getFrom());
        assertEquals("Order", g.getTo());
        assertEquals("Create Order", g.getLabel());
        // forEntity defaults to `from` when unset.
        assertEquals("Quote", g.getForEntity());
        // scope defaults to entity (a create-from needs a source record).
        assertEquals("entity", g.getScope());
        assertEquals("Customer", g.getMap()
                                  .get("Customer"));
        assertEquals("QuoteItem", g.getItems()
                                   .getFrom());
        assertEquals("OrderItem", g.getItems()
                                   .getTo());
    }

    @Test
    void parsesACrossModelGenerate() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: sales }
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string }
                generates:
                  - name: invoice-from-timesheet
                    from: ProjectTimesheet
                    to: SalesInvoice
                    uses: sales
                    map:
                      Note: note
                """;
        IntentModel model = IntentParser.parse(yaml);
        GeneratesIntent g = model.getGenerates()
                                 .get(0);
        assertEquals("sales", g.getUses());
        assertEquals("SalesInvoice", g.getTo());
    }

    @Test
    void rejectsUnknownFromEntity() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Missing
                    to: Order
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("from references unknown entity [Missing]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsUnknownToWhenNotCrossModel() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Order
                    to: SalesInvoice
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("to references unknown entity [SalesInvoice]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsUnknownUsesAlias() {
        String yaml = """
                name: timesheets
                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: ProjectTimesheet
                    to: SalesInvoice
                    uses: sales
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("uses unknown model alias [sales]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsMapSourceThatIsNotASourceProperty() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                generates:
                  - name: bad
                    from: Quote
                    to: Order
                    map:
                      Note: doesNotExist
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("source [doesNotExist] is not a field or to-one relation")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsOneHopRelationFieldMapping() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                generates:
                  - name: bad
                    from: Quote
                    to: Order
                    map:
                      Note: Customer.name
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("relation.field path") && i.contains("not yet supported")),
                "got: " + ex.getIssues());
    }
}
