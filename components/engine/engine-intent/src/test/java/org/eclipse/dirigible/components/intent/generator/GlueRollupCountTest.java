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
 * A {@code count} roll-up - the default {@code op} - must recompute on the child's UPDATE as well
 * as its create and delete. A child moves between parents by an ordinary edit of its parent
 * relation, and without the update handler neither count moved: the new parent never counted the
 * row it received.
 */
class GlueRollupCountTest {

    private static final String YAML = """
            name: library
            entities:
              - name: Member
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: loanCount, type: integer }
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Member, kind: manyToOne, to: Member }
            rollups:
              - { name: memberLoanCount, entity: Loan, via: Member, field: loanCount }
            """;

    @Test
    void aCountRollupRecomputesOnCreateUpdateAndDelete() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);

        assertEquals(4, rollups.size(),
                "a count roll-up must recompute on create, update, delete and rekey (the rekey variant repairs the vacated parent, #6819)");
        assertEquals(List.of("", "-updated", "-deleted", "-rekeyed"), rollups.stream()
                                                                             .map(r -> String.valueOf(r.get("topicSuffix")))
                                                                             .toList(),
                "the handlers bind the child's base, -updated, -deleted and -rekeyed topics");
        assertEquals(List.of("LoanMemberRollupOnCreate", "LoanMemberRollupOnUpdate", "LoanMemberRollupOnDelete", "LoanMemberRollupOnRekey"),
                rollups.stream()
                       .map(r -> String.valueOf(r.get("className")))
                       .toList());
        // Every variant carries the same op and recompute criteria - the update handler is not a
        // special case, it is the same idempotent read-modify-write of the affected parent.
        assertTrue(rollups.stream()
                          .allMatch(r -> "count".equals(r.get("op"))
                                  && "Criteria.create().eq(\"Member\", entity.Member)".equals(r.get("criteriaExpression"))),
                "all handlers must recompute the same way: " + rollups);
    }
}
