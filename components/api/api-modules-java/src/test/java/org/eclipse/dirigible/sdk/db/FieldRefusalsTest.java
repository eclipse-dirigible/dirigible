/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * dirigible #8596: a required, too long, off-pattern or repeated unique value is refused in the
 * language of the request - the sentence from the entity's catalog, the field and the entity by
 * their translated labels - with the key, the parameters and the field it is about.
 */
class FieldRefusalsTest {

    private static final String CATALOG = "uoms:uoms-model";
    private static final String UOM_LABEL_KEY = CATALOG + ".t.UOM";
    private static final String SAP_LABEL_KEY = CATALOG + ".t.UOM_SAP";

    private static final Map<String, String> BG = Map.of( //
            CATALOG + ".checks.fieldRequired", "Полето '{label}' е задължително", //
            CATALOG + ".checks.fieldTooLong", "Полето '{label}' е по-дълго от {length} знака", //
            CATALOG + ".checks.fieldDuplicate", "Вече съществува {entity} с това '{label}'", //
            UOM_LABEL_KEY, "мерна единица", //
            SAP_LABEL_KEY, "САП код");

    private static final CheckMessages.CatalogReader CATALOGS = (language, key) -> "bg".equals(language) ? BG.get(key) : null;

    @Test
    void aRequiredValueIsRefusedInTheDefaultLanguageWithTheFieldsLabel() {
        ValidationException refusal = FieldRefusals.refusal(null, CATALOGS, CATALOG, FieldRefusals.REQUIRED_KEY,
                FieldRefusals.REQUIRED_TEXT, "Sap", "SAP Code", SAP_LABEL_KEY);

        assertEquals("The 'SAP Code' property is required", refusal.getMessage());
        assertEquals(CATALOG + ".checks.fieldRequired", refusal.getMessageKey());
        assertEquals(Map.of("label", "SAP Code"), refusal.getMessageParams());
        assertEquals("Sap", refusal.getField());
    }

    @Test
    void aTooLongValueIsRefusedInTheReadersLanguage() {
        ValidationException refusal = FieldRefusals.refusal("bg", CATALOGS, CATALOG, FieldRefusals.TOO_LONG_KEY,
                FieldRefusals.TOO_LONG_TEXT, "Sap", "SAP Code", SAP_LABEL_KEY, "length", 20);

        assertEquals("Полето 'САП код' е по-дълго от 20 знака", refusal.getMessage());
        assertEquals("Sap", refusal.getField());
    }

    @Test
    void aRepeatedUniqueValueNamesTheEntityAndTheFieldByTheirTranslatedLabels() {
        ValidationException refusal =
                FieldRefusals.duplicate("bg", CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Sap", "SAP Code", SAP_LABEL_KEY);

        assertEquals("Вече съществува мерна единица с това 'САП код'", refusal.getMessage());
        assertEquals(CATALOG + ".checks.fieldDuplicate", refusal.getMessageKey());
        assertEquals("Sap", refusal.getField());
        assertEquals("A UoM with this 'SAP Code' already exists",
                FieldRefusals.duplicate(null, CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Sap", "SAP Code", SAP_LABEL_KEY)
                             .getMessage());
    }

    /** A sentence the language's catalog does not carry keeps its default-language text. */
    @Test
    void anUntranslatedSentenceFallsBackToTheDefaultText() {
        ValidationException refusal = FieldRefusals.refusal("bg", CATALOGS, CATALOG, FieldRefusals.PATTERN_KEY, FieldRefusals.PATTERN_TEXT,
                "Sap", "SAP Code", SAP_LABEL_KEY, "pattern", "^[A-Z]{3}$");

        assertEquals("The value of 'САП код' does not match the required pattern '^[A-Z]{3}$'", refusal.getMessage());
    }

    /** A controller generated without a catalog refuses in the default text, still naming the field. */
    @Test
    void withoutACatalogTheRefusalCarriesNoKeyButItsField() {
        ValidationException refusal = FieldRefusals.refusal("bg", CATALOGS, null, FieldRefusals.REQUIRED_KEY, FieldRefusals.REQUIRED_TEXT,
                "Sap", "SAP Code", null);

        assertEquals("The 'SAP Code' property is required", refusal.getMessage());
        assertNull(refusal.getMessageKey());
        assertEquals("Sap", refusal.getField());
    }
}
