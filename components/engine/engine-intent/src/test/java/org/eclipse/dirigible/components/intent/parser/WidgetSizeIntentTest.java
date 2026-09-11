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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * A field's or a to-one relation's {@code size:} is a column count on the form's 12-column grid;
 * the Harmonia form renders it as {@code sm:col-span-<n>}, a class that exists for 1..12 only. Any
 * other number used to reach the templates unchecked and render a width class Harmonia never ships.
 */
class WidgetSizeIntentTest {

    private static final String ORDERS = """
            name: orders
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, size: 4 }
                  - { name: note, type: string }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, size: 8 }
            """;

    @Test
    void aSizeWithinTheGridParses() {
        IntentModel model = IntentParser.parse(ORDERS);

        EntityIntent order = entity(model, "Order");
        assertEquals(4, order.getFields()
                             .get(1)
                             .getSize());
        assertEquals(8, order.getRelations()
                             .get(0)
                             .getSize());
    }

    @Test
    void aFieldSizeAboveTheGridIsRefusedNamingTheField() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(ORDERS.replace("size: 4", "size: 20")));

        assertTrue(ex.getIssues()
                     .contains("entity [Order] field [number] declares size 20 - a form width is a column count between 1 and 12"),
                () -> String.valueOf(ex.getIssues()));
    }

    @Test
    void aRelationSizeBelowTheGridIsRefusedNamingTheRelation() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(ORDERS.replace("size: 8", "size: 0")));

        assertTrue(ex.getIssues()
                     .contains("entity [Order] relation [customer] declares size 0 - a form width is a column count between 1 and 12"),
                () -> String.valueOf(ex.getIssues()));
    }

    @Test
    void anAbsentSizeStaysUnset() {
        IntentModel model = IntentParser.parse(ORDERS);

        assertNull(entity(model, "Order").getFields()
                                         .get(2)
                                         .getSize(),
                "no size authored means no widgetSize emitted - the form keeps its default width");
    }

    private static EntityIntent entity(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(e -> name.equals(e.getName()))
                    .findFirst()
                    .orElseThrow();
    }
}
