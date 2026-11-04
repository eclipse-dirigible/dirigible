/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A roll-up's row filter and its capacity-guard gate (dirigible #7542) - WHICH of the child's rows
 * count at all, and WHEN the ceiling is enforced.
 *
 * Without the filter a cancelled or voided document keeps consuming the parent's capacity for ever
 * and its replacement can never be issued; without the gate a guard over a DOCUMENT TOTAL only ever
 * sees the 0 the header was created with, because the total is recomputed from the lines after the
 * header is written.
 */
class RollupMembershipIntentTest {

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
                guardAt: ISSUED
                message: "Only {remaining} of {capacity} left."
            """;

    private static String refusalOf(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getMessage();
    }

    @Test
    void aFilterAndAGateParse() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
        // The status names are resolved to their seed ids before the typed mapping, like every other
        // status site - the generators and templates only ever see integers.
        assertEquals("2", IntentParser.parse(YAML)
                                      .getRollups()
                                      .get(0)
                                      .getGuardAt());
        assertEquals(3L, ((Number) IntentParser.parse(YAML)
                                               .getRollups()
                                               .get(0)
                                               .getWhere()
                                               .get(0)
                                               .getValue()).longValue());
    }

    /**
     * The filter is applied twice - as a query over the stored rows and, for the row being written, as
     * a Java comparison - so only an exact equality can be rendered the same way on both sides.
     */
    @Test
    void anInexactOperatorIsRefused() {
        assertTrue(refusalOf(YAML.replace("op: ne", "op: like")).contains("supported: eq/ne"));
    }

    @Test
    void aFilterFieldTheChildDoesNotDeclareIsRefused() {
        assertTrue(refusalOf(YAML.replace("field: Status, op: ne", "field: Nonsense, op: ne")).contains(
                "is not a field or to-one relation of [CallOff]"));
    }

    /**
     * A gate names the status the CAPACITY guard waits for, so without one there is nothing to gate.
     */
    @Test
    void aGateWithoutACapacityIsRefused() {
        assertTrue(refusalOf(YAML.replace("    capacity: ceiling\n", "")).contains("with no capacity"));
    }

    @Test
    void aGateOnAChildWithNoStatusRelationIsRefused() {
        String yaml =
                YAML.replace("      - { name: Status, kind: manyToOne, to: CallOffStatus, function: EntityStatus, init: DRAFT }\n", "");
        String refusal = refusalOf(yaml);
        assertTrue(refusal.contains("function: EntityStatus"), refusal);
    }

    @Test
    void aGateNamingNoSeededStatusIsRefused() {
        assertTrue(refusalOf(YAML.replace("guardAt: ISSUED", "guardAt: SHIPPED")).contains("SHIPPED"));
    }

    /**
     * A foreign child's rows are written by the owner's repository, which is also where its capacity
     * guard would have to be emitted - so a filter or a gate declared here would narrow and gate
     * nothing, the same reason {@code capacity:} itself is refused on that direction.
     */
    @Test
    void aFilterOnACrossModelChildIsRefused() {
        String yaml = """
                name: contracts
                uses:
                  - { model: call-offs }
                entities:
                  - name: Contract
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: committed, type: decimal }
                rollups:
                  - name: contractCommitted
                    entity: CallOff
                    model: call-offs
                    parent: Contract
                    via: Contract
                    field: committed
                    op: sum
                    of: total
                    where:
                      - { field: Status, op: ne, value: 3 }
                """;
        assertTrue(refusalOf(yaml).contains("where / guardAt are not supported"));
    }

    /**
     * {@code enforce: false} (#7904) keeps the balance without the guard, so it needs a capacity, and
     * leaves no guard for {@code guardAt:} / {@code message:}.
     */
    @Test
    void anUnenforcedCapacityParsesAndItsContradictionsAreRefused() {
        String unenforced = YAML.replace("    guardAt: ISSUED\n", "")
                                .replace("    message: \"Only {remaining} of {capacity} left.\"\n", "    enforce: false\n");
        assertEquals(false, IntentParser.parse(unenforced)
                                        .getRollups()
                                        .get(0)
                                        .isEnforced());
        assertTrue(refusalOf(unenforced.replace("    capacity: ceiling\n", "")).contains("enforce switches the CAPACITY guard"));
        assertTrue(refusalOf(YAML.replace("    guardAt: ISSUED\n", "    guardAt: ISSUED\n    enforce: false\n")).contains(
                "which enforce: false does not emit"));
    }

    /**
     * A foreign parent's repository is its owner's, so nothing here can follow an edit of its capacity.
     */
    @Test
    void anUnenforcedCapacityOnACrossModelParentIsRefused() {
        String yaml = """
                name: timesheets
                uses:
                  - { model: projects }
                entities:
                  - name: Timesheet
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: hours, type: decimal }
                    relations:
                      - { name: Project, kind: manyToOne, to: Project, model: projects }
                rollups:
                  - { name: projectHours, entity: Timesheet, via: Project, field: actualHours, op: sum, of: hours,
                      capacity: budgetHours, balance: remainingHours, enforce: false }
                """;
        assertTrue(refusalOf(yaml).contains("enforce: false on a cross-model parent"));
    }
}
