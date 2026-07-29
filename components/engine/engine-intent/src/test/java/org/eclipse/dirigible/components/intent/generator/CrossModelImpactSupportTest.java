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
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * dirigible #6422: when a model stops declaring a member, the projects that reference it
 * cross-model are the regeneration set - and the owner's own pass is the only place that can name
 * them, because the consumers are not regenerated.
 */
class CrossModelImpactSupportTest {

    private static final String BEFORE = """
            {"model":{"entities":[
              {"name":"SalesInvoice","properties":[{"name":"Id"},{"name":"Number"},{"name":"CustomerEmail"}]},
              {"name":"SalesInvoiceItem","properties":[{"name":"Id"},{"name":"Quantity"}]}
            ]}}
            """;

    private static final String AFTER_FIELD_REMOVED = """
            {"model":{"entities":[
              {"name":"SalesInvoice","properties":[{"name":"Id"},{"name":"Number"}]},
              {"name":"SalesInvoiceItem","properties":[{"name":"Id"},{"name":"Quantity"}]}
            ]}}
            """;

    private static final String AFTER_ENTITY_REMOVED = """
            {"model":{"entities":[
              {"name":"SalesInvoiceItem","properties":[{"name":"Id"},{"name":"Quantity"}]}
            ]}}
            """;

    /** A consumer: uses the owner model AND relates to the affected entity. */
    private static final String JOURNAL = """
            name: journal
            uses:
              - { model: sales_invoices }
            entities:
              - name: JournalEntry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, model: sales_invoices }
            """;

    /** Uses the owner model, but only for a DIFFERENT entity - untouched by this removal. */
    private static final String PAYMENTS = """
            name: payments
            uses:
              - { model: sales_invoices }
            entities:
              - name: Allocation
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Item, kind: manyToOne, to: SalesInvoiceItem, model: sales_invoices }
            """;

    /** Does not depend on the owner at all. */
    private static final String UNRELATED = """
            name: countries
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private static final Map<String, String> CANDIDATES = Map.of("journal", JOURNAL, "payments", PAYMENTS, "countries", UNRELATED);

    @Test
    void aDroppedPropertyIsReportedAgainstItsEntity() {
        List<CrossModelImpactSupport.Removal> removals = CrossModelImpactSupport.removals(CrossModelImpactSupport.parseShape(BEFORE),
                CrossModelImpactSupport.parseShape(AFTER_FIELD_REMOVED));

        assertEquals(List.of(new CrossModelImpactSupport.Removal("SalesInvoice", "CustomerEmail")), removals);
        assertEquals("[SalesInvoice.CustomerEmail]", removals.get(0)
                                                             .describe());
    }

    /** A vanished entity is reported once - not once per property it took with it. */
    @Test
    void aDroppedEntityIsReportedOnce() {
        List<CrossModelImpactSupport.Removal> removals = CrossModelImpactSupport.removals(CrossModelImpactSupport.parseShape(BEFORE),
                CrossModelImpactSupport.parseShape(AFTER_ENTITY_REMOVED));

        assertEquals(List.of(new CrossModelImpactSupport.Removal("SalesInvoice", null)), removals);
        assertEquals("entity [SalesInvoice]", removals.get(0)
                                                      .describe());
    }

    @Test
    void addingAPropertyRemovesNothing() {
        assertTrue(CrossModelImpactSupport
                                          .removals(CrossModelImpactSupport.parseShape(AFTER_FIELD_REMOVED),
                                                  CrossModelImpactSupport.parseShape(BEFORE))
                                          .isEmpty());
    }

    /** A first-ever generation has no previous shape, so it can remove nothing. */
    @Test
    void anAbsentPreviousShapeIsEmpty() {
        assertTrue(CrossModelImpactSupport.parseShape("")
                                          .isEmpty());
        assertTrue(CrossModelImpactSupport.parseShape("{}")
                                          .isEmpty());
    }

    @Test
    void theShapeIsEntityToPropertyNames() {
        Map<String, Set<String>> shape = CrossModelImpactSupport.parseShape(BEFORE);
        assertEquals(Set.of("Id", "Number", "CustomerEmail"), shape.get("SalesInvoice"));
        assertEquals(Set.of("Id", "Quantity"), shape.get("SalesInvoiceItem"));
    }

    /**
     * Only the projects whose generated code actually dereferences the affected entity are named: a
     * project that uses the owner for a different entity is not in the regeneration set, and neither is
     * one that does not use it at all.
     */
    @Test
    void onlyTheProjectsRelatingToTheAffectedEntityAreNamed() {
        assertEquals(List.of("journal"), CrossModelImpactSupport.consumers("sales_invoices", "sales_invoices", "SalesInvoice", CANDIDATES));
        assertEquals(List.of("payments"),
                CrossModelImpactSupport.consumers("sales_invoices", "sales_invoices", "SalesInvoiceItem", CANDIDATES));
        assertTrue(CrossModelImpactSupport.consumers("sales_invoices", "sales_invoices", "Nowhere", CANDIDATES)
                                          .isEmpty());
    }

    /**
     * The alias alone is not enough - a {@code uses:} entry resolves to a model IN A PROJECT, and a
     * same-named model in another project is a different owner.
     */
    @Test
    void aSameNamedModelInAnotherProjectIsNotAConsumer() {
        assertTrue(CrossModelImpactSupport.consumers("sales_invoices", "some-other-project", "SalesInvoice", CANDIDATES)
                                          .isEmpty());
    }

    /** An unparseable neighbour is skipped, never allowed to fail the owner's pass. */
    @Test
    void anUnparseableCandidateIsSkipped() {
        Map<String, String> candidates = Map.of("broken", "\tthis: is: not: yaml", "journal", JOURNAL);

        assertEquals(List.of("journal"), CrossModelImpactSupport.consumers("sales_invoices", "sales_invoices", "SalesInvoice", candidates));
    }
}
