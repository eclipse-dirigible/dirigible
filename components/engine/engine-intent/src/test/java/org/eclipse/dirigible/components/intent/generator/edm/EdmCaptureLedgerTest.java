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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * An entity a document capture creates records of is flagged for its {@code
 *
<TABLE>
 * _CAPTURES} ledger (#8544), and no other entity is - the flag is what makes the schema template
 * emit the ledger table.
 */
class EdmCaptureLedgerTest {

    private static final String YAML = """
            name: purchasing
            entities:
              - name: PurchaseInvoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
              - name: Supplier
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            captures:
              - { name: supplierInvoice, into: PurchaseInvoice, fields: [{ field: number }] }
            """;

    @Test
    @SuppressWarnings("unchecked")
    void only_the_captured_entity_carries_the_ledger_flag() {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(YAML), "purchasing");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");

        assertEquals("true", entity(entities, "PurchaseInvoice").get("captureLedger"));
        assertNull(entity(entities, "Supplier").get("captureLedger"));
        assertNull(entity(entities, "PurchaseInvoice").get("importLedger"), "the two ledgers are independent");
    }

    private static Map<String, Object> entity(List<Map<String, Object>> entities, String name) {
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow();
    }
}
