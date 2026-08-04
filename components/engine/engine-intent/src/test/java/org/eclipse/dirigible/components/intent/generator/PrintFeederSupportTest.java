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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

class PrintFeederSupportTest {

    private static final String INTENT = """
            name: sales
            uses:
              - { model: customers }
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: date, type: date }
                  - { name: total, type: decimal, aggregate: true }
                relations:
                  - { name: Status, kind: manyToOne, to: SalesInvoiceStatus }
                  - { name: Customer, kind: manyToOne, to: Customer, model: customers }
                  - { name: Region, kind: manyToOne, to: Region }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: quantity, type: decimal, required: true }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Unit, kind: manyToOne, to: Unit }
                  - { name: Product, kind: manyToOne, to: Product, model: customers }
              - name: SalesInvoiceStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Region
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Unit
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);

    /** No repository → cross-model resolution falls back to convention (safe for a unit test). */
    private Map<String, Object> feeder() {
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", null);
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        List<Map<String, Object>> feeders =
                PrintFeederSupport.buildPrintFeeders(model, byName, IntentEntities.compositionParents(model), context);
        assertEquals(1, feeders.size(), "one feeder per document master");
        return feeders.get(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void headerAndItemsAreProjected() {
        Map<String, Object> feeder = feeder();
        assertEquals("SalesInvoice", feeder.get("className"));
        assertEquals("SalesInvoice", feeder.get("entity"));

        List<Map<String, Object>> rootScalars = (List<Map<String, Object>>) feeder.get("rootScalars");
        Map<String, Boolean> rootByName = new java.util.LinkedHashMap<>();
        for (Map<String, Object> scalar : rootScalars) {
            rootByName.put((String) scalar.get("name"), (Boolean) scalar.get("date"));
        }
        assertTrue(rootByName.containsKey("Number"), "own field, PascalCased");
        assertTrue(rootByName.containsKey("Total"), "aggregate totals are still projected");
        assertFalse(rootByName.containsKey("Id"), "primary key is excluded");
        assertEquals(Boolean.TRUE, rootByName.get("Date"), "a date field is flagged so the feeder emits its ISO string");
        assertEquals(Boolean.FALSE, rootByName.get("Total"), "a numeric field is not flagged (money formatting stays with the binder)");

        assertEquals("SalesInvoiceItem", feeder.get("itemsEntity"));
        assertEquals("SalesInvoice", feeder.get("itemsFkProperty"), "the composition FK back to the master");
        List<Map<String, Object>> itemScalars = (List<Map<String, Object>>) feeder.get("itemScalars");
        List<String> itemNames = itemScalars.stream()
                                            .map(s -> (String) s.get("name"))
                                            .toList();
        assertTrue(itemNames.contains("Name") && itemNames.contains("Quantity"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sameModelRelationRecursesToDepthTwoAndCrossModelStopsAtOne() {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) feeder().get("nodes");
        Map<String, Map<String, Object>> byVar = new java.util.LinkedHashMap<>();
        for (Map<String, Object> node : nodes) {
            byVar.put((String) node.get("entityVar"), node);
        }

        // A setting relation resolves to the shared Settings perspective.
        Map<String, Object> status = byVar.get("status");
        assertNotNull(status);
        assertEquals(Boolean.FALSE, status.get("crossModel"));
        assertEquals("Settings", status.get("perspective"));

        // Cross-model relation: depth-1, no descent; convention label field with no repository.
        Map<String, Object> customer = byVar.get("customer");
        assertNotNull(customer);
        assertEquals(Boolean.TRUE, customer.get("crossModel"));
        assertEquals("customers", customer.get("model"));
        assertEquals("Name", customer.get("labelField"));

        // Same-model relation descends one more hop (Region -> Country) in pre-order.
        Map<String, Object> region = byVar.get("region");
        Map<String, Object> country = byVar.get("country");
        assertNotNull(region);
        assertNotNull(country, "depth-2 same-model relation is walked");
        assertEquals("region", country.get("parentEntityVar"));
        assertEquals("regionMap", country.get("parentMapVar"));
        assertEquals("document", region.get("parentMapVar"));
        assertTrue(nodes.indexOf(region) < nodes.indexOf(country), "parent materialised before child");
    }

    /**
     * dirigible #6422: the owner's field list must NOT be baked into this project's feeder - it would
     * be a snapshot of another model's schema, and the day the owner retires a field the consumer's
     * committed gen/ stops compiling (taking the whole client-Java batch with it). Only same-model
     * nodes name their fields; a cross-model node carries none and the template copies the record
     * reflectively instead.
     */
    @Test
    @SuppressWarnings("unchecked")
    void crossModelNodeNamesNoFieldsOfTheOwner() {
        Map<String, Object> feeder = feeder();
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) feeder.get("nodes");
        Map<String, Map<String, Object>> byVar = new java.util.LinkedHashMap<>();
        for (Map<String, Object> node : nodes) {
            byVar.put((String) node.get("entityVar"), node);
        }

        List<Map<String, Object>> customerScalars = (List<Map<String, Object>>) byVar.get("customer")
                                                                                     .get("scalars");
        assertTrue(customerScalars.isEmpty(), "a cross-model node dereferences no named field of the owner");

        List<Map<String, Object>> regionScalars = (List<Map<String, Object>>) byVar.get("region")
                                                                                   .get("scalars");
        assertFalse(regionScalars.isEmpty(), "a same-model node still names its fields - it is regenerated with them");

        assertEquals(Boolean.TRUE, feeder.get("hasCrossModel"), "the reflective copy helper is emitted for this feeder");
    }

    /**
     * An items-table column must be able to render a line's to-one relation ({@code {{Unit}}} - "1
     * month") exactly as the header relations resolve: one node per item to-one, the composition
     * back-reference to the document excluded, same-model fields named / cross-model copied
     * reflectively, and the variables prefixed so a header relation of the same name cannot collide.
     */
    @Test
    @SuppressWarnings("unchecked")
    void itemToOneRelationsAreFedWithLabelAndFields() {
        Map<String, Object> feeder = feeder();
        List<Map<String, Object>> itemNodes = (List<Map<String, Object>>) feeder.get("itemNodes");
        Map<String, Map<String, Object>> byKey = new java.util.LinkedHashMap<>();
        for (Map<String, Object> node : itemNodes) {
            byKey.put((String) node.get("keyInParent"), node);
        }

        assertFalse(byKey.containsKey("SalesInvoice"), "the composition back-reference is the header, not an item lookup");

        Map<String, Object> unit = byKey.get("Unit");
        assertNotNull(unit, "a same-model item relation is fed");
        assertEquals(Boolean.FALSE, unit.get("crossModel"));
        assertEquals("Settings", unit.get("perspective"), "a setting target loads from the shared Settings perspective");
        assertEquals("Name", unit.get("labelField"));
        assertEquals("itemUnit", unit.get("entityVar"), "item variables are prefixed so header names cannot collide");
        List<Map<String, Object>> unitScalars = (List<Map<String, Object>>) unit.get("scalars");
        assertFalse(unitScalars.isEmpty(), "a same-model item node names its fields - {{Unit.Name}} descends");

        Map<String, Object> product = byKey.get("Product");
        assertNotNull(product, "a cross-model item relation is fed");
        assertEquals(Boolean.TRUE, product.get("crossModel"));
        assertEquals("customers", product.get("model"));
        assertTrue(((List<Map<String, Object>>) product.get("scalars")).isEmpty(),
                "a cross-model item node dereferences no named field of the owner (dirigible #6422)");
    }

    /** A document whose whole relation graph is local needs no reflective copy helper. */
    @Test
    void feederWithoutCrossModelRelationsSkipsTheHelper() {
        IntentModel local = IntentParser.parse("""
                name: sales
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: SalesInvoiceStatus }
                  - name: SalesInvoiceItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal, required: true }
                    relations:
                      - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - name: SalesInvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                """);
        List<Map<String, Object>> feeders = PrintFeederSupport.buildPrintFeeders(local, IntentEntities.byName(local),
                IntentEntities.compositionParents(local), TestContexts.context(local));
        assertEquals(Boolean.FALSE, feeders.get(0)
                                           .get("hasCrossModel"));
    }
}
