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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A capacity roll-up with a status hands the emitter the column that remembers the status it
 * displaced (#7016), under the same name the EDM generator gives the parent's column - a roll-up
 * without a status hands it nothing, so the emitted compute block stays byte-identical.
 */
class GlueRollupStatusTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: Bill
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: total,   type: decimal, precision: 18, scale: 2 }
                  - { name: paid,    type: decimal, precision: 18, scale: 2 }
                  - { name: balance, type: decimal, precision: 18, scale: 2 }
                  - { name: lines,   type: integer }
                relations:
                  - { name: Status, kind: manyToOne, to: BillStatus }
              - name: BillStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 50 }
              - name: BillPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
            rollups:
              - { name: billPaid, entity: BillPayment, via: Bill, field: paid, op: sum, of: amount,
                  capacity: total, balance: balance, status: Status, statusWhenFull: 2, statusWhenPartial: 1 }
              - { name: billLines, entity: BillPayment, via: Bill, field: lines }
            """;

    @Test
    void aStatusRollupNamesTheDisplacedStatusColumnAndACountRollupDoesNot() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);

        List<Map<String, Object>> paid = rollups.stream()
                                                .filter(r -> "Paid".equals(r.get("countField")))
                                                .toList();
        List<Map<String, Object>> lines = rollups.stream()
                                                 .filter(r -> "Lines".equals(r.get("countField")))
                                                 .toList();
        assertEquals(4, paid.size());
        assertEquals(4, lines.size());
        assertTrue(paid.stream()
                       .allMatch(r -> "Status".equals(r.get("statusField")) && "DisplacedStatus".equals(r.get("statusDisplacedField"))),
                "every variant of the status roll-up - create, update, delete AND rekey - must relinquish through the same column: "
                        + paid);
        assertTrue(lines.stream()
                        .allMatch(r -> "".equals(r.get("statusField")) && "".equals(r.get("statusDisplacedField"))),
                "a roll-up without a status carries no displaced-status column: " + lines);
    }
}
