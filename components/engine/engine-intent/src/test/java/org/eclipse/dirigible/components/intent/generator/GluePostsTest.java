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
 * Verifies the {@code posts} glue the {@link GlueIntentGenerator} emits from a {@code posts:} rule:
 * the source entity + event + per-item collection + target + idempotency back-reference, and the
 * field map (including a sign-flip Calc expression). Structural glue - the handler template + BPMN
 * wiring is a later stage (kf-catalog PROPOSAL_EVENT_POSTING.md).
 */
class GluePostsTest {

    private static final String YAML = """
            name: inventory
            entities:
              - name: StockMovementStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: StockMovement
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                  - { name: direction, type: integer }
                relations:
                  - { name: Product, kind: manyToOne, to: Product }
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: GoodsIssue
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Store, kind: manyToOne, to: Product }
                  - { name: Status, kind: manyToOne, to: StockMovementStatus, function: EntityStatus, init: 1 }
              - name: GoodsIssueItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue, composition: true }
                  - { name: Product, kind: manyToOne, to: Product }
            posts:
              - name: goodsIssueLedger
                forEntity: GoodsIssue
                event: 2
                forEach: items
                into: StockMovement
                idempotentBy: GoodsIssue
                guard: negativeStock
                set:
                  Product: item.Product
                  Quantity: "-item.Quantity"
                  Direction: 2
                  Store: source.Store
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsTheResolvedPostGlueDescriptor() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> posts = GlueIntentGenerator.buildPostsForTest(model);
        assertEquals(1, posts.size());
        Map<String, Object> p = posts.get(0);

        assertEquals("goodsIssueLedger", p.get("name"));
        assertEquals("GoodsIssueLedger", p.get("className"));
        assertEquals("GoodsIssue", p.get("entity"));
        // status-triggered: not a create, guarded to status seed id 2 on the EntityStatus property.
        assertEquals(Boolean.FALSE, p.get("isCreate"));
        assertEquals("Status", p.get("statusProperty"));
        assertEquals("2", p.get("statusValue"));
        // per-item: the composition child of GoodsIssue is GoodsIssueItem (FK GoodsIssue).
        assertEquals(Boolean.TRUE, p.get("perItem"));
        assertEquals("GoodsIssueItem", p.get("itemsEntity"));
        assertEquals("GoodsIssue", p.get("itemsFk"));
        // target + idempotency back-reference.
        assertEquals("StockMovement", p.get("into"));
        assertEquals("Id", p.get("targetPk"));
        assertEquals("GoodsIssue", p.get("backRef"));

        List<Map<String, String>> assigns = (List<Map<String, String>>) p.get("assigns");
        assertEquals(4, assigns.size());
        // item copy, null-safe negation, integer constant, source copy - rendered to Java expressions.
        assertEquals(Map.of("field", "Product", "expr", "item.Product"), assigns.get(0));
        assertEquals(Map.of("field", "Quantity", "expr", "item.Quantity == null ? null : item.Quantity.negate()"), assigns.get(1));
        assertEquals(Map.of("field", "Direction", "expr", "2"), assigns.get(2));
        assertEquals(Map.of("field", "Store", "expr", "source.Store"), assigns.get(3));
    }
}
