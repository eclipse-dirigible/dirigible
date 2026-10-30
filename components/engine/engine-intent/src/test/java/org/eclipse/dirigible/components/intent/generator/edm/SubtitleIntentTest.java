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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * {@code subtitle: [field, ...]} (dirigible #8370): the line under the record page's title, own
 * fields and to-one relations only, at most four. It rides the entity as {@code subtitleOrder}, in
 * the model's property names.
 */
class SubtitleIntentTest {

    private static String order(String subtitle) {
        return """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                  - name: Order
                    label: "{customer.name} / {number}"
                %s    fields:
                      - { name: id,      type: integer, primaryKey: true, generated: true }
                      - { name: number,  type: string, length: 20 }
                      - { name: dueDate, type: date }
                      - { name: notes,   type: text }
                      - { name: total,   type: decimal }
                      - { name: closedOn, type: date, visibleWhen: "number != X" }
                    relations:
                      - { name: customer, kind: manyToOne, to: Customer }
                      - { name: lines,    kind: oneToMany, to: OrderLine }
                  - name: OrderLine
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal }
                    relations:
                      - { name: order, kind: manyToOne, to: Order, composition: true }
                """.formatted(subtitle);
    }

    @Test
    void aSubtitleOfFieldsAndToOneRelationsParses() {
        IntentModel model = IntentParser.parse(order("    subtitle: [dueDate, customer]\n"));
        assertEquals(List.of("dueDate", "customer"), model.getEntities()
                                                          .stream()
                                                          .filter(e -> "Order".equals(e.getName()))
                                                          .findFirst()
                                                          .orElseThrow()
                                                          .getSubtitle());
        assertEquals("DueDate,Customer", entity(order("    subtitle: [dueDate, customer]\n"), "Order").get("subtitleOrder"));
        assertNull(entity(order(""), "Order").get("subtitleOrder"), "no subtitle - no attribute");
    }

    @Test
    void anUnknownOrToManyEntryIsRefused() {
        assertIssue(order("    subtitle: [dueDate, shipDate]\n"),
                "entity [Order] subtitle names [shipDate], which is not a field or to-one relation of the entity");
        assertIssue(order("    subtitle: [lines]\n"), "entity [Order] subtitle names [lines], a collection path or a to-many relation");
        assertIssue(order("    subtitle: [lines.amount]\n"),
                "entity [Order] subtitle names [lines.amount], a collection path or a to-many relation");
        assertIssue(order("    subtitle: [id]\n"), "entity [Order] subtitle names [id], the primary key");
        assertIssue(order("    subtitle: [dueDate, DueDate]\n"), "entity [Order] subtitle names [DueDate] more than once");
    }

    @Test
    void aVisibleWhenFieldIsRefused() {
        assertIssue(order("    subtitle: [closedOn]\n"), "entity [Order] subtitle names [closedOn], which carries visibleWhen");
    }

    @Test
    void moreThanFourEntriesAreRefused() {
        assertIssue(order("    subtitle: [number, dueDate, customer, notes, total]\n"),
                "entity [Order] subtitle names 5 fields - at most 4 fit one line");
        IntentParser.parse(order("    subtitle: [number, dueDate, customer, notes]\n"));
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
