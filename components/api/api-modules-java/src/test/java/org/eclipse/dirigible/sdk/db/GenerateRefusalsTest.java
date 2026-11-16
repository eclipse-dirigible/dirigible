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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * dirigible #8646: a create-from's refusals are answered in the language of the request - the
 * sentence from the intent's catalog, the entities and the action by their labels there - and fall
 * back to the default-language text piece by piece.
 */
class GenerateRefusalsTest {

    private static final String CATALOG = "hotel:hotel-model";
    private static final GenerateRefusals.Label STAY = new GenerateRefusals.Label("Stay", CATALOG + ".t.HOTEL_STAY");
    private static final GenerateRefusals.Label NIGHT = new GenerateRefusals.Label("Stay Night", CATALOG + ".t.HOTEL_STAY_NIGHT");
    private static final GenerateRefusals.Label BILL = new GenerateRefusals.Label("Bill", CATALOG + ".t.HOTEL_BILL");
    private static final GenerateRefusals.Label ACTION = new GenerateRefusals.Label("Bill the stay", CATALOG + ".actions.bill-from-stay");
    private static final String REFUSE_KEY = CATALOG + ".checks.billFromStayItemsRefused";

    private static final Map<String, String> BG = Map.of( //
            CATALOG + ".checks.generateNoItemQualifies",
            "Нито един ред „{item}“ на „{entity}“ не отговаря за „{action}“ - „{target}“ би останал без редове", //
            CATALOG + ".checks.generateNothingQualifies", "Нито един ред на справка {report} не отговаря за „{action}“", //
            CATALOG + ".checks.generateFromItem", "{message} (от {item} [{id}])", //
            REFUSE_KEY, "Нощувката няма сума", //
            CATALOG + ".t.HOTEL_STAY", "Престой", //
            CATALOG + ".t.HOTEL_STAY_NIGHT", "Нощувка", //
            CATALOG + ".t.HOTEL_BILL", "Сметка", //
            CATALOG + ".actions.bill-from-stay", "Таксувай престоя");

    private static final CheckMessages.CatalogReader CATALOGS = (language, key) -> "bg".equals(language) ? BG.get(key) : null;

    @Test
    void noQualifyingItemNamesTheEntitiesAndTheActionByTheirLabels() {
        ValidationException refusal = GenerateRefusals.noItemQualifies(null, CATALOGS, CATALOG, NIGHT, STAY, ACTION, BILL);

        assertEquals("no Stay Night row of the Stay qualifies for Bill the stay, so the Bill would have no lines", refusal.getMessage());
        assertEquals(CATALOG + ".checks.generateNoItemQualifies", refusal.getMessageKey());
        assertNull(refusal.getField());
    }

    @Test
    void noQualifyingItemIsTranslatedWithTheTranslatedLabels() {
        ValidationException refusal = GenerateRefusals.noItemQualifies("bg", CATALOGS, CATALOG, NIGHT, STAY, ACTION, BILL);

        assertEquals("Нито един ред „Нощувка“ на „Престой“ не отговаря за „Таксувай престоя“ - „Сметка“ би останал без редове",
                refusal.getMessage());
        assertEquals(Map.of("item", "Нощувка", "entity", "Престой", "action", "Таксувай престоя", "target", "Сметка"),
                refusal.getMessageParams());
    }

    @Test
    void nothingQualifiesNamesTheReportAndTheAction() {
        assertEquals("no row of report Arrivals qualifies for Bill the stay, so there is nothing to generate",
                GenerateRefusals.nothingQualifies(null, CATALOGS, CATALOG, "Arrivals", ACTION)
                                .getMessage());
        assertEquals("Нито един ред на справка Arrivals не отговаря за „Таксувай престоя“",
                GenerateRefusals.nothingQualifies("bg", CATALOGS, CATALOG, "Arrivals", ACTION)
                                .getMessage());
    }

