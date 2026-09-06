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
 * Validation of the process {@code whenDeleted: abort | refuse} construct (dirigible #7074) - what
 * a DELETE of the trigger entity's row does to the in-flight instance.
 */
class WhenDeletedIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                whenDeleted: refuse
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [id], actions: [confirm] }
            """;

    @Test
    void refuseParses() {
        assertEquals("refuse", IntentParser.parse(YAML)
                                           .getProcesses()
                                           .get(0)
                                           .getWhenDeleted());
    }

    @Test
    void abortParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("whenDeleted: refuse", "whenDeleted: abort")));
    }

    /** Omitted is the default - abort - and needs no key at all. */
    @Test
    void omittedParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("    whenDeleted: refuse\n", "")));
    }

    @Test
    void anUnknownValueIsRejected() {
        assertIssue(YAML.replace("whenDeleted: refuse", "whenDeleted: ignore"), "whenDeleted [ignore] must be `abort`");
    }

    @Test
    void whenDeletedWithoutATriggerEntityIsRejected() {
        assertIssue(YAML.replace("    trigger: { onCreate: SalesOrder }\n", ""), "whenDeleted needs a process trigger entity");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
