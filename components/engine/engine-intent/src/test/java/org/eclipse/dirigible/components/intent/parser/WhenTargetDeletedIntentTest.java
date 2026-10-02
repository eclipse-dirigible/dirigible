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
 * Validation of a to-one association's {@code whenTargetDeleted} construct (dirigible #7547) - what
 * a DELETE of the TARGET does to the records still referencing it: {@code restrict} (the default,
 * also when the key is absent), {@code nullify} or {@code cascade}, same-model and cross-model. The
 * mirror of {@link WhenMasterDeletedIntentTest}'s composition case, for a plain association
 * instead.
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
        assertEquals("restrict", ruleOf(YAML));
    }

    /** Omitted is restrict: a reference left pointing at a deleted record is never a valid state. */
    @Test
    void omittedIsRestrict() {
        assertEquals("restrict", ruleOf(YAML.replace(", whenTargetDeleted: restrict", "")));
    }

    @Test
    void cascadeParses() {
        assertEquals("cascade", ruleOf(YAML.replace("whenTargetDeleted: restrict", "whenTargetDeleted: cascade")));
    }

    /** Nullify clears the foreign key, so it needs a relation that may be empty. */
    @Test
    void nullifyParsesOnAnOptionalRelation() {
        assertEquals("nullify", ruleOf(YAML.replace("required: true, whenTargetDeleted: restrict", "whenTargetDeleted: nullify")));
    }

    @Test
    void nullifyIsRejectedOnARequiredRelation() {
        assertIssue(YAML.replace("whenTargetDeleted: restrict", "whenTargetDeleted: nullify"),
                "is required, so whenTargetDeleted: nullify cannot clear it");
    }

    @Test
    void anUnknownValueIsRejected() {
        assertIssue(YAML.replace("whenTargetDeleted: restrict", "whenTargetDeleted: ignore"),
                "whenTargetDeleted [ignore] must be `restrict`");
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

    /**
     * A cross-model target takes every rule too (the issue's employee referenced by another module's
     * expense claims): the target's repository cannot see this model, so this entity's repository
     * contributes the rule its delete applies.
     */
    @Test
    void crossModelIsAccepted() {
        String yaml = """
                name: expenses
                uses:
                  - { model: hr, project: hr-app }
                entities:
                  - name: Expense
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: employee, kind: manyToOne, model: hr, to: Employee, required: true, whenTargetDeleted: cascade }
                """;
        assertEquals("cascade", ruleOf(yaml));
    }

    @Test
    void restrictOnAOneToOneIsAccepted() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("kind: manyToOne", "kind: oneToOne")));
    }

    /** The rule of the last entity's first relation. */
    private static String ruleOf(String yaml) {
        var entities = IntentParser.parse(yaml)
                                   .getEntities();
        return entities.get(entities.size() - 1)
                       .getRelations()
                       .get(0)
                       .getTargetDeleteRule();
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
