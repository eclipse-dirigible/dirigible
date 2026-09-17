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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code event.when} guard of the three declarative glue lists, rendered against the guarded
 * property's declared type (dirigible #7289).
 *
 * <p>
 * A status guard is the natural authoring of the construct - "mail the customer when the invoice
 * reaches ISSUED" - and the seeded name is resolved to its seed id before the typed mapping. The
 * untyped renderer quoted whatever it did not recognise, so that id was compared as a string
 * against the integer status FK: the mail never went out, the departure never left, and parse,
 * generation, compile and publish were all green.
 */
class GlueEventGuardTest {

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
                  - { name: contactEmail, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            notifications:
              - name: issued-mail
                event: { onUpdate: Order, when: "Status == ISSUED" }
                to: contactEmail
                subject: "Order {number} is on its way"
                body: "Thank you for your order."
              - name: web-issued-mail
                event: { onUpdate: Order, when: ["Status == ISSUED", "channel == 'web'"] }
                to: contactEmail
                subject: "Order {number} is on its way"
                body: "Thank you for your order."
            integrations:
              - name: pushOrder
                event: { onUpdate: Order, when: "Status == ISSUED" }
                method: POST
                url: "https://api.example.com/orders"
            outbound:
              - name: publishOrder
                event: { onUpdate: Order, when: "Status == ISSUED" }
                to: { queue: "codbex.orders" }
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            """;

    /**
     * The seed id compared numerically: a to-one's key is a whole number of a width the generator
     * cannot know (a cross-model target types its key in the owner's {@code .model}), and
     * {@code Objects.equals(Long, Integer)} never holds - the same rule {@code requiredWhen} renders
     * its own to-one guard by (#7237).
     */
    @Test
    void aStatusGuardComparesTheForeignKeyNumerically() {
        IntentModel model = IntentParser.parse(YAML);
        String expected = "(entity.Status != null && entity.Status.longValue() == 2L)";

        assertEquals(expected, guard(GlueIntentGenerator.buildNotificationsForTest(model), "issued-mail"), "notification");
        assertEquals(expected, guard(GlueIntentGenerator.buildIntegrationsForTest(model), "pushOrder"), "integration");
        assertEquals(expected, guard(GlueIntentGenerator.buildOutboundForTest(model), "publishOrder"), "outbound");
    }

    /**
     * The list form is the implicit AND of #6957. On this axis the whole list used to be stringified
     * into the scalar pattern, which never matched - so a two-term guard rendered {@code true} and the
     * mail went out on every update.
     */
    @Test
    void theListFormRendersAsAConjunction() {
        assertEquals("(entity.Status != null && entity.Status.longValue() == 2L) && java.util.Objects.equals(entity.Channel, \"web\")",
                guard(GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(YAML)), "web-issued-mail"));
    }

    /**
     * A guard on the record's own string field keeps the exact boxed equality - its width is declared.
     */
    @Test
    void aStringGuardKeepsTheBoxedEquality() {
        IntentModel model = IntentParser.parse(YAML.replace("when: \"Status == ISSUED\"", "when: \"channel == 'web'\""));

        assertEquals("java.util.Objects.equals(entity.Channel, \"web\")",
                guard(GlueIntentGenerator.buildNotificationsForTest(model), "issued-mail"));
    }

    private static String guard(List<Map<String, Object>> entries, String name) {
        for (Map<String, Object> entry : entries) {
            if (name.equals(entry.get("name"))) {
                return String.valueOf(entry.get("guardExpression"));
            }
        }
        throw new AssertionError("no glue entry [" + name + "] in " + entries);
    }
}
