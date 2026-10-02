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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * An entity's {@code list:} is its list tables' exact column set and order, apart from the control
 * order {@code order:} gives the form (dirigible #7614). It is validated like {@code order:}.
 */
class ListColumnsIntentTest {

    /** An invoice whose list wants a different order than its form. */
    private static String invoice(String list) {
        return """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Invoice
                    label: "{number}"
                    list: %s
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: date, type: date }
                      - { name: due, type: date }
                      - { name: total, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: lines, kind: oneToMany, to: Customer }
                """.formatted(list);
    }

    @Test
    void aListOfFieldsAndToOneRelationsParses() {
        IntentModel model = IntentParser.parse(invoice("[Number, Date, Customer, Total, Due]"));

        assertEquals(List.of("Number", "Date", "Customer", "Total", "Due"), model.getEntities()
                                                                                 .get(1)
                                                                                 .getList());
    }

    @Test
    void namesMatchCaseInsensitivelyAndTheLabelNameIsAColumn() {
        IntentParser.parse(invoice("[number, customer, Name]"));
    }

    @Test
    void anUnknownNameIsRefused() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, Balance]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list references [Balance] which is not a field or to-one relation of the entity"),
                ex.getMessage());
    }

    @Test
    void aNameListedTwiceIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, number]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list names [number] more than once"),
                ex.getMessage());
    }

    @Test
    void aToManyRelationHasNoColumnAndIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, lines]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list references [lines]"),
                ex.getMessage());
    }

    @Test
    void aBlankEntryIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, \"\"]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list has a blank entry"),
                ex.getMessage());
    }
}
