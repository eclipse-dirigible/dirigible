/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * An entity's read / write GATES come from the intent's {@code permissions[].can:} tokens whenever
 * a token names it (dirigible #6760). Before this, the gate was always the convention-derived
 * {@code <project>.<perspective>.<Entity>FullAccess} - a name no intent construct mentions - so
 * granting an authored role granted nothing at all, silently.
 */
class PermissionEntityGatesTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: items, kind: oneToMany, to: OrderItem }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer }
                relations:
                  - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            permissions:
              - { role: SalesAdmin,  can: [Order:read, Order:create, Order:delete] }
              - { role: SalesViewer, can: [Order:read] }
            """;

    @Test
    void anAuthoredGrantBecomesTheEntitysGate() {
        Map<String, Object> order = entity(YAML, "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"),
                "every grantee of the entity, read or write, must satisfy the read gate");
        assertEquals("SalesAdmin", order.get("roleWrite"));
        // The authored roles are already declared by <intent>.roles; re-declaring them from the
        // template's default-roles artefact would produce a second row per role name.
        assertEquals("false", order.get("generateDefaultRoles"));
    }

    @Test
    void anEntityNoTokenNamesKeepsTheConventionGates() {
        Map<String, Object> customer = entity(YAML, "Customer");
        assertEquals("sales.Customer.CustomerReadOnly", customer.get("roleRead"));
        assertEquals("sales.Customer.CustomerFullAccess", customer.get("roleWrite"));
        assertEquals("true", customer.get("generateDefaultRoles"), "the convention roles must still be declared and grantable");
    }

    @Test
    void aCompositionChildIsGatedByTheMasterItIsManagedUnder() {
        Map<String, Object> item = entity(YAML, "OrderItem");
        assertEquals("SalesAdmin,SalesViewer", item.get("roleRead"),
                "a document's items must be reachable to exactly the roles the document is");
        assertEquals("SalesAdmin", item.get("roleWrite"));
    }

    @Test
    void aReadOnlyAllowListLeavesNoOneAbleToWrite() {
        Map<String, Object> order = entity(YAML.replace("can: [Order:read, Order:create, Order:delete]", "can: [Order:read]"), "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"));
        // No grant may write, so the write gate names a role that is never declared - the honest
        // encoding of an allow-list that only grants reads.
        assertEquals("sales.Order.OrderFullAccess", order.get("roleWrite"));
        assertEquals("false", order.get("generateDefaultRoles"));
    }

    @Test
    void aWildcardGrantsBothGates() {
        Map<String, Object> order = entity(YAML.replace("can: [Order:read, Order:create, Order:delete]", "can: [Order:*]"), "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"));
        assertEquals("SalesAdmin", order.get("roleWrite"));
    }

    private static Map<String, Object> entity(String yaml, String name) {
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> json = EdmIntentGenerator.buildModelJsonForTest(model, "sales");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) json.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "] was emitted"));
    }

}
