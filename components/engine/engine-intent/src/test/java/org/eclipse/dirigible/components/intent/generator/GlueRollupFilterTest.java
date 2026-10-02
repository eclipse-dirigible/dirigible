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

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A roll-up's {@code where:} filter (dirigible #7542) reaches the ASYNCHRONOUS recompute as the
 * clauses appended to its own foreign-key query.
 *
 * The same clauses are stamped on the capacity guard by the EDM generator, which is what keeps the
 * stored balance and the enforced ceiling from disagreeing: one authored definition, two readers.
 */
class GlueRollupFilterTest {

    private static final String YAML = """
            name: contracts
            entities:
              - name: Contract
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: ceiling,   type: decimal }
                  - { name: committed, type: decimal }
              - name: CallOffStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CallOff
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: Contract, kind: manyToOne, to: Contract }
                  - { name: Status, kind: manyToOne, to: CallOffStatus, function: EntityStatus, init: DRAFT }
            seeds:
              - name: callOffStatuses
                entity: CallOffStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
                  - { id: 3, name: CANCELLED, stage: cancelled }
            rollups:
              - name: contractCommitted
                entity: CallOff
                via: Contract
                field: committed
                op: sum
                of: total
                capacity: ceiling
                where:
                  - { field: Status, op: ne, value: CANCELLED }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void everyHandlerOfAFilteredRollupCarriesTheClauses() {
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(YAML));
        assertEquals(4, rollups.size());
        for (Map<String, Object> rollup : rollups) {
            List<Map<String, Object>> filter = (List<Map<String, Object>>) rollup.get("filter");
            assertEquals(1, filter.size(), "every variant recomputes over the same row set: " + rollup);
            assertEquals("ne", filter.get(0)
                                     .get("op"));
            assertEquals("Status", filter.get(0)
                                         .get("property"));
            assertEquals("3", ((Map<?, ?>) filter.get(0)
                                                 .get("value")).get("text"),
                    "the CANCELLED name resolves to its seed id like every other status site");
        }
    }

    /** A roll-up declaring no filter counts every child, exactly as it always did. */
    @Test
    void anUnfilteredRollupCarriesNoClauses() {
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(
                IntentParser.parse(YAML.replace("    where:\n      - { field: Status, op: ne, value: CANCELLED }\n", "")));
        assertFalse(rollups.isEmpty());
        assertTrue(rollups.stream()
                          .noneMatch(r -> r.containsKey("filter")),
                "an unfiltered roll-up must render byte-identically: " + rollups);
    }
}
