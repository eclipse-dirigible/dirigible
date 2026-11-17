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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * {@code patternMessage:} (dirigible #7749): what a value off a field's {@code pattern} is refused
 * with, instead of the generic sentence that quotes the regex. Accepted beside a pattern; refused
 * blank or without a pattern.
 */
class FieldPatternMessageIntentTest {

    private static String customer(String field) {
        return """
                name: customers
                entities:
                  - name: Customer
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 80, required: true }
                      - %s
                """.formatted(field);
    }

    @Test
    void aPatternCarriesItsMessage() {
        IntentModel model = IntentParser.parse(customer(
                "{ name: registrationNumber, type: string, length: 13, pattern: '^([0-9]{9}|[0-9]{10}|[0-9]{13})$', patternMessage: 'EIK: 9, 10 or 13 digits' }"));
        FieldIntent field = fieldOf(model, "registrationNumber");
        assertEquals("^([0-9]{9}|[0-9]{10}|[0-9]{13})$", field.getPattern());
        assertEquals("EIK: 9, 10 or 13 digits", field.getPatternMessage());
    }

    @Test
    void aPatternWithoutAMessageIsUnchanged() {
        IntentModel model = IntentParser.parse(customer("{ name: code, type: string, length: 3, pattern: '^[A-Z]{3}$' }"));
        assertNull(fieldOf(model, "code").getPatternMessage());
    }

    @Test
    void aBlankPatternMessageIsRefused() {
        assertIssue(customer("{ name: code, type: string, length: 3, pattern: '^[A-Z]{3}$', patternMessage: '   ' }"),
                "entity [Customer] field [code] `patternMessage` is blank - remove it to keep the generic pattern message");
    }

    @Test
    void aPatternMessageWithoutAPatternIsRefused() {
        assertIssue(customer("{ name: code, type: string, length: 3, patternMessage: Three capital letters }"),
                "entity [Customer] field [code] declares `patternMessage` without a `pattern`");
    }

    @Test
    void aPatternMessageBesideAFormatIsRefused() {
        assertIssue(customer("{ name: email, type: string, length: 120, format: email, patternMessage: An e-mail address }"),
                "entity [Customer] field [email] declares `patternMessage` without a `pattern`");
    }

    private static FieldIntent fieldOf(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(e -> "Customer".equals(e.getName()))
                    .findFirst()
                    .orElseThrow()
                    .getFields()
                    .stream()
                    .filter(f -> name.equals(f.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "expected an issue containing [" + fragment + "], got: " + ex.getIssues());
    }
}
