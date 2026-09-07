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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code stampOn: issue} numbering descriptor carries the partition relation's {@code init:} as
 * {@code perDefault} (#7101), so the generated stamp delegate resolves a null FK to the default
 * company's partition exactly as the create-time allocator does - never to the series' base row.
 */
class NumberingInitPartitionTest {

    private static final String PURCHASE = """
            name: purchase
            entities:
              - name: Company
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: PurchaseInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Purchase Invoice, per: Company, stampOn: issue } }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, init: 1 }
            """;

    @Test
    void theDescriptorCarriesTheRelationsInitAsThePartitionFallback() {
        Map<String, Object> descriptor = onlyDescriptor(PURCHASE);
        assertEquals("Company", descriptor.get("per"));
        assertEquals("1", descriptor.get("perDefault"));
    }

    @Test
    void noInitMeansNoFallback() {
        Map<String, Object> descriptor = onlyDescriptor(PURCHASE.replace(", init: 1", ""));
        assertEquals("Company", descriptor.get("per"));
        assertEquals("", descriptor.get("perDefault"), "an empty marker, like an unpartitioned per - the binder copies it verbatim");
    }

    @Test
    void anUnpartitionedSeriesCarriesNeither() {
        Map<String, Object> descriptor = onlyDescriptor(PURCHASE.replace("per: Company, ", ""));
        assertEquals("", descriptor.get("per"));
        assertEquals("", descriptor.get("perDefault"));
    }

    private static Map<String, Object> onlyDescriptor(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        List<Map<String, Object>> numbering = NumberingSupport.buildNumbering(model, IntentEntities.compositionParents(model));
        assertEquals(1, numbering.size(), "exactly one stampOn: issue number: " + numbering);
        return numbering.get(0);
    }
}
