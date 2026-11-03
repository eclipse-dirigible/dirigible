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

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A roll-up's targets on its local parent are read-only whether or not the author declared
 * {@code readOnly: true} (dirigible #8442). {@code isReadOnlyProperty} is what puts a column in the
 * generated repository's preserved-on-update set and the controllers' dropped-on-create set;
 * without it a full-row PUT that omitted {@code Paid} wrote it back to null.
 */
class EdmRollupTargetReadOnlyTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: total,        type: decimal }
                  - { name: paid,         type: decimal }
                  - { name: balance,      type: decimal }
                  - { name: paymentCount, type: integer }
                  - { name: lastPaidOn,   type: date }
                  - { name: note,         type: string }
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: paidOn, type: date }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice }
            rollups:
              - { name: invoicePaid, entity: Payment, via: Invoice, field: paid, op: sum, of: amount,
                  capacity: total, balance: balance }
              - { name: invoicePayments, entity: Payment, via: Invoice, field: paymentCount }
              - { name: invoiceLastPaid, entity: Payment, via: Invoice, field: lastPaidOn, op: latest, of: paidOn, by: paidOn }
            """;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String entityName, String propertyName) {
        Map<String, Object> json = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "test");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) json.get("model")).get("entities");
        for (Map<String, Object> entity : entities) {
            if (entityName.equals(entity.get("name"))) {
                for (Map<String, Object> property : (List<Map<String, Object>>) entity.get("properties")) {
                    if (propertyName.equals(property.get("name"))) {
                        return property;
                    }
                }
            }
        }
        throw new AssertionError("no property " + entityName + "." + propertyName);
    }

    @Test
    void everyFieldARollupWritesIsReadOnly() {
        assertEquals("true", property(YAML, "Invoice", "Paid").get("isReadOnlyProperty"), "the sum target");
        assertEquals("true", property(YAML, "Invoice", "Balance").get("isReadOnlyProperty"), "the capacity balance");
        assertEquals("true", property(YAML, "Invoice", "PaymentCount").get("isReadOnlyProperty"), "the count target");
        assertEquals("true", property(YAML, "Invoice", "LastPaidOn").get("isReadOnlyProperty"), "the latest target");
    }

    @Test
    void theCapacityAndTheOtherFieldsStayEditable() {
        assertNull(property(YAML, "Invoice", "Total").get("isReadOnlyProperty"), "the capacity is the user's");
        assertNull(property(YAML, "Invoice", "Note").get("isReadOnlyProperty"));
        assertNull(property(YAML, "Payment", "Amount").get("isReadOnlyProperty"), "the summed child field is the user's");
    }

    @Test
    void aRequiredTargetWithoutADefaultIsRefused() {
        String message =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("{ name: paid,         type: decimal }",
                        "{ name: paid,         type: decimal, required: true }"))).getMessage();
        assertTrue(message.contains("rollup [invoicePaid] field [paid] is required"), message);
        String balance =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML.replace("{ name: balance,      type: decimal }",
                        "{ name: balance,      type: decimal, required: true }"))).getMessage();
        assertTrue(balance.contains("rollup [invoicePaid] balance [balance] is required"), balance);
        // With a default the create stores it, and the roll-up takes over from there.
        assertEquals("true",
                property(YAML.replace("{ name: paid,         type: decimal }",
                        "{ name: paid,         type: decimal, required: true, defaultValue: \"0\" }"), "Invoice", "Paid").get(
                                "isReadOnlyProperty"));
    }
}
