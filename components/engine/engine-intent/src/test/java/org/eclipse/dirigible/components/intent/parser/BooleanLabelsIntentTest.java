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
 * A boolean field's own words for its two values (#8564): {@code labels: { true: X, false: Y }},
 * accepted on a boolean field with both words, refused anywhere else at parse time.
 */
class BooleanLabelsIntentTest {

    private static final String CATALOG = """
            name: catalog
            entities:
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: active
                    type: boolean
                    labels: { true: Active, false: Inactive }
                  - { name: archived, type: boolean }
            """;

    @Test
    void aBooleanFieldCarriesItsWords() {
        FieldIntent field = field(IntentParser.parse(CATALOG), "active");

        assertEquals("Active", field.getLabels()
                                    .get("true"));
        assertEquals("Inactive", field.getLabels()
                                      .get("false"));
    }

    @Test
    void aBooleanFieldWithoutWordsCarriesNone() {
        assertNull(field(IntentParser.parse(CATALOG), "archived").getLabels());
    }

    @Test
    void aQuotedYamlBooleanWordIsKeptAsWritten() {
        FieldIntent field = field(
                IntentParser.parse(CATALOG.replace("{ true: Active, false: Inactive }", "{ true: \"Yes\", false: \"No\" }")), "active");

        assertEquals("Yes", field.getLabels()
                                 .get("true"));
    }

    @Test
    void wordsOnANonBooleanFieldAreRefused() {
        assertRefused(CATALOG.replace("type: boolean\n        labels", "type: string\n        labels"),
                "field [active] declares `labels` - they name a boolean's two values, so they apply to a boolean field only");
    }

    @Test
    void aMissingFalseWordIsRefused() {
        assertRefused(CATALOG.replace("{ true: Active, false: Inactive }", "{ true: Active }"), "`labels` has no word for [false]");
    }

    @Test
    void aMissingTrueWordIsRefused() {
        assertRefused(CATALOG.replace("{ true: Active, false: Inactive }", "{ false: Inactive }"), "`labels` has no word for [true]");
    }

    @Test
    void aKeyOtherThanTrueOrFalseIsRefused() {
        assertRefused(CATALOG.replace("{ true: Active, false: Inactive }", "{ true: Active, false: Inactive, maybe: Unknown }"),
                "unknown key [maybe]");
    }

    @Test
    void anUnquotedYamlBooleanWordIsRefused() {
        assertRefused(CATALOG.replace("{ true: Active, false: Inactive }", "{ true: Yes, false: No }"),
                "`labels` word for [true] reads as a YAML boolean - quote it");
    }

    private static FieldIntent field(IntentModel model, String name) {
        return model.getEntities()
                    .get(0)
                    .getFields()
                    .stream()
                    .filter(field -> name.equals(field.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertRefused(String yaml, String expected) {
        IntentValidationException exception = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(exception.getMessage()
                            .contains(expected),
                () -> "expected an issue containing [" + expected + "] but got: " + exception.getMessage());
    }
}
