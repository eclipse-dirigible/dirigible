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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A roll-up whose PARENT is owned by another model: the child owns the event the handler binds to,
 * while the parent's package and perspective come from the owner's model. This was impossible
 * before - the parser rejected the roll-up because the parent entity is not in the local document,
 * so the canonical case (actual hours summed from a time-tracking model onto a project the projects
 * model owns) had no declarative form at all.
 *
 * With a null generation context the cross-model resolution falls back to the naming-convention
 * defaults, which is deterministic and enough to assert the emitted coordinates.
 */
class GlueRollupCrossModelTest {

    private static final String YAML = """
            name: timesheets
            uses:
              - { model: projects }
            entities:
              - name: DayAllocation
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, model: projects }
            rollups:
              - { name: projectActualHours, entity: DayAllocation, via: Project, field: actualHours,
                  op: sum, of: hours }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void aCrossModelParentResolvesToTheOwnerModelsPackage() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);

        // create + update + delete variants of the same roll-up.
        assertTrue(rollups.size() >= 1, "a cross-model roll-up must be emitted, got: " + rollups.size());
        Map<String, Object> first = rollups.get(0);
        assertEquals("DayAllocation", first.get("childEntity"));
        assertEquals("Project", first.get("parentEntity"));
        assertEquals("Project", first.get("fkProperty"));
        assertEquals("ActualHours", first.get("countField"));
        assertEquals("sum", first.get("op"));
        assertEquals("Hours", first.get("sumField"));
        // The parent is reached through `uses: projects`, so the handler must import from THAT model's
        // generated package - an unset parentModel would silently emit the local one and fail to compile.
        assertEquals(Boolean.TRUE, first.get("parentCrossModel"));
        assertEquals("projects", first.get("parentModel"));
    }

    @Test
    void aLocalParentCarriesNoCrossModelCoordinates() {
        String local = """
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
        Map<String, Object> first = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(local))
                                                       .get(0);
        assertEquals(Boolean.FALSE, first.get("parentCrossModel"));
        assertEquals("", first.get("parentModel"), "a local parent must leave the model empty so the local gen folder is used");
    }

    @Test
    void capacityBalanceAndStatusStayLocalOnly() {
        // These stamp a capacity guard that reads the parent's table and reference the parent's own
        // status seeds; supporting them across models is a deeper change than resolving coordinates, so
        // they are rejected with a message that says where to put them instead.
        String withCapacity = YAML.replace("op: sum, of: hours }", "op: sum, of: hours, capacity: budgetHours, balance: remainingHours }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(withCapacity));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("capacity / balance / status are not supported")),
                "expected a cross-model capacity issue, got: " + ex.getIssues());
    }

    @Test
    void aParentModelThatIsNotDeclaredInUsesIsRejected() {
        String undeclared = YAML.replace("uses:\n  - { model: projects }\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(undeclared));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("projects")),
                "an undeclared parent model must be reported, got: " + ex.getIssues());
    }
}
