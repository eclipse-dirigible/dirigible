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
 * shape.
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

    private static BigInteger expand(String value) {
        StringBuilder digits = new StringBuilder();
        for (char c : value.toCharArray()) {
            digits.append(Character.isDigit(c) ? String.valueOf(c) : String.valueOf(c - 'A' + 10));
        }
        return new BigInteger(digits.toString());
    }
}
