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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * {@code inlineCreate: false} (issue #7748): a to-one picker offering existing rows only.
 */
class InlineCreateIntentTest {

    private static final String HEAD = """
            name: sales
            uses:
              - { model: payments, project: payments }
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Payment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Allocation
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
            """;

    @Test
    void anExplicitFalseIsEmittedOnTheForeignKey() {
        Map<String, Object> payment = allocationProperty("""
                  - { name: Payment, kind: manyToOne, to: Payment, inlineCreate: false }
                """, "Payment");
        assertEquals("false", payment.get("widgetInlineCreate"));
    }

    @Test
    void aCrossModelTargetCarriesItToo() {
        Map<String, Object> payment = allocationProperty("""
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: payments, inlineCreate: false }
                """, "CustomerPayment");
        assertEquals("false", payment.get("widgetInlineCreate"));
    }

    /** Absent - or true, the generated UI's own default - nothing changes in the model. */
    @Test
    void absentOrTrueEmitsNothing() {
        assertFalse(allocationProperty("""
                  - { name: Payment, kind: manyToOne, to: Payment }
                """, "Payment").containsKey("widgetInlineCreate"));
        assertFalse(allocationProperty("""
                  - { name: Payment, kind: manyToOne, to: Payment, inlineCreate: true }
                """, "Payment").containsKey("widgetInlineCreate"));
    }

    @Test
    void theAttributeRidesTheEdmTwin() {
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(withRelation("""
                  - { name: Payment, kind: manyToOne, to: Payment, inlineCreate: false }
                """)), "sales");
        assertTrue(edm.contains("widgetInlineCreate=\"false\""), edm);
    }

    /** An n:m's picker lives on the materialised link row, so the key travels there. */
    @Test
    void aManyToManyCarriesItOntoTheLinkPicker() {
        String yaml = HEAD + """
                  - name: Voucher
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payments, kind: manyToMany, to: Payment, inlineCreate: false }
                """;
        Map<String, Object> link = entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "sales"), "VoucherPayment");
        assertEquals("false", propertyByName(link, "Payment").get("widgetInlineCreate"));
    }

    @Test
    void aCollectionRelationIsRefused() {
        String yaml = HEAD + """
                  - name: Ledger
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: allocations, kind: oneToMany, to: Allocation, inlineCreate: false }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("relation [allocations] declares `inlineCreate`, but a collection relation has no picker")),
                ex.getIssues()
                  .toString());
    }

    private static String withRelation(String relation) {
        return HEAD + relation.indent(4);
    }

    private static Map<String, Object> allocationProperty(String relation, String name) {
        return propertyByName(
                entity(EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(withRelation(relation)), "sales"), "Allocation"), name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Map<String, Object> modelJson, String name) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> name.equals(e.get("name")))
                       .findFirst()
                       .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        Map<String, Object> property = properties.stream()
                                                 .filter(p -> name.equals(p.get("name")))
                                                 .findFirst()
                                                 .orElse(null);
        assertFalse(property == null, "property [" + name + "] not found");
        return property;
    }
}
