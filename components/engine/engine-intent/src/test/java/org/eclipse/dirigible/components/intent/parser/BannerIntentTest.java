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

import java.util.List;

import org.eclipse.dirigible.components.intent.model.BannerIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * {@code banners:} on an entity (dirigible #8371): a record-page sentence held by a condition on
 * the record, on a row of one of its related registers, or both, optionally with the action that
 * fixes it.
 */
class BannerIntentTest {

    /** An order with its shipments' register; the banners are spliced in. */
    static String orders(String banners) {
        return """
                name: shipping
                uses:
                  - { model: carriers }
                seeds:
                  - name: order-statuses
                    entity: OrderStatus
                    rows:
                      - { id: 1, name: IN_PROGRESS }
                      - { id: 2, name: ON_HOLD }
                  - name: shipment-statuses
                    entity: ShipmentStatus
                    rows:
                      - { id: 1, name: PENDING }
                      - { id: 2, name: DELIVERED }
                      - { id: 3, name: FAILED }
                entities:
                  - name: OrderStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                  - name: ShipmentStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                  - name: Order
                    label: "{number}"
                    related:
                      - { entity: Shipment }
                      - { entity: Waybill, model: carriers }
                %s
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 20 }
                    relations:
                      - { name: status, kind: manyToOne, to: OrderStatus, function: EntityStatus }
                  - name: Shipment
                    fields:
                      - { name: id,         type: integer, primaryKey: true, generated: true }
                      - { name: failure,    type: string, length: 500, readOnly: true }
                      - { name: finishedAt, type: timestamp, readOnly: true }
                    relations:
                      - { name: order,  kind: manyToOne, to: Order }
                      - { name: status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus }
                  - name: Customer
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 50 }
                transitions:
                  - { name: RetryShipment, forEntity: Shipment, from: [3], setStatus: 1 }
                  - { name: HoldOrder, forEntity: Order, from: [1], setStatus: 2 }
                """.formatted(banners);
    }

    static final String BOTH_KINDS = """
                banners:
                  - text: "The last shipment failed: {failure}"
                    variant: negative
                    related: { entity: Shipment, row: latest, when: "status == FAILED" }
                    action: RetryShipment
                  - text: "This order is on hold."
                    variant: warning
                    when: "status == ON_HOLD"
            """;

    @Test
    void aBannerOnOwnFieldsAndOnALatestRelatedRowParses() {
        List<BannerIntent> banners = order(IntentParser.parse(orders(BOTH_KINDS))).getBanners();
        assertEquals(2, banners.size());
        assertEquals("Shipment", banners.get(0)
                                        .getRelated()
                                        .getEntity());
        assertEquals("latest", banners.get(0)
                                      .getRelated()
                                      .getRow());
        assertEquals("RetryShipment", banners.get(0)
                                             .getAction());
        assertEquals("warning", banners.get(1)
                                       .variantOrDefault());
    }

    @Test
    void aBannerNeedsWhenOrRelated() {
        assertIssue(orders("""
                    banners:
                      - { text: "Always here." }
                """), "entity [Order] banners[1] needs a when, a related, or both");
        assertIssue(orders("""
                    banners:
                      - { text: "Hm.", variant: danger, when: "status == ON_HOLD" }
                """), "variant must be information, warning or negative");
    }

    @Test
    void aRelatedEntityThatIsNoRegisterIsRefused() {
        assertIssue(orders("""
                    banners:
                      - { text: "x", related: { entity: Customer, row: latest, when: "name == X" } }
                """), "entity [Customer] is not one of [Order]'s related: registers");
        assertIssue(orders("""
                    banners:
                      - { text: "x", related: { entity: Shipment, row: first, when: "status == FAILED" } }
                """), "row must be latest or any");
    }

    @Test
    void aCrossModelRegisterIsRefused() {
        assertIssue(orders("""
                    banners:
                      - { text: "x", related: { entity: Waybill, row: any, when: "status == 1" } }
                """), "is a register from another model");
    }

    @Test
    void aPlaceholderMustNameASubjectField() {
        assertIssue(orders("""
                    banners:
                      - { text: "Order {number} failed", related: { entity: Shipment, row: latest, when: "status == FAILED" } }
                """), "text names {number}, which is not a field of [Shipment]");
        assertIssue(orders("""
                    banners:
                      - { text: "Failed: {status}", related: { entity: Shipment, row: latest, when: "status == FAILED" } }
                """), "a relation has no text of its own");
        IntentParser.parse(orders("""
                    banners:
                      - { text: "Order {number} is on hold.", when: "status == ON_HOLD" }
                """));
    }

    @Test
    void anActionOfAnotherEntityIsRefused() {
        assertIssue(orders("""
                    banners:
                      - { text: "x", related: { entity: Shipment, row: latest, when: "status == FAILED" }, action: HoldOrder }
                """), "action [HoldOrder] is declared for [Order], not for the banner's subject [Shipment]");
        assertIssue(orders("""
                    banners:
                      - { text: "x", when: "status == ON_HOLD", action: Vanish }
                """), "action [Vanish] is no action, transition or generates of this model");
    }

    @Test
    void statusNamesResolveInTheSourcesTerms() {
        List<BannerIntent> banners = order(IntentParser.parse(orders(BOTH_KINDS))).getBanners();
        assertEquals("status == 3", String.valueOf(banners.get(0)
                                                          .getRelated()
                                                          .getWhen()),
                "FAILED is the shipment's status 3 - the order has no FAILED");
        assertEquals("status == 2", String.valueOf(banners.get(1)
                                                          .getWhen()),
                "ON_HOLD is the order's own status 2");
        assertIssue(orders("""
                    banners:
                      - { text: "x", related: { entity: Shipment, row: latest, when: "status == ON_HOLD" } }
                """), "ON_HOLD");
    }

    private static org.eclipse.dirigible.components.intent.model.EntityIntent order(IntentModel model) {
        return model.getEntities()
                    .stream()
                    .filter(e -> "Order".equals(e.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
