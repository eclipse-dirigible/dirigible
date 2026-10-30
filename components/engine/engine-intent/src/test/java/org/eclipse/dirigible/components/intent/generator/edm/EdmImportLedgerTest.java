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
 * An entity a file import writes into is flagged for its {@code
 *
<TABLE>
 * _IMPORTS} ledger (#7856), and no other entity is - the flag is what makes the schema template
 * emit the ledger table.
 */
class EdmImportLedgerTest {

    private static final String YAML = """
            name: bank
            entities:
              - name: StatementLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
              - name: Account
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: iban, type: string }
            imports:
              - { name: swift, into: StatementLine, format: mt940, map: { amount: amount } }
            """;

    @Test
    @SuppressWarnings("unchecked")
    void only_the_imported_entity_carries_the_ledger_flag_and_its_write_gate_is_unchanged() {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(YAML), "bank");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");

        Map<String, Object> line = entity(entities, "StatementLine");
        assertEquals("true", line.get("importLedger"));
        assertEquals("bank.StatementLine.StatementLineFullAccess", line.get("roleWrite"),
                "the shared write-gate derivation keeps the convention role the controller checks");
        assertNull(entity(entities, "Account").get("importLedger"));
    }

    private static final String DUP_KEY_YAML = """
            name: bank
            entities:
              - name: StatementLine
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: statementId, type: string,  length: 100, required: true }
                  - { name: lineNumber,  type: integer, required: true }
                  - { name: amount,      type: decimal }
            imports:
              - name: bankCsv
                into: StatementLine
                format: csv
                map: { statementId: 1, lineNumber: 2, amount: 3 }
                duplicateKey: [statementId, lineNumber]
              - name: swift
                into: StatementLine
                format: mt940
                map: { statementId: statementId, lineNumber: lineNumber, amount: amount }
                duplicateKey: [statementId, lineNumber]
            """;

    @Test
    @SuppressWarnings("unchecked")
    void an_import_duplicate_key_becomes_one_composite_unique_constraint_on_the_target() {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(DUP_KEY_YAML), "bank");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");

        List<Map<String, Object>> constraints = (List<Map<String, Object>>) entity(entities, "StatementLine").get("uniqueConstraints");
        assertEquals(1, constraints.size(), "two imports sharing the duplicate key synthesize one constraint, not two: " + constraints);
        Map<String, Object> constraint = constraints.get(0);
        assertEquals("StatementId,LineNumber", constraint.get("properties"));
        assertEquals("STATEMENT_LINE_STATEMENT_ID,STATEMENT_LINE_LINE_NUMBER", constraint.get("columnsCsv"),
                "the key's columns match the target's own column naming, so the database constraint references real columns");
    }

    private static Map<String, Object> entity(List<Map<String, Object>> entities, String name) {
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow();
    }
}
