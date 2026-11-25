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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * dirigible #8302: {@code format: iban} checks the ISO 7064 MOD 97-10 check digits, not only the
 * shape. dirigible #8328: a {@code month} field holds a real {@code YYYY-MM}, a {@code week} field
 * a real ISO {@code YYYY-Www}.
 */
class FieldFormatsTest {

    @ParameterizedTest
    @ValueSource(strings = {"BG80BNBG96611020345678", "GB82WEST12345698765432", "DE89370400440532013000", "NO9386011117947",
            "MT84MALT011000012345MTLCAST001S"})
    void anIbanWithValidCheckDigitsIsAccepted(String iban) {
        assertTrue(FieldFormats.isIban(iban));
    }

    /** The account of #8302 with check digits that can never be valid - 00 is outside 02..98. */
    @Test
    void anIbanWhoseCheckDigitsFailIsRefused() {
        assertFalse(FieldFormats.isIban("BG00BNBG96611020345678"));
        assertFalse(FieldFormats.isIban("BG81BNBG96611020345678"));
        assertFalse(FieldFormats.isIban("GB82WEST12345698765433"));
    }

    /** The value is checked as stored: the printed form is the job of {@code normalize:}. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"BG80 BNBG 9661 1020 3456 78", "bg80bnbg96611020345678", "BG8BNBG96611020345678", "8080BNBG96611020345678",
            "BG80BNBG9661", "BG80BNBG96611020345678-", "BG80BNBG966110203456789012345678901"})
    void aValueOffTheShapeIsRefused(String value) {
        assertFalse(FieldFormats.isIban(value));
    }

    /**
     * The longest shape there is - 30 letters and digits after the check digits, 68 digits once the
     * letters are expanded - against check digits computed with arbitrary precision: the digit by digit
     * remainder agrees with the whole number's.
     */
    @Test
    void theLongestIbanIsCheckedPieceByPiece() {
        String bban = "ZZ9A8B7C6D5E4F3G2H1I0JKLMNOPQR";
        String checkDigits = String.format("%02d", 98 - expand(bban + "XK00").mod(BigInteger.valueOf(97))
                                                                             .intValue());
        String iban = "XK" + checkDigits + bban;

        assertEquals(34, iban.length());
        assertTrue(FieldFormats.isIban(iban));
        assertFalse(FieldFormats.isIban("XK" + (checkDigits.equals("02") ? "03" : "02") + bban));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-05", "2026-01", "2026-12", "0001-06", "9999-12"})
    void aMonthOfTheFormYearDashMonthIsAccepted(String month) {
        assertTrue(FieldFormats.isMonth(month));
    }

    /** The values of #8328 - one calendar month spelled three ways, a thirteenth month, and junk. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2026-5", "202605", "abc", "2026-13", "2026-00", "2026/05", "26-05", " 2026-05", "2026-05 ", "2026-05-01",
            "２０２６-05", "2026-W05"})
    void aValueThatIsNoMonthIsRefused(String value) {
        assertFalse(FieldFormats.isMonth(value));
    }

    /**
     * 2026 and 2020 have a 53rd ISO week (a year starting on a Thursday, a leap year on a Wednesday).
     */
    @ParameterizedTest
    @ValueSource(strings = {"2026-W01", "2026-W22", "2025-W52", "2026-W53", "2020-W53", "2015-W53"})
    void anIsoWeekOfTheFormYearDashWeekIsAccepted(String week) {
        assertTrue(FieldFormats.isWeek(week));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2026-W5", "2026W05", "2026-w05", "2026-W00", "2026-W54", "2025-W53", "2024-W53", "abc", "2026-05", "2026-W05 ",
            "2026-W5a"})
    void aValueThatIsNoIsoWeekIsRefused(String value) {
        assertFalse(FieldFormats.isWeek(value));
    }

    private static BigInteger expand(String value) {
        StringBuilder digits = new StringBuilder();
        for (char c : value.toCharArray()) {
            digits.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
        }
        return new BigInteger(digits.toString());
    }
}
