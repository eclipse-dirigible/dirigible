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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The arrival mapping reaches all three inbound collections through the one shared entry, so a
 * webhook, a queue consumer and a polled folder cannot drift in how they read the same envelope.
 */
class GlueInboundMappingTest {

    private static final String YAML = """
            name: provisioning
            entities:
              - name: Tenant
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: tenantId, type: string, unique: true }
                  - { name: name,     type: string }
              - name: TenantUserAssignment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: messageId, type: string, unique: true }
                  - { name: email,     type: string }
                relations:
                  - { name: tenant, kind: manyToOne, to: Tenant }
            inbound:
              - name: assignmentHook
                path: /assignments
                accept: { type: user.assignment.requested, version: 1 }
                create: TenantUserAssignment
                map:
                  messageId: messageId
                  email:     email
                  tenant:    { lookup: Tenant, by: tenantId, from: tenantId }
              - name: assignmentQueue
                source: { queue: "global:codbex.user-assignment-requests" }
                accept: { type: user.assignment.requested, version: 1 }
                create: TenantUserAssignment
                map:
                  messageId: messageId
                  tenant:    { lookup: Tenant, by: tenantId, from: tenantId }
              - name: assignmentDrop
                source: { folder: target/inbox, cron: "0/5 * * * * ?" }
                accept: { type: user.assignment.requested, version: 1 }
                create: TenantUserAssignment
                map:
                  messageId: messageId
                  tenant:    { lookup: Tenant, by: tenantId, from: tenantId }
            """;

    /** The map-less arrival, which must keep emitting exactly what it emitted before the feature. */
    private static final String PLAIN = """
            name: crm
            entities:
              - name: Lead
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            inbound:
              - { name: leadHook, path: /webhooks/lead, create: Lead }
            """;

    @Test
    void everyArrivalKindCarriesTheSameGateAndProjection() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Object> webhook = GlueIntentGenerator.buildInboundForTest(model)
                                                         .get(0);
        Map<String, Object> consumer = GlueIntentGenerator.buildInboundMessagesForTest(model)
                                                          .get(0);
        Map<String, Object> job = GlueIntentGenerator.buildInboundFilesForTest(model)
                                                     .get(0);

        for (Map<String, Object> entry : List.of(webhook, consumer, job)) {
            assertEquals(Boolean.TRUE, entry.get("hasEnvelope"));
            assertEquals(Boolean.TRUE, entry.get("hasAccept"));
            assertEquals(Boolean.TRUE, entry.get("hasMap"));
            assertEquals("type=user.assignment.requested, version=1", entry.get("acceptSummary"));
            assertTrue(entry.get("acceptExpression")
                            .toString()
                            .contains("\"user.assignment.requested\".equals(envelope.get(\"type\"))"),
                    "the gate is pre-rendered as one Java expression: " + entry.get("acceptExpression"));

            List<?> lookups = (List<?>) entry.get("lookups");
            assertEquals(1, lookups.size());
            Map<?, ?> lookup = (Map<?, ?>) lookups.get(0);
            assertEquals("Tenant", lookup.get("property"));
            assertEquals("Tenant", lookup.get("targetEntity"));
            assertEquals("TenantId", lookup.get("byProperty"));
            assertEquals("Id", lookup.get("targetKeyProperty"));
            assertEquals("String.valueOf(lookupTenantKey)", lookup.get("byValueExpression"));
        }

        // The projection is the entry's own, not a shared one: only the webhook maps the e-mail. A
        // lookup is not a mapped field - it is a query, so it lands in its own collection.
        assertEquals(List.of("MessageId", "Email"), propertiesOf(webhook));
        assertEquals(List.of("MessageId"), propertiesOf(consumer));
    }

    private static List<String> propertiesOf(Map<String, Object> entry) {
        return ((List<?>) entry.get("mapFields")).stream()
                                                 .map(field -> String.valueOf(((Map<?, ?>) field).get("property")))
                                                 .toList();
    }

    @Test
    void anArrivalWithoutTheKeysDeclaresNoMappingAtAll() {
        Map<String, Object> webhook = GlueIntentGenerator.buildInboundForTest(IntentParser.parse(PLAIN))
                                                         .get(0);

        // Present but false, never absent - a template branches on them, and an undefined Velocity
        // variable renders as its own literal name.
        assertEquals(Boolean.FALSE, webhook.get("hasEnvelope"));
        assertEquals(Boolean.FALSE, webhook.get("hasAccept"));
        assertEquals(Boolean.FALSE, webhook.get("hasMap"));
        assertTrue(((List<?>) webhook.get("mapFields")).isEmpty());
        assertTrue(((List<?>) webhook.get("lookups")).isEmpty());
        assertEquals("", webhook.get("acceptExpression"));
    }
}
