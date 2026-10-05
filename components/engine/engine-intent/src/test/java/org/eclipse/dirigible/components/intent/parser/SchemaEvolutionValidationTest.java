/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The expand/contract declarations of an entity's table (#7635) - {@code dropped:} and a field's
 * {@code renamedFrom:} - are refused when they would drop or move a column the entity still
 * declares.
 */
class SchemaEvolutionValidationTest {

    @Test
    void aRenameAndADropOfFormerNamesAreAccepted() {
        assertDoesNotThrow(() -> IntentParser.parse(invoice("dropped: [oldNote]", "renamedFrom: invoiceDate")));
    }

    @Test
    void aDroppedNameTheEntityStillDeclaresIsRefused() {
        assertIssue(invoice("dropped: [total]", null), "lists [total] under `dropped` but still declares [total]");
    }

    @Test
    void aDroppedRelationIsComparedByItsColumnToo() {
        assertIssue(invoice("dropped: [Customer]", null), "lists [Customer] under `dropped` but still declares [customer]");
    }

    @Test
    void aFieldRenamedFromItsOwnNameIsRefused() {
        assertIssue(invoice(null, "renamedFrom: issueDate"), "field [issueDate] is `renamedFrom` its own name");
    }

    @Test
    void aRenameFromADeclaredFieldIsRefused() {
        assertIssue(invoice(null, "renamedFrom: total"), "which the entity still declares as [total]");
    }

    @Test
    void aRenameFromADroppedNameIsRefused() {
        assertIssue(invoice("dropped: [invoiceDate]", "renamedFrom: invoiceDate"), "which is also listed under `dropped`");
    }

    @Test
    void twoFieldsRenamedFromOneNameAreRefused() {
        String yaml = """
                name: billing
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: issueDate, type: date, renamedFrom: invoiceDate }
                      - { name: dueDate, type: date, renamedFrom: invoiceDate }
                """;
        assertIssue(yaml, "only one field can take over its column");
    }

    private static String invoice(String dropped, String renamedFrom) {
        return """
                name: billing
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Invoice
                    %s
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: issueDate, type: date%s }
                      - { name: total, type: decimal }
                    relations:
                      - { name: customer, kind: manyToOne, to: Customer }
                """.formatted(dropped == null ? "" : dropped, renamedFrom == null ? "" : ", " + renamedFrom);
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(fragment)),
                "expected an issue containing [" + fragment + "], got " + ex.getIssues());
    }
}
