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
              - name: StockMovement
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
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
                event: POSTED
                forEach: items
                into: StockMovement
                idempotentBy: GoodsIssue
                guard: negativeStock
                set:
                  Product: item.Product
                  Quantity: "-item.Quantity"
                  GoodsIssue: Issue.Id
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsThePostGlueDescriptor() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> posts = GlueIntentGenerator.buildPostsForTest(model);
        assertEquals(1, posts.size());
        Map<String, Object> p = posts.get(0);

        assertEquals("goodsIssueLedger", p.get("name"));
        assertEquals("GoodsIssueLedger", p.get("className"));
        assertEquals("GoodsIssue", p.get("entity"));
        assertEquals("POSTED", p.get("event"));
        assertEquals("items", p.get("forEach"));
        assertEquals("StockMovement", p.get("into"));
        assertEquals("GoodsIssue", p.get("idempotentBy"));
        assertEquals("negativeStock", p.get("guard"));

        List<Map<String, String>> set = (List<Map<String, String>>) p.get("set");
        assertEquals(3, set.size());
        // field names are pascal-cased; values (incl. the sign-flip expression) pass through verbatim.
        assertEquals(Map.of("field", "Product", "value", "item.Product"), set.get(0));
        assertEquals(Map.of("field", "Quantity", "value", "-item.Quantity"), set.get(1));
        assertEquals(Map.of("field", "GoodsIssue", "value", "Issue.Id"), set.get(2));
    }
}
