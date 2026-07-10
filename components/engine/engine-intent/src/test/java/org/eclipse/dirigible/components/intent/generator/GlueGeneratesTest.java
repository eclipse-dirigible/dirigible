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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code generates} entries the {@link GlueIntentGenerator} emits: the pre-rendered
 * field assignment expressions (source copy vs {@code now} / literal), the composition-item foreign
 * keys, and the cross-model target gen folder resolution.
 */
class GlueGeneratesTest {

    private static final String YAML = """
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
                  - { name: date, type: date }
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
                map:
                  Customer: Customer
                  Note: note
                defaults:
                  Date: now
                  Note: "from quote"
                items:
                  from: QuoteItem
                  to: OrderItem
                  map:
                    Amount: amount
            """;

    @SuppressWarnings("unchecked")
    @Test
    void rendersHeaderAssignmentsItemsAndKeys() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> generates = GlueIntentGenerator.buildGeneratesForTest(model);
        assertEquals(1, generates.size());
        Map<String, Object> g = generates.get(0);

        assertEquals("OrderFromQuote", g.get("className"));
        assertEquals("Quote", g.get("fromEntity"));
        assertEquals("Order", g.get("toEntity"));
        assertEquals(false, g.get("crossModel"));
        assertEquals(true, g.get("hasItems"));
        // A document child's FK back to its master is, by convention, the master entity's name.
        assertEquals("Quote", g.get("srcFkProperty"));
        assertEquals("Order", g.get("toFkProperty"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) g.get("fieldAssignments");
        // map first (source copy), then defaults (now / literal).
        assertTrue(fields.contains(Map.of("targetProp", "Customer", "expr", "source.Customer")));
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "source.Note")));
        assertTrue(fields.contains(Map.of("targetProp", "Date", "expr", "java.time.LocalDate.now()")));
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "\"from quote\"")));

        List<Map<String, Object>> itemFields = (List<Map<String, Object>>) g.get("itemFieldAssignments");
        assertTrue(itemFields.contains(Map.of("targetProp", "Amount", "expr", "srcItem.Amount")));

        // No completion hook declared - the template's #if renders nothing.
        assertEquals("", g.get("sourceStatusProperty"));
        assertEquals("", g.get("sourceStatusValue"));
    }

    @Test
    void completionHookResolvesTheSourceStatusRelation() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: ProformaStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Proforma
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                    relations:
                      - { name: Status, kind: manyToOne, to: ProformaStatus, function: EntityStatus, init: 1 }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                generates:
                  - name: invoice-from-proforma
                    from: Proforma
                    to: Invoice
                    forEntity: Proforma
                    sourceStatus: 3
                """);
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);

        // Pre-resolved to the EntityStatus FK property + the seed id the source flips to.
        assertEquals("Status", g.get("sourceStatusProperty"));
        assertEquals("3", g.get("sourceStatusValue"));
        assertEquals("Proforma", g.get("fromPerspective"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resolvesCrossModelTargetGenFolderAndFlag() {
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
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(model)
                                                   .get(0);
        assertEquals(true, g.get("crossModel"));
        assertEquals("sales", g.get("toModel"));
        // With no repository, the cross-model perspective falls back to the entity name (convention).
        assertEquals("SalesInvoice", g.get("toPerspective"));
        assertEquals(false, ((Boolean) g.get("hasItems")).booleanValue());

        List<Map<String, Object>> fields = (List<Map<String, Object>>) g.get("fieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "Note", "expr", "source.Note")));
    }

    @Test
    void integerDecimalAndBooleanLiteralsRenderTyped() {
        String yaml = """
                name: sales
                entities:
                  - name: Quote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: rate, type: decimal }
                      - { name: qty, type: integer }
                      - { name: active, type: boolean }
                generates:
                  - name: order-from-quote
                    from: Quote
                    to: Order
                    defaults:
                      Qty: "3"
                      Rate: "1.5"
                      Active: "true"
                """;
        IntentModel model = IntentParser.parse(yaml);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) GlueIntentGenerator.buildGeneratesForTest(model)
                                                                                          .get(0)
                                                                                          .get("fieldAssignments");
        assertTrue(fields.contains(Map.of("targetProp", "Qty", "expr", "3")));
        assertTrue(fields.contains(Map.of("targetProp", "Rate", "expr", "new java.math.BigDecimal(\"1.5\")")));
        assertTrue(fields.contains(Map.of("targetProp", "Active", "expr", "true")));
        assertFalse(fields.isEmpty());
    }
}
