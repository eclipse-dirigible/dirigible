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
import org.eclipse.dirigible.components.intent.model.OutboundIntent;
import org.junit.jupiter.api.Test;

/**
 * The parse-time half of {@code outbound:} - a record leaving on a queue or a topic. The one rule
 * this construct adds is the arrival rule read backwards: exactly one channel. Everything else it
 * shares with the constructs it mirrors, and the tests here pin that it really is shared (the event
 * axis rejects a bad binding, the payload rejects a bad value) rather than re-implemented.
 */
class OutboundIntentTest {

    private static final String ENTITIES = """
            name: sales
            entities:
              - name: Customer
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: channel, type: string }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            """;

    @Test
    void aDepartureParsesWithItsChannelGuardAndPayload() {
        IntentModel model = IntentParser.parse(ENTITIES + """
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: { queue: "codbex.orders" }
                  - name: announceOrder
                    event: { onUpdate: Order, when: "channel != internal" }
                    to: { topic: "codbex.order-updates" }
                    payload:
                      type: "order.updated"
                      version: 1
                      messageId: "{uuid}"
                      customer: customer.name
                """);

        assertEquals(2, model.getOutbound()
                             .size());
        OutboundIntent published = model.getOutbound()
                                        .get(0);
        assertEquals("publishOrder", published.getName());
        assertEquals("codbex.orders", published.getTo()
                                               .getQueue());
        assertTrue(published.getPayload()
                            .isEmpty(),
                "no payload declared - the body is the record itself");

        OutboundIntent announced = model.getOutbound()
                                        .get(1);
        assertEquals("codbex.order-updates", announced.getTo()
                                                      .getTopic());
        assertEquals("channel != internal", announced.getEvent()
                                                     .get("when"));
        assertEquals(4, announced.getPayload()
                                 .size());
        assertEquals("customer.name", announced.getPayload()
                                               .get("customer"));
    }

    @Test
    void aDepartureWithNoChannelFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: {}
                """, "exactly one of queue/topic");
    }

    @Test
    void aDepartureWithNoTargetAtAllFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                """, "exactly one of queue/topic");
    }

    @Test
    void aDepartureNamingBothChannelsFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: { queue: "codbex.orders", topic: "codbex.orders" }
                """, "exactly one of queue/topic");
    }

    @Test
    void twoDeparturesOfTheSameNameFailTheParse() {
        assertFails("""
                outbound:
                  - { name: publishOrder, event: { onCreate: Order }, to: { queue: a } }
                  - { name: publishOrder, event: { onUpdate: Order }, to: { queue: b } }
                """, "duplicate outbound [publishOrder]");
    }

    @Test
    void aDepartureMustBindToExactlyOneEvent() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order, onUpdate: Order }
                    to: { queue: "codbex.orders" }
                """, "exactly one of onCreate/onUpdate/onDelete/onTransition/onStepReached/onStepCompleted");
    }

    @Test
    void aDepartureOfAnUnknownEntityFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Invoice }
                    to: { queue: "codbex.orders" }
                """, "references unknown entity [Invoice]");
    }

    @Test
    void anUnknownPayloadTokenFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: { queue: "codbex.orders" }
                    payload: { stamp: "{today}" }
                """, "neither a context token");
    }

    @Test
    void anUnknownKeyOnADepartureFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: { queue: "codbex.orders" }
                    method: POST
                """, "unknown key [method]");
    }

    @Test
    void anUnknownChannelKeyFailsTheParse() {
        assertFails("""
                outbound:
                  - name: publishOrder
                    event: { onCreate: Order }
                    to: { destination: "codbex.orders" }
                """, "unknown key [destination]");
    }

    private static void assertFails(String outbound, String expected) {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ENTITIES + outbound));
        assertTrue(failure.getMessage()
                          .contains(expected),
                failure.getMessage());
    }
}
