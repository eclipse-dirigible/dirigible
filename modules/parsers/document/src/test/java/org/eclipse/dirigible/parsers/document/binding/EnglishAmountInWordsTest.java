/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;

import org.junit.Test;

public class EnglishAmountInWordsTest {

    private final AmountInWords words = new EnglishAmountInWords();

    private String render(String amount, String currency) {
        return words.render(new BigDecimal(amount), currency);
    }

    @Test
    public void rendersTheBulgarianExampleInEnglish() {
        assertEquals("FIVE THOUSAND TWO HUNDRED SIXTY-FOUR EUROS AND 44 CENTS", render("5264.44", "EUR"));
    }

    @Test
    public void namesEachCurrencyInTheSingularAndThePlural() {
        assertEquals("ONE EURO AND 01 CENT", render("1.01", "EUR"));
        assertEquals("ONE DOLLAR AND 00 CENTS", render("1", "USD"));
        assertEquals("TWO DOLLARS AND 50 CENTS", render("2.5", "USD"));
        assertEquals("ONE POUND AND 01 PENNY", render("1.01", "GBP"));
        assertEquals("TEN POUNDS AND 99 PENCE", render("10.99", "GBP"));
        assertEquals("ONE LEV AND 01 STOTINKA", render("1.01", "BGN"));
        assertEquals("TWENTY-ONE LEVA AND 02 STOTINKI", render("21.02", "BGN"));
    }

    @Test
    public void spellsTeensTensAndHundreds() {
        assertEquals("NINETEEN EUROS AND 00 CENTS", render("19", "EUR"));
        assertEquals("NINETY EUROS AND 00 CENTS", render("90", "EUR"));
        assertEquals("ONE HUNDRED FIVE EUROS AND 00 CENTS", render("105", "EUR"));
        assertEquals("THREE HUNDRED FORTY THOUSAND FOUR HUNDRED TWELVE EUROS AND 00 CENTS", render("340412", "EUR"));
    }

    @Test
    public void spellsTheScales() {
        assertEquals("ONE THOUSAND EUROS AND 00 CENTS", render("1000", "EUR"));
        assertEquals("TWO MILLION ONE HUNDRED THOUSAND EUROS AND 00 CENTS", render("2100000", "EUR"));
        assertEquals("ONE BILLION ONE EUROS AND 00 CENTS", render("1000000001", "EUR"));
        assertEquals("NINE HUNDRED NINETY-NINE BILLION NINE HUNDRED NINETY-NINE MILLION NINE HUNDRED NINETY-NINE THOUSAND "
                + "NINE HUNDRED NINETY-NINE EUROS AND 99 CENTS", render("999999999999.99", "EUR"));
    }

    @Test
    public void roundsToCentsAndRendersZeroAndNegatives() {
        assertEquals("ZERO EUROS AND 00 CENTS", render("0", "EUR"));
        assertEquals("ONE EURO AND 00 CENTS", render("0.995", "EUR"));
        assertEquals("MINUS TEN EUROS AND 05 CENTS", render("-10.05", "EUR"));
    }

    @Test
    public void anUnsupportedCurrencyOrAnOutOfRangeAmountRendersNothing() {
        assertNull(render("1", "JPY"));
        assertNull(render("1", null));
        assertNull(render("1000000000000", "EUR"));
    }
}
