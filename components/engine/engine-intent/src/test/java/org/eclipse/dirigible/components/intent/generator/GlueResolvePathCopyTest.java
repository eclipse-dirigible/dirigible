/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The motivating case of dirigible #7025: pricing an invoice LINE from a price list. Neither
 * operand the register is queried by is a column of the line - the list is the header's customer's,
 * the date in force is the header's - and the value the business needs is a scalar of the found
 * row, not the row itself. So the lookup declares to-one PATHS off the record and a {@code copy:},
 * and the glue carries the hops the handler loads plus the scalars it writes.
 */
class GlueResolvePathCopyTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: PriceList
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: priceList, kind: manyToOne, to: PriceList }
              - name: PriceListItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: price, type: decimal }
                  - { name: validFrom, type: date }
                  - { name: validTo, type: date }
                relations:
                  - { name: priceList, kind: manyToOne, to: PriceList }
                  - { name: product, kind: manyToOne, to: Product }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: price, type: decimal }
                  - { name: note, type: string }
                relations:
                  - { name: salesInvoice, kind: manyToOne, to: SalesInvoice, composition: true }
                  - { name: product, kind: manyToOne, to: Product }
                  - { name: priceListItem, kind: manyToOne, to: PriceListItem }
            resolves:
              - name: priceFromList
                event: { onCreate: SalesInvoiceItem }
                set: priceListItem
                from: PriceListItem
                match:
                  product: product
                  priceList: salesInvoice.customer.priceList
                between: { start: validFrom, end: validTo, value: salesInvoice.date }
                copy: { price: price }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void walksTheHeaderPathsAndCopiesTheFoundScalar() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> resolves = GlueIntentGenerator.buildResolvesForTest(model);
        assertEquals(1, resolves.size());
        Map<String, Object> resolve = resolves.get(0);

        // Two hops, shared by BOTH paths: the header is loaded once even though the key walks on
        // through the customer and the date is read straight off it.
        List<Map<String, Object>> loads = (List<Map<String, Object>>) resolve.get("pathLoads");
        assertEquals(2, loads.size());
        assertEquals("hop0", loads.get(0)
                                  .get("local"));
        assertEquals("SalesInvoice", loads.get(0)
                                          .get("entity"));
        assertEquals("entity.SalesInvoice", loads.get(0)
                                                 .get("sourceExpression"));
        assertEquals("hop1", loads.get(1)
                                  .get("local"));
        assertEquals("Customer", loads.get(1)
                                      .get("entity"));
        // The second hop's foreign key is read off the FIRST hop's local, null-guarded.
        assertEquals("(hop0 == null ? null : hop0.Customer)", loads.get(1)
                                                                   .get("sourceExpression"));

        List<Map<String, String>> matches = (List<Map<String, String>>) resolve.get("matches");
        assertEquals(2, matches.size());
        assertEquals("entity.Product", matches.get(0)
                                              .get("recordExpression"));
        assertEquals("(hop1 == null ? null : hop1.PriceList)", matches.get(1)
                                                                      .get("recordExpression"));
        // The summary and the generated javadoc name the authored walk, not the local it landed in.
        assertEquals("SalesInvoice.Customer.PriceList", matches.get(1)
                                                               .get("recordProperty"));
        assertEquals("Product = Product, PriceList = SalesInvoice.Customer.PriceList", resolve.get("matchSummary"));

        assertEquals("(hop0 == null ? null : hop0.Date)", resolve.get("valueExpression"));
        assertEquals("SalesInvoice.Date", resolve.get("valueProperty"));

        assertEquals(List.of(Map.of("registerProperty", "Price", "recordProperty", "Price")), resolve.get("copies"));
        assertEquals("true", resolve.get("hasCopies"));
        assertEquals("Price -> Price", resolve.get("copySummary"));
    }

    /**
     * The two ways a path can be wrong, both reported at parse: a middle segment that is not a to-one,
     * and a terminal segment the walk's target does not declare.
     */
    @Test
    void refusesAPathThatDoesNotWalk() {
        String broken = YAML.replace("priceList: salesInvoice.customer.priceList", "priceList: salesInvoice.price.priceList");
        String issues = assertRefused(broken);
        assertTrue(issues.contains("[SalesInvoice] has no to-one relation [price]"), issues);

        String terminal = YAML.replace("value: salesInvoice.date", "value: salesInvoice.issued");
        issues = assertRefused(terminal);
        assertTrue(issues.contains("[SalesInvoice] has no field or to-one relation [issued]"), issues);
    }

    /** A period path must still END at a date - a text column would compare as text. */
    @Test
    void refusesAPeriodPathThatDoesNotEndAtADate() {
        String issues = assertRefused(YAML.replace("- { name: date, type: date }", "- { name: date, type: string }"));
        assertTrue(issues.contains("must end at a date or timestamp field, was [string]"), issues);
    }

    /** A copy writes the value through unchanged, so the two columns must be the same type. */
    @Test
    void refusesACopyBetweenDifferentTypes() {
        String issues = assertRefused(YAML.replace("copy: { price: price }", "copy: { price: note }"));
        assertTrue(issues.contains("copy [price] is type [decimal] but [note] is type [string]"), issues);
    }

    /** A copy takes a SCALAR: the relation the row points at is what set: fills. */
    @Test
    void refusesACopyOfARelation() {
        String issues = assertRefused(YAML.replace("copy: { price: price }", "copy: { product: product }"));
        assertTrue(issues.contains("copy source [product] is not a field of register [PriceListItem]"), issues);
    }

    private static String assertRefused(String yaml) {
        try {
            IntentParser.parse(yaml);
            throw new AssertionError("expected the intent to be refused");
        } catch (org.eclipse.dirigible.components.intent.parser.IntentValidationException expected) {
            return String.join("; ", expected.getIssues());
        }
    }
}
