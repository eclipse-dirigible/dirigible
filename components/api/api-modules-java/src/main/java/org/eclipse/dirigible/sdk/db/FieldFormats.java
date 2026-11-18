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

import java.util.regex.Pattern;

/**
 * The checks behind the intent field's named {@code format:} values that a regular expression
 * cannot express (dirigible #8302). A generated controller refuses a value that fails one with
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
}