    /** A key the language's catalog does not translate keeps its default-language text. */
    @Test
    void anUntranslatedSentenceFallsBackToTheDefaultTextWithTranslatedLabels() {
        ValidationException refusal = GenerateRefusals.noReportRowQualifies("bg", CATALOGS, CATALOG, "Arrivals", ACTION, BILL);

        assertEquals("no row of report Arrivals qualifies for Таксувай престоя, so the Сметка would have no lines", refusal.getMessage());
        assertEquals(CATALOG + ".checks.generateNoReportRowQualifies", refusal.getMessageKey());
        ValidationException missing = GenerateRefusals.reportValueMissing("bg", CATALOGS, CATALOG, STAY, 7, "Period", "Arrivals", "period");
        assertEquals("Престой [7] has no Period, which report Arrivals is run with as period", missing.getMessage());
    }

    /** The author's sentence is translated under its own key; the refused rows follow it. */
    @Test
    void theAuthoredRefusalIsTranslatedAndNamesTheRows() {
        List<Object> rows = List.of(3, 5);
        assertEquals("Stay night carries no \"amount\" (Stay Night [3, 5])",
                GenerateRefusals.itemsRefused(null, CATALOGS, REFUSE_KEY, "Stay night carries no \"amount\"", NIGHT, rows)
                                .getMessage());
        ValidationException refusal =
                GenerateRefusals.itemsRefused("bg", CATALOGS, REFUSE_KEY, "Stay night carries no \"amount\"", NIGHT, rows);
        assertEquals("Нощувката няма сума (Нощувка [3, 5])", refusal.getMessage());
        assertEquals(REFUSE_KEY, refusal.getMessageKey());
    }

    /**
     * The target's own refusal stays the cause, and its key and field are the ones the body carries.
     */
    @Test
    void aRefusedTargetLineNamesItsSourceRowAndKeepsItsOwnKey() {
        ValidationException line = new ValidationException("Полето 'Сума' е задължително", "hotel:hotel-model.checks.fieldRequired",
                Map.of("label", "Сума"), "Amount");

        ValidationException refusal = GenerateRefusals.fromItem("bg", CATALOGS, CATALOG, line, NIGHT, 4);

        assertEquals("Полето 'Сума' е задължително (от Нощувка [4])", refusal.getMessage());
        assertSame(line, refusal.getCause());
        assertEquals("hotel:hotel-model.checks.fieldRequired", refusal.getMessageKey());
        assertEquals("Amount", refusal.getField());
        assertEquals("Полето 'Сума' е задължително (from Stay Night [4])",
                GenerateRefusals.fromItem(null, CATALOGS, CATALOG, line, NIGHT, 4)
                                .getMessage());
    }

    @Test
    void aReportValueTheTargetCannotHoldIsRefusedInTheReadersLanguage() {
        assertEquals("report column [Amount] holds [1E+20], too large to be read exactly",
                GenerateRefusals.reportValueTooLarge(null, CATALOGS, CATALOG, "Amount", "1E+20")
                                .getMessage());
        assertEquals("Колона [Amount] на справката съдържа [1E+20] - твърде голяма стойност",
                GenerateRefusals.reportValueTooLarge("bg",
                        (language, key) -> key.endsWith("generateReportValueTooLarge")
                                ? "Колона [{column}] на справката съдържа [{value}] - твърде голяма стойност"
                                : null,
                        CATALOG, "Amount", "1E+20")
                                .getMessage());
        assertEquals("report column [Count] holds [2.5], which is not a whole number",
                GenerateRefusals.reportValueNotWhole("bg", CATALOGS, CATALOG, "Count", "2.5")
                                .getMessage());
        ValidationException rounding = GenerateRefusals.reportValueRounding("bg", CATALOGS, CATALOG, "Amount", "1.005", 2);
        assertEquals("report column [Amount] holds [1.005], which a field of 2 decimal places cannot hold without rounding",
                rounding.getMessage());
        assertEquals(CATALOG + ".checks.generateReportValueRounding", rounding.getMessageKey());
    }

    /** A create-from generated without a catalog refuses in the default text, with no key. */
    @Test
    void withoutACatalogTheRefusalCarriesNoKey() {
        ValidationException refusal =
                GenerateRefusals.noItemQualifies("bg", CATALOGS, null, new GenerateRefusals.Label("Stay Night", null), STAY, ACTION, BILL);

        assertEquals("no Stay Night row of the Престой qualifies for Таксувай престоя, so the Сметка would have no lines",
                refusal.getMessage());
        assertNull(refusal.getMessageKey());
    }
}
