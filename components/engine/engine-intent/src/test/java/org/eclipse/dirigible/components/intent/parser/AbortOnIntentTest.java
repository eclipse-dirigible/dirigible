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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Validation of the process {@code abortOn: { status: [ids] | id, then: <step> }} construct. */
class AbortOnIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: OrderStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                abortOn: { status: [3, 4], then: markVoid }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: markVoid, kind: serviceTask, args: { setRelationField: Status, value: 3 } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [Status], actions: [confirm] }
            """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void aBareStatusScalarParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("status: [3, 4], then: markVoid", "status: 3")));
    }

    @Test
    void thenEndParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("then: markVoid", "then: end")));
    }

    @Test
    void abortOnWithoutStatusIsRejected() {
        assertIssue(YAML.replace("status: [3, 4], then: markVoid", "then: end"), "abortOn must declare `status`");
    }

    @Test
    void aNonIntegerStatusIsRejected() {
        assertIssue(YAML.replace("status: [3, 4]", "status: [VOID]"), "must be an integer EntityStatus seed id");
    }

    @Test
    void abortOnWithoutATriggerEntityIsRejected() {
        assertIssue(YAML.replace("    trigger: { onCreate: SalesOrder }\n", ""), "needs a process trigger entity");
    }

    @Test
    void abortOnWithoutAnEntityStatusRelationIsRejected() {
        String yaml = YAML.replace("      - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }\n", "");
        assertIssue(yaml, "declare a function: EntityStatus relation");
    }

    @Test
    void aThenReferencingAnUnknownStepIsRejected() {
        assertIssue(YAML.replace("then: markVoid", "then: nowhere"), "abortOn `then` references unknown step [nowhere]");
    }

    @Test
    void aThenOnAUserTaskIsRejected() {
        assertIssue(YAML.replace("then: markVoid", "then: confirm"), "must be a serviceTask cleanup");
    }

    @Test
    void aThenServiceTaskThatSetsNothingIsRejected() {
        String yaml = YAML.replace("{ name: markVoid, kind: serviceTask, args: { setRelationField: Status, value: 3 } }",
                "{ name: markVoid, kind: serviceTask, args: { call: custom/x.ts } }");
        assertIssue(yaml, "must set a field/relation");
    }

    @Test
    void aThenStepReachableFromTheMainFlowIsRejected() {
        // Route the main flow into markVoid (next from confirm) - it must be abort-only.
        String yaml = YAML.replace("{ name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }",
                "{ name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder, next: markVoid } }");
        assertIssue(yaml, "abort-only and must not be reachable from the main flow");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
