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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The n:m promise at the layer that actually runs: a {@code manyToMany} must reach the
 * {@code .model} as a real link table with both foreign keys - the table the schema layer creates
 * and the detail grid the generated UI renders (#6718).
 */
class EdmManyToManyTest {

    private static final String ORDERS = """
            name: orders
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: products, kind: manyToMany, to: Product }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    @Test
    void theLinkEntityIsADetailTableWithBothForeignKeys() {
        List<Map<String, Object>> entities = entities(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(ORDERS), "orders"));

        Map<String, Object> link = entityByName(entities, "OrderProduct");
        assertNotNull(link, "the manyToMany must materialise a link entity");
        assertEquals("ORDERS_ORDER_PRODUCT", link.get("dataName"), "the link owns a real table");
        assertEquals("DEPENDENT", link.get("type"));
        assertEquals("MANAGE_DETAILS", link.get("layoutType"), "the link is edited as a detail grid of its owner, not a page of its own");

        Map<String, Object> ownerFk = propertyByName(link, "Order");
        assertEquals("COMPOSITION", ownerFk.get("relationshipType"));
        assertEquals("1_n", ownerFk.get("relationshipCardinality"));
        assertEquals("false", ownerFk.get("dataNullable"));

        Map<String, Object> targetFk = propertyByName(link, "Product");
        assertEquals("ASSOCIATION", targetFk.get("relationshipType"));
        assertEquals("n_1", targetFk.get("relationshipCardinality"));
        assertEquals("DROPDOWN", targetFk.get("widgetType"), "the target end is picked from a dropdown on the link row");
        assertEquals("Product", targetFk.get("relationshipEntityName"));
        assertEquals("false", targetFk.get("dataNullable"));
    }

    @Test
    void theDeclaringEntityKeepsNoColumnForTheNavigation() {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(ORDERS), "orders");
        List<Map<String, Object>> entities = entities(model);

        Map<String, Object> order = entityByName(entities, "Order");
        assertNull(propertyByName(order, "Products"), "the n:m lives on the link table - never as a column on the declaring entity");
        assertEquals("MANAGE_MASTER", order.get("layoutType"), "owning a link makes the declaring entity a master with a detail panel");

        // The link is a detail, so it must not claim navigation of its own.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> perspectives = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("perspectives");
        assertTrue(perspectives.stream()
                               .noneMatch(perspective -> "OrderProduct".equals(perspective.get("name"))),
                "a link entity must not create a perspective");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entities(Map<String, Object> model) {
        return (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
    }

    private static Map<String, Object> entityByName(List<Map<String, Object>> entities, String name) {
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        return properties.stream()
                         .filter(property -> name.equals(property.get("name")))
                         .findFirst()
                         .orElse(null);
    }
}
