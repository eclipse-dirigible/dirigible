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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The {@code event.when} guard of the declarative glue lists - {@code notifications},
 * {@code integrations}, {@code outbound} - is held to the closed grammar every other typed guard is
 * held to, and refused when it does not parse (dirigible #7289).
 *
 * <p>
 * The renderer answered {@code true} for anything its pattern did not match, so a guard with a typo
 * switched itself off and the consumer fired on EVERY event - a guard nobody authored, silent all
 * the way through generation, compile and publish. That is the degradation {@code requiredWhen}
 * refused for its own condition (#7094), for the same reason.
 */
class EventGuardIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: OrderStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: channel, type: string }
                  - { name: total, type: decimal }
                  - { name: contactEmail, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            notifications:
              - name: issued-mail
                event: { onUpdate: Order, when: "Status == ISSUED" }
                to: contactEmail
                subject: "Order {number} is on its way"
                body: "Thank you for your order."
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            """;

    /** The construct the issue is about: the seeded name is what an author writes, and it resolves. */
    @Test
    void aSeededStatusNameIsAcceptedAndResolved() {
        IntentModel model = IntentParser.parse(YAML);

        assertEquals("Status == 2", String.valueOf(model.getNotifications()
                                                        .get(0)
                                                        .getEvent()
                                                        .get("when")));
    }

    /** A single {@code =} is the typo the old renderer answered {@code true} to. */
    @Test
    void anAssignmentInsteadOfAComparisonIsRefused() {
        assertIssue(YAML.replace("Status == ISSUED", "Status = ISSUED"), "must be `<Property> ==|!= <literal>`");
    }

    /** So is a conjunction spelled as prose - the list form is how an AND is said here. */
    @Test
    void aProseConjunctionIsRefused() {
        assertIssue(YAML.replace("Status == ISSUED", "Status == ISSUED and channel == 'web'"), "must be `<Property> ==|!= <literal>`");
    }

    /** The list form IS accepted - one comparison per element, ANDed (#6957). */
    @Test
    void theListFormIsAccepted() {
        IntentModel model =
                IntentParser.parse(YAML.replace("when: \"Status == ISSUED\"", "when: [\"Status == ISSUED\", \"channel == 'web'\"]"));

        assertEquals("[Status == 2, channel == 'web']", String.valueOf(model.getNotifications()
                                                                            .get(0)
                                                                            .getEvent()
                                                                            .get("when")));
    }

    /** A guard is read off the record, so it can only name the record's own properties. */
    @Test
    void aGuardOnAPropertyTheRecordDoesNotCarryIsRefused() {
        assertIssue(YAML.replace("Status == ISSUED", "sentMethod == 1"), "is not a field or to-one relation of [Order]");
    }

    /**
     * And only on the types an equality is exact on: {@code Objects.equals(BigDecimal, 0)} never holds,
     * so a guard on a decimal column would switch itself off while looking authored.
     */
    @Test
    void aGuardOnADecimalIsRefused() {
        assertIssue(YAML.replace("Status == ISSUED", "total == 0"), "which is a [decimal] field");
    }

    /** The same grammar on the other two lists of the axis - one validator, one answer. */
    @Test
    void theIntegrationAndOutboundGuardsAreHeldToTheSameGrammar() {
        String yaml = """
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: channel, type: string }
                integrations:
                  - name: pushOrder
                    event: { onUpdate: Order, when: "channel = web" }
                    method: POST
                    url: "https://api.example.com/orders"
                outbound:
                  - name: publishOrder
                    event: { onUpdate: Order, when: "channel == 'web' or channel == 'mail'" }
                    to: { queue: "codbex.orders" }
                """;
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.startsWith("integration [pushOrder] event when [channel = web] must be")),
                "the integration guard must be refused: " + thrown.getIssues());
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.startsWith("outbound [publishOrder] event when [channel == 'web' or")),
                "the outbound guard must be refused: " + thrown.getIssues());
    }

    /**
     * A step-bound consumer's guard resolves on the process's TRIGGER entity - the record the step
     * event is delivered about - because the binding itself names a step and no record at all.
     */
    @Test
    void aStepEventGuardResolvesOnTheProcessTriggerEntity() {
        String yaml = YAML
                          .replace("event: { onUpdate: Order, when: \"Status == ISSUED\" }",
                                  "event: { onStepReached: { process: OrderFlow, step: review }, when: \"Status == ISSUED\" }")
                          .replace("notifications:", """
                                  processes:
                                    - name: OrderFlow
                                      trigger: { onCreate: Order }
                                      steps:
                                        - { name: review, kind: userTask, args: { assignee: manager, form: ReviewOrder } }
                                  forms:
                                    - { name: ReviewOrder, forEntity: Order, fields: [number], actions: [approve] }
                                  notifications:""");

        assertEquals("Status == 2", String.valueOf(IntentParser.parse(yaml)
                                                               .getNotifications()
                                                               .get(0)
                                                               .getEvent()
                                                               .get("when")));
    }

    /** An empty list is a guard that says nothing while looking like one. */
    @Test
    void anEmptyListGuardIsRefused() {
        assertIssue(YAML.replace("when: \"Status == ISSUED\"", "when: []"), "event when must not be an empty list");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
