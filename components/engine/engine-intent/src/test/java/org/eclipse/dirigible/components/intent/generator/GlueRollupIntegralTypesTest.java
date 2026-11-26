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
 * A sum roll-up over integral columns (#7944) tells the emitter which of them to convert into, and
 * narrow out of, the BigDecimal it computes in. A decimal roll-up carries none of the keys, so its
 * emitted handler stays byte-identical.
 */
class GlueRollupIntegralTypesTest {

    private static final String YAML = """
            name: venues
            entities:
              - name: Session
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: seats,     type: integer }
                  - { name: booked,    type: integer }
                  - { name: free,      type: long }
                  - { name: income,    type: decimal, precision: 18, scale: 2 }
              - name: Booking
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: places, type: integer }
                  - { name: price,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Session, kind: manyToOne, to: Session, required: true }
            rollups:
              - { name: sessionBooked, entity: Booking, via: Session, field: booked, op: sum, of: places,
                  capacity: seats, balance: free }
              - { name: sessionIncome, entity: Booking, via: Session, field: income, op: sum, of: price }
            """;

    @Test
    void integralColumnsAreNamedAndDecimalOnesAreNot() {
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(YAML));

        List<Map<String, Object>> booked = rollups.stream()
                                                  .filter(r -> "Booked".equals(r.get("countField")))
                                                  .toList();
        assertFalse(booked.isEmpty());
        for (Map<String, Object> rollup : booked) {
            assertEquals("Integer", rollup.get("sumFieldType"), "the summed child field: " + rollup);
            assertEquals("Integer", rollup.get("countFieldType"), "the parent total: " + rollup);
            assertEquals("Integer", rollup.get("capacityFieldType"), "the capacity: " + rollup);
            assertEquals("Long", rollup.get("balanceFieldType"), "the balance: " + rollup);
        }
        assertTrue(rollups.stream()
                          .filter(r -> "Income".equals(r.get("countField")))
                          .allMatch(r -> !r.containsKey("sumFieldType") && !r.containsKey("countFieldType")),
                "a decimal roll-up carries no type key: " + rollups);
    }
}
