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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code outbound} glue collection: one publisher descriptor per departure, carrying the source
 * topic it subscribes to, the channel it re-publishes on, and - when declared - the guard and the
 * envelope. The step-bound case additionally proves the departure participates in the event axis
 * like any other consumer: it contributes the step emitter that publishes the record at that
 * boundary, which is the only reason the publisher's own subscription ever receives anything.
 */
class GlueOutboundTest {

    private static final String YAML = """
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
            processes:
              - name: OrderApproval
                trigger: { onCreate: Order }
                steps:
                  - { name: activate, kind: serviceTask, args: { setField: channel, value: DIRECT } }
            outbound:
              - name: publishOrder
                event: { onCreate: Order }
                to: { queue: "codbex.orders" }
              - name: announce-order-update
                event: { onUpdate: Order, when: "channel != internal" }
                to: { topic: "codbex.order-updates" }
                payload:
                  type: "order.updated"
                  version: 1
                  messageId: "{uuid}"
                  tenantId: "{tenant}"
                  reference: id
                  customer: customer.name
              - name: announceActivation
                event: { onStepCompleted: { process: OrderApproval, step: activate } }
                to: { topic: "codbex.order-activations" }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void aBareDepartureRepublishesTheRecordOnItsChannel() {
        Map<String, Object> departure = departure("publishOrder");

        assertEquals("PublishOrder", departure.get("className"));
        assertEquals("Order", departure.get("entity"));
        assertEquals("", departure.get("topicSuffix"), "an onCreate departure subscribes to the base entity topic");
        assertEquals("codbex.orders", departure.get("destination"));
        assertEquals("QUEUE", departure.get("channel"));
        assertEquals("sendToQueue", departure.get("producerMethod"));
        assertEquals(false, departure.get("hasGuard"));
        assertEquals("true", departure.get("guardExpression"));
        assertEquals(false, departure.get("hasPayload"));
        assertTrue(((List<Map<String, Object>>) departure.get("payloadFields")).isEmpty());
        assertTrue(((List<Map<String, Object>>) departure.get("relationLoads")).isEmpty(),
                "nothing to resolve - the message received is the message sent");
    }

    @SuppressWarnings("unchecked")
    @Test
    void aGuardedDepartureCarriesItsEnvelopeAndItsRelationLoads() {
        Map<String, Object> departure = departure("announce-order-update");

        assertEquals("AnnounceOrderUpdate", departure.get("className"), "a hyphenated name still yields a legal Java class");
        assertEquals("-updated", departure.get("topicSuffix"));
        assertEquals("TOPIC", departure.get("channel"));
        assertEquals("sendToTopic", departure.get("producerMethod"));
        assertEquals(true, departure.get("hasGuard"));
        assertEquals("!java.util.Objects.equals(entity.Channel, \"internal\")", departure.get("guardExpression"));

        assertEquals(true, departure.get("hasPayload"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) departure.get("payloadFields");
        assertEquals(List.of("type", "version", "messageId", "tenantId", "reference", "customer"), fields.stream()
                                                                                                         .map(field -> field.get("key"))
                                                                                                         .toList(),
                "the envelope keeps its authored order - a contract is read in the order it was written");
        assertEquals("\"order.updated\"", fields.get(0)
                                                .get("expression"));
        assertEquals("java.util.UUID.randomUUID().toString()", fields.get(2)
                                                                     .get("expression"));
        assertEquals("entity.Id", fields.get(4)
                                        .get("expression"));
        assertEquals("(customer == null ? null : customer.Name)", fields.get(5)
                                                                        .get("expression"));

        List<Map<String, Object>> loads = (List<Map<String, Object>>) departure.get("relationLoads");
        assertEquals(1, loads.size(), "loads: " + loads);
        assertEquals("customer", loads.get(0)
                                      .get("local"));
        assertEquals("Customer", loads.get(0)
                                      .get("targetEntity"));
    }

    @Test
    void aStepBoundDepartureSubscribesToTheStepTopicAndBringsItsEmitter() {
        Map<String, Object> departure = departure("announceActivation");

        assertEquals("Order", departure.get("entity"), "a step event is about the record the process runs on");
        assertEquals("-step-OrderApproval-activate-completed", departure.get("topicSuffix"));
        assertEquals("codbex.order-activations", departure.get("destination"));

        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> emitters = GlueIntentGenerator.buildStepEventsForTest(model);
        assertEquals(1, emitters.size(), "the departure alone must pull in the emitter that publishes at that boundary");
        assertEquals("-step-OrderApproval-activate-completed", emitters.get(0)
                                                                       .get("topicSuffix"));
    }

    @Test
    void anExternalContractDestinationReachesThePublisherVerbatim() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                outbound:
                  - { name: publishOrder, event: { onCreate: Order }, to: { topic: "global:codbex.orders" } }
                """);

        Map<String, Object> departure = GlueIntentGenerator.buildOutboundForTest(model)
                                                           .get(0);
        assertEquals("global:codbex.orders", departure.get("destination"),
                "the platform resolves a destination name - this layer must never rewrite one, marker included");
    }

    @Test
    void aDepartureNamingNoSingleChannelContributesNothing() {
        // The parser rejects this shape; the generator is asked directly to prove it never emits a
        // publisher that picked a channel of its own.
        IntentModel model = new IntentModel();
        model.setEntities(IntentParser.parse(YAML)
                                      .getEntities());
        var departure = new org.eclipse.dirigible.components.intent.model.OutboundIntent();
        departure.setName("publishOrder");
        departure.getEvent()
                 .put("onCreate", "Order");
        model.getOutbound()
             .add(departure);

        assertTrue(GlueIntentGenerator.buildOutboundForTest(model)
                                      .isEmpty());
    }

    @Test
    void anIntegrationHonoursTheEventGuardTheAxisDeclares() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: channel, type: string }
                integrations:
                  - name: pushOrder
                    event: { onCreate: Order, when: "channel == web" }
                    method: POST
                    url: "https://api.example.com/orders"
                """);

        Map<String, Object> integration = GlueIntentGenerator.buildIntegrationsForTest(model)
                                                             .get(0);
        assertEquals(true, integration.get("hasGuard"));
        assertEquals("java.util.Objects.equals(entity.Channel, \"web\")", integration.get("guardExpression"));
    }

    @Test
    void anIntegrationWithoutAGuardForwardsEveryEvent() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                integrations:
                  - { name: pushOrder, event: { onCreate: Order }, method: POST, url: "https://api.example.com/orders" }
                """);

        Map<String, Object> integration = GlueIntentGenerator.buildIntegrationsForTest(model)
                                                             .get(0);
        assertFalse((Boolean) integration.get("hasGuard"));
    }

    private static Map<String, Object> departure(String name) {
        IntentModel model = IntentParser.parse(YAML);
        for (Map<String, Object> entry : GlueIntentGenerator.buildOutboundForTest(model)) {
            if (name.equals(entry.get("name"))) {
                return entry;
            }
        }
        throw new AssertionError("no outbound [" + name + "] in the glue");
    }
}
