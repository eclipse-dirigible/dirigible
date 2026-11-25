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

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.regex.Pattern;

/**
 * The checks behind the intent field's named {@code format:} values that a regular expression
 * cannot express (dirigible #8302), and behind the string-stored calendar types {@code month} and
 * {@code week} (dirigible #8328). A generated controller refuses a value that fails one with
 * {@link FieldRefusals}.
 *
 * <p>
 * A value is checked as it is stored: in its electronic form, without spaces and in upper case. A
 * field that accepts the printed form declares {@code normalize: [strip: " ", upper]}, which runs
 * before the check.
 */
public final class FieldFormats {

    /**
     * The shape of an IBAN (ISO 13616) in its electronic form: a country code, two check digits and a
     * basic bank account number of 11 to 30 letters and digits. The generated form carries it as the
     * input's HTML pattern.
     */
    public static final String IBAN_PATTERN = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$";

    private static final Pattern IBAN = Pattern.compile(IBAN_PATTERN);

    private FieldFormats() {}

    /**
     * Whether a value is an IBAN: of its shape, and with check digits that hold under ISO 7064 MOD
     * 97-10 - the first four characters moved to the end and every letter read as 10 to 35, the number
     * leaves a remainder of 1 when divided by 97.
     *
     * @param value the stored value, may be null
     * @return true for an IBAN whose check digits are valid
     */
    public static boolean isIban(String value) {
        if (value == null || !IBAN.matcher(value)
                                  .matches()) {
            return false;
        }
        String rearranged = value.substring(4) + value.substring(0, 4);
        int remainder = 0;
        // Digit by digit, so a 34-character IBAN (up to 68 digits once its letters are expanded) never
        // has to be held as one number.
        for (int i = 0; i < rearranged.length(); i++) {
            char c = rearranged.charAt(i);
            remainder = Character.isDigit(c) ? (remainder * 10 + (c - '0')) % 97 : (remainder * 100 + (c - 'A' + 10)) % 97;
        }
        return remainder == 1;
    }

    /**
     * Whether a value is a calendar month as a {@code month} field stores it: {@code YYYY-MM}, a
     * four-digit year and a month from 01 to 12 - the shape an HTML month input sends.
     *
     * @param value the stored value, may be null
     * @return true for a month of that shape
     */
    public static boolean isMonth(String value) {
        if (value == null || value.length() != 7 || value.charAt(4) != '-' || !isDigits(value, 0, 4) || !isDigits(value, 5, 7)) {
            return false;
        }
        int month = Integer.parseInt(value.substring(5, 7));
        return month >= 1 && month <= 12;
    }

    /**
     * Whether a value is an ISO 8601 week as a {@code week} field stores it: {@code YYYY-Www}, a
     * four-digit week-based year and a week from 01 to 52, or 53 in a year that has a 53rd week - the
     * shape an HTML week input sends.
     *
     * @param value the stored value, may be null
     * @return true for a week of that shape that exists in its year
     */
    public static boolean isWeek(String value) {
        if (value == null || value.length() != 8 || value.charAt(4) != '-' || value.charAt(5) != 'W' || !isDigits(value, 0, 4)
                || !isDigits(value, 6, 8)) {
            return false;
        }
        int year = Integer.parseInt(value.substring(0, 4));
        int week = Integer.parseInt(value.substring(6, 8));
        // Mid-year always lies in the week-based year of the same number, so its range is that year's.
        long weeksInYear = IsoFields.WEEK_OF_WEEK_BASED_YEAR.rangeRefinedBy(LocalDate.of(year, 7, 1))
                                                            .getMaximum();
        return week >= 1 && week <= weeksInYear;
    }

    /**
     * Whether the characters of a value from {@code from} (inclusive) to {@code to} are ASCII digits.
     */
    private static boolean isDigits(String value, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
