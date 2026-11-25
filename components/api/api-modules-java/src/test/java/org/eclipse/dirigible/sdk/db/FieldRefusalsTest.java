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
    private static final String BASE_LABEL_KEY = CATALOG + ".t.UOM_BASE";
    private static final String COMPANY_LABEL_KEY = CATALOG + ".t.UOM_COMPANY";

    private static final Map<String, String> BG = Map.ofEntries( //
            Map.entry(CATALOG + ".checks.fieldRequired", "Полето '{label}' е задължително"), //
            Map.entry(CATALOG + ".checks.fieldTooLong", "Полето '{label}' е по-дълго от {length} знака"), //
            Map.entry(CATALOG + ".checks.fieldDuplicate", "Вече съществува {entity} с това '{label}'"), //
            Map.entry(CATALOG + ".checks.fieldIban", "Стойността на '{label}' не е валиден IBAN"), //
            Map.entry(CATALOG + ".checks.UoM_pattern_Sap", "{label}: три главни латински букви"), //
            Map.entry(CATALOG + ".checks.fieldSingleRequired",
                    "Поне една {entity} трябва да е '{label}' - първо отбележете друга като '{label}'"), //
            Map.entry(CATALOG + ".checks.fieldSingleTaken", "Друга {entity} вече е '{label}'"), //
            Map.entry(CATALOG + ".checks.fieldSingleTakenInScope", "Друга {entity} вече е '{label}' за тази {scope}"), //
            Map.entry(BASE_LABEL_KEY, "Основна"), //
            Map.entry(UOM_LABEL_KEY, "мерна единица"), //
            Map.entry(SAP_LABEL_KEY, "САП код"), //
            Map.entry(COMPANY_LABEL_KEY, "фирма"));

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

    /**
     * dirigible #8700: a required single flag left without a holder, named by the translated labels.
     */
    @Test
    void aRequiredSingleFlagLeftWithoutAHolderNamesTheEntityAndTheFlagByTheirTranslatedLabels() {
        ValidationException refusal =
                FieldRefusals.singleRequired("bg", CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base", BASE_LABEL_KEY);

        assertEquals("Поне една мерна единица трябва да е 'Основна' - първо отбележете друга като 'Основна'", refusal.getMessage());
        assertEquals(CATALOG + ".checks.fieldSingleRequired", refusal.getMessageKey());
        assertEquals("Base", refusal.getField());
        assertEquals("One UoM must stay 'Base' - mark another one 'Base' first",
                FieldRefusals.singleRequired(null, CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base", BASE_LABEL_KEY)
                             .getMessage());
    }

    /**
     * dirigible #8101: a refusing single flag another record holds, named by the translated labels -
     * fleet-wide, and within the scope of the relation the flag is scoped by.
     */
    @Test
    void aTakenSingleFlagNamesTheEntityTheFlagAndItsScopeByTheirTranslatedLabels() {
        ValidationException refusal =
                FieldRefusals.singleTaken("bg", CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base", BASE_LABEL_KEY);

        assertEquals("Друга мерна единица вече е 'Основна'", refusal.getMessage());
        assertEquals(CATALOG + ".checks.fieldSingleTaken", refusal.getMessageKey());
        assertEquals("Base", refusal.getField());
        assertEquals("Another UoM is already 'Base'",
                FieldRefusals.singleTaken(null, CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base", BASE_LABEL_KEY)
                             .getMessage());

        ValidationException scoped = FieldRefusals.singleTakenInScope("bg", CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base",
                BASE_LABEL_KEY, "Company", COMPANY_LABEL_KEY);

        assertEquals("Друга мерна единица вече е 'Основна' за тази фирма", scoped.getMessage());
        assertEquals(CATALOG + ".checks.fieldSingleTakenInScope", scoped.getMessageKey());
        assertEquals("Base", scoped.getField());
        assertEquals("Another UoM is already 'Base' for this Company",
                FieldRefusals.singleTakenInScope(null, CATALOGS, CATALOG, "UoM", UOM_LABEL_KEY, "Base", "Base", BASE_LABEL_KEY, "Company",
                        COMPANY_LABEL_KEY)
                             .getMessage());
    }

    /** dirigible #8302: a value of a {@code format: iban} field that is no IBAN names the field. */
    @Test
    void aValueThatIsNoIbanNamesTheFieldByItsTranslatedLabel() {
        ValidationException refusal = FieldRefusals.refusal("bg", CATALOGS, CATALOG, FieldRefusals.IBAN_KEY, FieldRefusals.IBAN_TEXT, "Sap",
                "SAP Code", SAP_LABEL_KEY);

        assertEquals("Стойността на 'САП код' не е валиден IBAN", refusal.getMessage());
        assertEquals(CATALOG + ".checks.fieldIban", refusal.getMessageKey());
        assertEquals("Sap", refusal.getField());
        assertEquals("The value of 'SAP Code' is not a valid IBAN",
                FieldRefusals.refusal(null, CATALOGS, CATALOG, FieldRefusals.IBAN_KEY, FieldRefusals.IBAN_TEXT, "Sap", "SAP Code",
                        SAP_LABEL_KEY)
                             .getMessage());
    }

    /**
     * dirigible #8328: a value of a {@code month} or {@code week} field off its shape names the field.
     */
    @Test
    void aValueThatIsNoMonthOrWeekNamesTheField() {
        ValidationException month =
                FieldRefusals.refusal(null, CATALOGS, CATALOG, FieldRefusals.MONTH_KEY, FieldRefusals.MONTH_TEXT, "Period", "Period", null);
        ValidationException week =
                FieldRefusals.refusal(null, CATALOGS, CATALOG, FieldRefusals.WEEK_KEY, FieldRefusals.WEEK_TEXT, "Sprint", "Sprint", null);

        assertEquals("The value of 'Period' is not a month of the form YYYY-MM", month.getMessage());
        assertEquals(CATALOG + ".checks.fieldMonth", month.getMessageKey());
        assertEquals("Period", month.getField());
        assertEquals("The value of 'Sprint' is not a week of the form YYYY-Www", week.getMessage());
        assertEquals(CATALOG + ".checks.fieldWeek", week.getMessageKey());
        assertEquals("Sprint", week.getField());
    }

    /** A sentence the language's catalog does not carry keeps its default-language text. */
    @Test
    void anUntranslatedSentenceFallsBackToTheDefaultText() {
        ValidationException refusal = FieldRefusals.refusal("bg", CATALOGS, CATALOG, FieldRefusals.PATTERN_KEY, FieldRefusals.PATTERN_TEXT,
                "Sap", "SAP Code", SAP_LABEL_KEY, "pattern", "^[A-Z]{3}$");

        assertEquals("The value of 'САП код' does not match the required pattern '^[A-Z]{3}$'", refusal.getMessage());
    }

    /**
     * dirigible #7749: a pattern with the author's message is refused with that message, in the
     * reader's language, instead of the generic sentence quoting the regex.
     */
    @Test
    void anAuthoredPatternMessageIsRefusedInTheReadersLanguage() {
        ValidationException refusal = FieldRefusals.authoredPattern("bg", CATALOGS, CATALOG + ".checks.UoM_pattern_Sap",
                "{label}: three capital Latin letters", "Sap", "SAP Code", SAP_LABEL_KEY, "^[A-Z]{3}$");

        assertEquals("САП код: три главни латински букви", refusal.getMessage());
        assertEquals(CATALOG + ".checks.UoM_pattern_Sap", refusal.getMessageKey());
        assertEquals(Map.of("label", "САП код", "pattern", "^[A-Z]{3}$"), refusal.getMessageParams());
        assertEquals("Sap", refusal.getField());
    }

    @Test
    void anAuthoredPatternMessageKeepsItsDefaultTextWhereItIsNotTranslated() {
        ValidationException refusal = FieldRefusals.authoredPattern(null, CATALOGS, CATALOG + ".checks.UoM_pattern_Sap",
                "{label}: three capital Latin letters", "Sap", "SAP Code", SAP_LABEL_KEY, "^[A-Z]{3}$");

        assertEquals("SAP Code: three capital Latin letters", refusal.getMessage());
        assertEquals("Sap", refusal.getField());
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
