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

public class BulgarianAmountInWordsTest {

    private final AmountInWords words = new BulgarianAmountInWords();

    private String render(String amount, String currency) {
        return words.render(new BigDecimal(amount), currency);
    }

    @Test
    public void rendersTheReviewerConfirmedExample() {
        assertEquals("ПЕТ ХИЛЯДИ ДВЕСТА ШЕСТДЕСЕТ И ЧЕТИРИ ЕВРО И 44 ЕВРОЦЕНТА", render("5264.44", "EUR"));
    }

    @Test
    public void agreesInGenderWithTheCurrencyNoun() {
        assertEquals("ЕДНО ЕВРО И 00 ЕВРОЦЕНТА", render("1", "EUR"));
        assertEquals("ДВЕ ЕВРО И 01 ЕВРОЦЕНТ", render("2.01", "EUR"));
        assertEquals("ЕДИН ЛЕВ И 01 СТОТИНКА", render("1.01", "BGN"));
        assertEquals("ДВА ЛЕВА И 50 СТОТИНКИ", render("2.5", "BGN"));
        assertEquals("ДВАДЕСЕТ И ЕДИН ЛЕВА И 00 СТОТИНКИ", render("21", "BGN"));
    }

    @Test
    public void thousandsAreFeminineAndASingleThousandIsBare() {
        assertEquals("ХИЛЯДА ЕВРО И 00 ЕВРОЦЕНТА", render("1000", "EUR"));
        assertEquals("ДВЕ ХИЛЯДИ ЕВРО И 00 ЕВРОЦЕНТА", render("2000", "EUR"));
        assertEquals("ДВАДЕСЕТ И ЕДНА ХИЛЯДИ ЛЕВА И 00 СТОТИНКИ", render("21000", "BGN"));
        assertEquals("ДВА МИЛИОНА ДВАДЕСЕТ И ДВЕ ХИЛЯДИ ЛЕВА И 00 СТОТИНКИ", render("2022000", "BGN"));
    }

    @Test
    public void placesTheConjunctionBeforeTheLastNumberWord() {
        assertEquals("СТО И ПЕТ ЕВРО И 00 ЕВРОЦЕНТА", render("105", "EUR"));
        assertEquals("СТО ДВАДЕСЕТ И ПЕТ ЕВРО И 00 ЕВРОЦЕНТА", render("125", "EUR"));
        assertEquals("СТО И ПЕТНАДЕСЕТ ЕВРО И 00 ЕВРОЦЕНТА", render("115", "EUR"));
        assertEquals("ДВЕ ХИЛЯДИ И СТО ЕВРО И 00 ЕВРОЦЕНТА", render("2100", "EUR"));
        assertEquals("ХИЛЯДА И ЕДНО ЕВРО И 00 ЕВРОЦЕНТА", render("1001", "EUR"));
        assertEquals("ЕДИН МИЛИОН И ХИЛЯДА ЕВРО И 00 ЕВРОЦЕНТА", render("1001000", "EUR"));
        assertEquals("ДВА МИЛИОНА И СТО ХИЛЯДИ ЛЕВА И 00 СТОТИНКИ", render("2100000", "BGN"));
    }

    @Test
    public void spellsHundredsAndLargeScales() {
        assertEquals(
                "ДЕВЕТСТОТИН ДЕВЕТДЕСЕТ И ДЕВЕТ МИЛИАРДА ДЕВЕТСТОТИН ДЕВЕТДЕСЕТ И ДЕВЕТ МИЛИОНА "
                        + "ДЕВЕТСТОТИН ДЕВЕТДЕСЕТ И ДЕВЕТ ХИЛЯДИ ДЕВЕТСТОТИН ДЕВЕТДЕСЕТ И ДЕВЕТ ЕВРО И 99 ЕВРОЦЕНТА",
                render("999999999999.99", "EUR"));
        assertEquals("ЕДИН МИЛИАРД ЕВРО И 00 ЕВРОЦЕНТА", render("1000000000", "EUR"));
        assertEquals("ТРИСТА И ЧЕТИРИДЕСЕТ ХИЛЯДИ ЧЕТИРИСТОТИН И ДВАНАДЕСЕТ ЕВРО И 00 ЕВРОЦЕНТА", render("340412", "EUR"));
    }

    @Test
    public void roundsToCentsAndRendersZeroAndNegatives() {
        assertEquals("НУЛА ЕВРО И 00 ЕВРОЦЕНТА", render("0", "EUR"));
        assertEquals("ЕДНО ЕВРО И 00 ЕВРОЦЕНТА", render("0.999", "EUR"));
        assertEquals("МИНУС ДЕСЕТ ЕВРО И 05 ЕВРОЦЕНТА", render("-10.05", "EUR"));
    }

    @Test
    public void acceptsTheCurrencyCodeInAnyCase() {
        assertEquals("ЕДНО ЕВРО И 00 ЕВРОЦЕНТА", render("1", " eur "));
    }

    @Test
    public void anUnsupportedCurrencyOrAnOutOfRangeAmountRendersNothing() {
        assertNull(render("1", "USD"));
        assertNull(render("1", null));
        assertNull(render("1000000000000", "EUR"));
    }
}
