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

/**
 * The decisions after a form task account for every action the form completes it with (issue
 * #7551). An action none of them tests lands in the last {@code else}, which is that action's
 * branch only while it is the one action left - two or more there make the {@code else} a catch-all
 * that silently takes an action the process never decided about. A decision testing an action the
 * form does not offer is a branch nothing can reach.
 */
class TaskActionDecisionIntentTest {

    private static final String YAML = """
            name: helpdesk
            entities:
              - name: Ticket
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string, length: 20 }
            processes:
              - name: TicketTriage
                trigger: { onCreate: Ticket }
                steps:
                  - { name: triage,    kind: userTask, args: { assignee: agent, form: TriageTicket } }
            %s
                  - { name: confirm,   kind: serviceTask, args: { setField: status, value: CONFIRMED, next: done } }
                  - { name: park,      kind: serviceTask, args: { setField: status, value: ON_HOLD, next: done } }
                  - { name: cancel,    kind: serviceTask, args: { setField: status, value: CANCELLED, next: done } }
                  - { name: done,      kind: end }
            forms:
              - { name: TriageTicket, forEntity: Ticket, fields: [status], actions: [%s, close] }
            permissions:
              - { role: Agent, description: Agent, can: [Ticket:read] }
            """;

    /** The issue's own Triage shape: the one action left to else is exactly the else's action. */
    @Test
    void anElseLeftWithOneActionIsThatActionsBranch() {
        assertDoesNotThrow(
                () -> IntentParser.parse(intent("confirm, reject", decision("accepted", "action == 'confirm'", "confirm", "cancel"))));
    }

    @Test
    void twoActionsFallingIntoOneElseAreACatchAll() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                intent("confirm, reject, hold", decision("accepted", "action == 'confirm'", "confirm", "cancel"))));

        assertTrue(ex.getMessage()
                     .contains("whose actions [reject, hold] are tested by none of the decisions after it"),
                ex.getMessage());
    }

    /** A chain of decisions through each else covers the form one action at a time. */
    @Test
    void aChainOfDecisionsThroughElseCoversEveryAction() {
        String chain = decision("accepted", "action == 'confirm'", "confirm", "parked") + "\n"
                + decision("parked", "action == 'hold'", "park", "cancel");

        assertDoesNotThrow(() -> IntentParser.parse(intent("confirm, reject, hold", chain)));
    }

    @Test
    void oneDecisionMayTestSeveralActionsAtOnce() {
        assertDoesNotThrow(() -> IntentParser.parse(
                intent("confirm, reject, hold", decision("accepted", "action == 'confirm' || action == 'hold'", "confirm", "cancel"))));
    }

    @Test
    void aDecisionTestingAnActionTheFormDoesNotOfferIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(intent("confirm, reject", decision("accepted", "action == 'approve'", "confirm", "cancel"))));

        assertTrue(ex.getMessage()
                     .contains("decision [accepted] tests the action [approve] the form does not offer"),
                ex.getMessage());
    }

    /** A decision that does not branch on the chosen action is not this check's to judge. */
    @Test
    void aDecisionOnSomethingElseIsLeftAlone() {
        assertDoesNotThrow(
                () -> IntentParser.parse(intent("confirm, reject, hold", decision("accepted", "status == 'NEW'", "confirm", "cancel"))));
    }

    private static String intent(String actions, String decisions) {
        return YAML.formatted(decisions, actions);
    }

    private static String decision(String name, String condition, String then, String otherwise) {
        return "      - { name: " + name + ", kind: decision, args: { if: \"" + condition + "\", then: " + then + ", else: " + otherwise
                + " } }";
    }
}
