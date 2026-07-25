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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code aggregates} glue the {@link GlueIntentGenerator} emits: a two-key on-hand sum
 * of the signed stock ledger materialised into a ProductAvailability target keyed by Product+Store.
 * The keys must be to-one relations of BOTH source and target; the descriptor carries source/target
 * coordinates + the summed field + the target field. Structural glue - the keyed-upsert handler
 * template is a later stage (kf-catalog PROPOSAL_AGGREGATE_CHECKS.md).
 */
class GlueAggregatesTest {

    private static final String YAML = """
            name: inventory
            entities:
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Store
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: StockMovement
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: Product, kind: manyToOne, to: Product }
                  - { name: Store, kind: manyToOne, to: Store }
              - name: ProductAvailability
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: onHand, type: decimal }
                relations:
                  - { name: Product, kind: manyToOne, to: Product }
                  - { name: Store, kind: manyToOne, to: Store }
            aggregates:
              - name: onHand
                of: StockMovement
                op: sum
                sum: quantity
                by: [Product, Store]
                into: ProductAvailability
                field: onHand
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsTheKeyedAggregateGlue() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> aggregates = GlueIntentGenerator.buildAggregatesForTest(model);
        assertEquals(1, aggregates.size());
        Map<String, Object> a = aggregates.get(0);

        assertEquals("onHand", a.get("name"));
        assertEquals("OnHand", a.get("className"));
        assertEquals("sum", a.get("op"));
        assertEquals("StockMovement", a.get("sourceEntity"));
        assertEquals("Quantity", a.get("sumField"));
        assertEquals("ProductAvailability", a.get("targetEntity"));
        assertEquals("Id", a.get("targetPk"));
        assertEquals("OnHand", a.get("targetField"));

        // both grouping keys resolved (relations of source AND target).
        List<Map<String, String>> keys = (List<Map<String, String>>) a.get("keys");
        assertEquals(2, keys.size());
        assertEquals(Map.of("key", "Product"), keys.get(0));
        assertEquals(Map.of("key", "Store"), keys.get(1));
    }
}
