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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * {@code emptyText:} (dirigible #8605) reaches the model as the trimmed scalar
 * {@code widgetEmptyText} on the property and on a {@code related:} register column. Without it the
 * property map is unchanged.
 */
class EdmFieldEmptyTextTest {

    private static String lending(String maxLoansExtra) {
        return """
                name: lending
                entities:
                  - name: Member
                    related:
                      - { entity: Loan }
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: name,     type: string, length: 80, required: true }
                      - { name: maxLoans, type: integer, readOnly: true, label: Loan limit%s }
                  - name: Loan
                    fields:
                      - { name: id,         type: integer, primaryKey: true, generated: true }
                      - { name: returnedOn, type: date, emptyText: Not returned }
                    relations:
                      - { name: member, kind: manyToOne, to: Member }
                """.formatted(maxLoansExtra);
    }

    @Test
    void anEmptyTextIsEmittedTrimmedAsWidgetEmptyText() {
        Map<String, Object> maxLoans = property(entity(lending(", emptyText: '  No limit  '"), "Member"), "MaxLoans");
        assertEquals("No limit", maxLoans.get("widgetEmptyText"));
        assertEquals("Loan limit", maxLoans.get("widgetLabel"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRelatedRegisterColumnCarriesIt() {
        Map<String, Object> member = entity(lending(""), "Member");
        List<Map<String, Object>> registers = (List<Map<String, Object>>) member.get("relatedEntities");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) registers.get(0)
                                                                                 .get("properties");
        Map<String, Object> returnedOn = columns.stream()
                                                .filter(c -> "ReturnedOn".equals(c.get("name")))
                                                .findFirst()
                                                .orElseThrow(() -> new AssertionError("no register column [ReturnedOn]: " + columns));
        assertEquals("Not returned", returnedOn.get("widgetEmptyText"));
    }

    @Test
    void noEmptyTextLeavesThePropertyMapUnchanged() {
        Map<String, Object> with = property(entity(lending(", emptyText: No limit"), "Member"), "MaxLoans");
        Map<String, Object> without = property(entity(lending(""), "Member"), "MaxLoans");
        assertFalse(without.containsKey("widgetEmptyText"));
        with.remove("widgetEmptyText");
        assertEquals(without, with, "the key adds widgetEmptyText and nothing else");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "lending");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> entity, String name) {
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(property -> name.equals(property.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError("no property [" + name + "]"));
    }
}
