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

    /** An order with its lines (objects, one element lookup each) and its tags (bare values). */
    private static final String ORDERS = """
            name: shop
            entities:
              - name: Product
                fields:
                  - { name: id,  type: integer, primaryKey: true, generated: true }
                  - { name: sku, type: string, unique: true }
              - name: PurchaseOrder
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: orderNumber, type: string, unique: true }
                relations:
                  - { name: lines, kind: oneToMany, to: OrderLine }
                  - { name: tags,  kind: oneToMany, to: OrderTag }
              - name: OrderLine
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer }
                relations:
                  - { name: purchaseOrder, kind: manyToOne, to: PurchaseOrder, composition: true }
                  - { name: product,       kind: manyToOne, to: Product }
              - name: OrderTag
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                relations:
                  - { name: purchaseOrder, kind: manyToOne, to: PurchaseOrder, composition: true }
            inbound:
              - name: orderHook
                path: /orders
                create: PurchaseOrder
                map: &order
                  orderNumber: orderNumber
                  lines:
                    from: lines
                    max: 50
                    map:
                      quantity: qty
                      product: { lookup: Product, by: sku, from: sku }
                  tags: { from: tags, map: { label: "." } }
              - name: orderQueue
                source: { queue: orders }
                create: PurchaseOrder
                map: *order
              - name: orderDrop
                source: { folder: target/orders, cron: "0/5 * * * * ?" }
                create: PurchaseOrder
                map: *order
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

    @Test
    void everyArrivalKindCarriesTheSameCollections() {
        IntentModel model = IntentParser.parse(ORDERS);
        Map<String, Object> webhook = GlueIntentGenerator.buildInboundForTest(model)
                                                         .get(0);
        Map<String, Object> consumer = GlueIntentGenerator.buildInboundMessagesForTest(model)
                                                          .get(0);
        Map<String, Object> job = GlueIntentGenerator.buildInboundFilesForTest(model)
                                                     .get(0);

        for (Map<String, Object> entry : List.of(webhook, consumer, job)) {
            assertEquals(Boolean.TRUE, entry.get("hasMap"));
            assertEquals(Boolean.TRUE, entry.get("hasCollections"));
            assertEquals(List.of("OrderNumber"), propertiesOf(entry), "a collection is not a top-level field");
            assertTrue(((List<?>) entry.get("lookups")).isEmpty(), "an element lookup is the collection's, not the record's");

            List<?> collections = (List<?>) entry.get("collections");
            assertEquals(2, collections.size());
            Map<?, ?> lines = (Map<?, ?>) collections.get(0);
            assertEquals("collectionLines", lines.get("local"));
            assertEquals("\"lines\"", lines.get("fromLiteral"));
            assertEquals(Boolean.TRUE, lines.get("hasMax"));
            assertEquals("50", lines.get("max"));
            assertEquals("OrderLine", lines.get("childEntity"));
            assertEquals("PurchaseOrder", lines.get("childPerspective"));
            assertEquals("PurchaseOrder", lines.get("backRef"));
            assertEquals("Id", lines.get("masterKey"));
            assertEquals(Boolean.FALSE, lines.get("scalarElements"));
            assertEquals(List.of("Quantity"), elementPropertiesOf(lines));
            Map<?, ?> product = (Map<?, ?>) ((List<?>) lines.get("lookups")).get(0);
            assertEquals("lookupLinesProduct", product.get("local"));
            assertEquals("Product", product.get("targetEntity"));
            assertEquals("Sku", product.get("byProperty"));
            assertEquals("String.valueOf(lookupLinesProductKey)", product.get("byValueExpression"));

            Map<?, ?> tags = (Map<?, ?>) collections.get(1);
            assertEquals(Boolean.TRUE, tags.get("scalarElements"));
            assertEquals(Boolean.FALSE, tags.get("hasMax"));
            assertEquals(List.of("Label"), elementPropertiesOf(tags));
            assertTrue(((List<?>) tags.get("lookups")).isEmpty());
        }
    }

    @Test
    void anArrivalWithoutCollectionsCarriesThemEmpty() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> entries = List.of(GlueIntentGenerator.buildInboundForTest(model)
                                                                       .get(0),
                GlueIntentGenerator.buildInboundMessagesForTest(model)
                                   .get(0),
                GlueIntentGenerator.buildInboundFilesForTest(model)
                                   .get(0),
                GlueIntentGenerator.buildInboundForTest(IntentParser.parse(PLAIN))
                                   .get(0));
        for (Map<String, Object> entry : entries) {
            assertEquals(Boolean.FALSE, entry.get("hasCollections"));
            assertTrue(((List<?>) entry.get("collections")).isEmpty());
        }
    }

    private static List<String> elementPropertiesOf(Map<?, ?> collection) {
        return ((List<?>) collection.get("mapFields")).stream()
                                                      .map(field -> String.valueOf(((Map<?, ?>) field).get("property")))
                                                      .toList();
    }
}
