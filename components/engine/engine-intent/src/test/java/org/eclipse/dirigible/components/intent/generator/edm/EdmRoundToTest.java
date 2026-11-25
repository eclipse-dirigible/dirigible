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
 * {@code roundTo:} (dirigible #8307 / #8327) - a money field rounded to the minor units of the
 * currency the record reaches. The property carries the path and the read of the digits off the
 * last hop; the entity carries the hops, shared by every field walking the same path. The parser
 * refuses a non-decimal field, a path that does not resolve and one that does not end in an
 * integer.
 */
class EdmRoundToTest {

    private static String invoicing(String netExtra, String rateExtra) {
        return """
                name: invoicing
                entities:
                  - name: Currency
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: code,     type: string, length: 3 }
                      - { name: rounding, type: integer }
                  - name: Invoice
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal, scale: 4, aggregate: true, roundTo: Currency.rounding }
                    relations:
                      - { name: Currency, kind: manyToOne, to: Currency }
                  - name: InvoiceItem
                    fields:
                      - { name: id,    type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal, scale: 4, roundTo: Invoice.Currency.rounding }
                      - { name: net,   type: decimal, scale: 4%s }
                      - { name: rate,  type: integer%s }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                """.formatted(netExtra, rateExtra);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aLineReadsItsHeadersCurrencyThroughSharedHops() {
        Map<String, Object> item = entity(invoicing(", roundTo: Invoice.Currency.rounding", ""), "InvoiceItem");
        Map<String, Object> total = property(item, "Total");
        assertEquals("Invoice.Currency.Rounding", total.get("roundTo"));
        assertEquals("(hop1 == null ? null : hop1.Rounding)", total.get("roundToExpression"));
        assertEquals(total.get("roundToExpression"), property(item, "Net").get("roundToExpression"));
        List<Map<String, Object>> loads = (List<Map<String, Object>>) item.get("roundToLoads");
        assertEquals(2, loads.size(), "two fields on one path load the header and the currency once: " + loads);
        assertEquals("entity.Invoice", loads.get(0)
                                            .get("sourceExpression"));
        assertEquals("Invoice", loads.get(0)
                                     .get("entity"));
        assertEquals("(hop0 == null ? null : hop0.Currency)", loads.get(1)
                                                                   .get("sourceExpression"));
        assertEquals("Currency", loads.get(1)
                                      .get("entity"));
    }

    @Test
    void aHeaderReadsItsOwnCurrency() {
        Map<String, Object> invoice = entity(invoicing("", ""), "Invoice");
        assertEquals("(hop0 == null ? null : hop0.Rounding)", property(invoice, "Total").get("roundToExpression"));
    }

    @Test
    void noRoundToLeavesTheModelUnchanged() {
        Map<String, Object> item = entity(invoicing("", ""), "InvoiceItem");
        assertFalse(property(item, "Net").containsKey("roundTo"));
        assertFalse(property(item, "Net").containsKey("roundToExpression"));
    }

    @Test
    void aNonDecimalFieldIsRefused() {
        assertRefused(invoicing("", ", roundTo: Invoice.Currency.rounding"), "roundTo applies to a decimal field");
    }

    @Test
    void anUnresolvedPathIsRefused() {
        assertRefused(invoicing(", roundTo: Invoice.Valuta.rounding", ""), "roundTo [Invoice.Valuta.rounding]");
    }

    @Test
    void aPathNotEndingInAnIntegerIsRefused() {
        assertRefused(invoicing(", roundTo: Invoice.Currency.code", ""), "must end in an integer field");
        assertRefused(invoicing(", roundTo: Invoice.Currency", ""), "got a to-one relation");
    }

    private static void assertRefused(String yaml, String expected) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(refused.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "]: " + refused.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "invoicing");
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
