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
 * Verifies that every roll-up also binds the child's {@code "-rekeyed"} event (#6819).
 *
 * <p>
 * A re-parented child is named by no create / update / delete event of the parent it moved AWAY
 * from - those all carry the parent it belongs to NOW - so that parent's total kept the moved
 * child's contribution forever. The rekey handler is the same recompute keyed on the payload's FK,
 * fed the row whose grouping moved.
 */
class GlueRollupRekeyTest {

    private static final String YAML = """
            name: library
            entities:
              - name: Member
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: loanCount, type: integer }
                relations:
                  - { name: loans, kind: oneToMany, to: Loan }
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Member, kind: manyToOne, to: Member }
            rollups:
              - { name: memberLoanCount, entity: Loan, via: Member, field: loanCount, op: count }
            """;

    @Test
    void everyRollupBindsTheRekeyEvent() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);

        Map<String, Object> rekey = rollups.stream()
                                           .filter(r -> "-rekeyed".equals(r.get("topicSuffix")))
                                           .findFirst()
                                           .orElseThrow(() -> new AssertionError("no rekey handler in " + rollups));
        assertEquals("LoanMemberRollupOnRekey", rekey.get("className"));
        assertEquals("Loan", rekey.get("childEntity"));
        assertEquals("Member", rekey.get("parentEntity"));
        // The recompute is keyed on the FK carried by the payload, so the SAME handler repairs the parent
        // the child left (fed the previous row) and the one it moved into (fed the written row).
        assertEquals("Criteria.create().eq(\"Member\", entity.Member)", rekey.get("criteriaExpression"));
        assertTrue(rollups.stream()
                          .map(r -> String.valueOf(r.get("topicSuffix")))
                          .toList()
                          .containsAll(List.of("", "-deleted", "-rekeyed")),
                "the rekey handler must be additional to the create/delete ones, not replace them: " + rollups);
    }
}
