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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A {@code number: { per: Company }} field is unique WITHIN its partition - identical numbers
 * across partitions are the contract (#7015). So the number never carries a single-column UNIQUE;
 * the key the author means is the composite (partition, number), synthesized unless declared by
 * hand.
 */
class EdmPartitionedNumberUniqueTest {

    private static final String BILLING =
            """
                    name: billing
                    entities:
                      - name: Company
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: SalesInvoice
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, unique: true, length: 100, number: { series: Sales Invoice, per: Company, stampOn: issue } }
                        relations:
                          - { name: Company, kind: manyToOne, to: Company, required: true }
                    """;

    @Test
    void aPartitionedNumberDropsItsSingleColumnUniqueForTheCompositeKey() {
        Map<String, Object> number = property(BILLING, "SalesInvoice", "Number");
        assertNull(number.get("dataUnique"),
                "a single-column UNIQUE would make the second company's first number collide with the first's");
        assertEquals("Company", number.get("numberPer"), "sanity: the partition marker is still there");

        Map<String, Object> constraint = onlyConstraint(BILLING, "SalesInvoice");
        assertEquals("SalesInvoice_Company_Number", constraint.get("name"));
        assertEquals(List.of("SALES_INVOICE_COMPANY", "SALES_INVOICE_NUMBER"), columnNames(constraint),
                "the partition's foreign-key column first, then the number - the shape a per-company range has");
        assertEquals("Company,Number", constraint.get("properties"));
        assertEquals("A sales invoice with the same company and number already exists", constraint.get("message"));
    }

    @Test
    void thePartitionMakesTheKeyEvenWithoutUniqueTrue() {
        String yaml = BILLING.replace("unique: true, ", "");
        assertNull(property(yaml, "SalesInvoice", "Number").get("dataUnique"));
        Map<String, Object> constraint = onlyConstraint(yaml, "SalesInvoice");
        assertEquals(List.of("SALES_INVOICE_COMPANY", "SALES_INVOICE_NUMBER"), columnNames(constraint),
                "the platform owns the number, so it owns its uniqueness - a silent duplicate within one company is never acceptable");
    }

    @Test
    void anUnpartitionedNumberKeepsItsSingleColumnUnique() {
        String yaml = BILLING.replace("per: Company, ", "");
        Map<String, Object> number = property(yaml, "SalesInvoice", "Number");
        assertEquals("true", number.get("dataUnique"), "one sequence for the whole tenant - the column itself is the key");
        assertNull(entity(yaml, "SalesInvoice").get("uniqueConstraints"), "and there is nothing composite to synthesize");
    }

    @Test
    void aHandDeclaredKeyIsHonouredAndNeverDoubled() {
        String yaml = BILLING.replace("  - name: SalesInvoice\n", "  - name: SalesInvoice\n" + "    unique:\n"
                + "      - { fields: [number, Company], message: \"This company already issued that number\" }\n");
        Map<String, Object> constraint = onlyConstraint(yaml, "SalesInvoice");
        assertEquals("SalesInvoice_Number_Company", constraint.get("name"), "the authored order and name win");
        assertEquals(List.of("SALES_INVOICE_NUMBER", "SALES_INVOICE_COMPANY"), columnNames(constraint));
        assertEquals("This company already issued that number", constraint.get("message"));
    }

    @Test
    void theSynthesizedKeyReachesTheEdmTwin() {
        String xml = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(BILLING), "billing");
        assertTrue(xml.contains("<uniqueKey><entity>SalesInvoice</entity><name>SalesInvoice_Company_Number</name>"
                + "<properties>Company,Number</properties>"), "the modeler round-trips the key like an authored one: " + xml);
        assertTrue(!xml.contains("dataUnique=\"true\""), "and the number property carries no single-column UNIQUE: " + xml);
    }

    private static Map<String, Object> onlyConstraint(String yaml, String entityName) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> constraints = (List<Map<String, Object>>) entity(yaml, entityName).get("uniqueConstraints");
        assertEquals(1, constraints == null ? 0 : constraints.size(), "exactly one key on " + entityName + ": " + constraints);
        return constraints.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<String> columnNames(Map<String, Object> constraint) {
        return ((List<Map<String, Object>>) constraint.get("columns")).stream()
                                                                      .map(column -> String.valueOf(column.get("name")))
                                                                      .toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String entityName, String propertyName) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity(yaml, entityName).get("properties");
        return properties.stream()
                         .filter(property -> propertyName.equals(property.get("name")))
                         .findFirst()
                         .orElseThrow(() -> new AssertionError("no property [" + propertyName + "] on [" + entityName + "]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "billing");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
