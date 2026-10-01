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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validation of a to-one association's {@code whenTargetDeleted: restrict} construct (dirigible
 * #7547) - what a DELETE of the TARGET does while this entity still references it. The same-model,
 * restrict-only v1: the mirror of {@link WhenMasterDeletedIntentTest}'s composition case, for a
 * plain association instead.
 */
class WhenTargetDeletedIntentTest {

    private static final String YAML = """
            name: expenses
            entities:
              - name: ExpenseCategory
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: Expense
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: category, kind: manyToOne, to: ExpenseCategory, required: true, whenTargetDeleted: restrict }
            """;

    @Test
    void restrictParses() {
        assertEquals("restrict", IntentParser.parse(YAML)
                                             .getEntities()
                                             .get(1)
                                             .getRelations()
                                             .get(0)
                                             .getWhenTargetDeleted());
    }

    /** Omitted is the default - no restriction - and needs no key at all. */
    @Test
    void omittedParsesAndDoesNotRestrict() {
        assertFalse(IntentParser.parse(YAML.replace(", whenTargetDeleted: restrict", ""))
                                .getEntities()
                                .get(1)
                                .getRelations()
                                .get(0)
                                .isTargetDeleteRestricted());
    }

    @Test
    void anUnknownValueIsRejected() {
        assertIssue(YAML.replace("whenTargetDeleted: restrict", "whenTargetDeleted: cascade"),
                "whenTargetDeleted [cascade] must be `restrict`");
    }

    /** Only a manyToOne/oneToOne association points at a target whose delete this could restrict. */
    @Test
    void onAOneToManyItIsRejected() {
        assertIssue(
                YAML.replace("kind: manyToOne, to: ExpenseCategory, required: true, whenTargetDeleted: restrict",
                        "kind: oneToMany, to: ExpenseCategory, whenTargetDeleted: restrict"),
                "only a manyToOne/oneToOne association points at a target");
    }

    /** A composition's master delete is whenMasterDeleted's question, not whenTargetDeleted's. */
    @Test
    void onACompositionItIsRejected() {
        assertIssue(YAML.replace("required: true, whenTargetDeleted: restrict", "composition: true, whenTargetDeleted: restrict"),
                "is a composition so its master's delete is whenMasterDeleted's question");
    }

    /** v1 is same-model only - the target's repository is generated in another model. */
    @Test
    void crossModelIsRejected() {
        String yaml = """
                name: expenses
                uses:
                  - { model: hr, project: hr-app }
                entities:
                  - name: Expense
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: employee, kind: manyToOne, model: hr, to: Employee, required: true, whenTargetDeleted: restrict }
                """;
        assertIssue(yaml, "is cross-model so whenTargetDeleted is not yet supported");
    }

    @Test
    void restrictOnAOneToOneIsAccepted() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("kind: manyToOne", "kind: oneToOne")));
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
