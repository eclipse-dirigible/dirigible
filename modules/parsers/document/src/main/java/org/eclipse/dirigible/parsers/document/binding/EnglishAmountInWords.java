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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The amount in English words, in the shape of the Bulgarian one: the whole amount in words, the
 * cents as two digits, both nouns spelled out, in capitals -
 * {@code FIVE THOUSAND TWO HUNDRED SIXTY-FOUR EUROS AND 44 CENTS}.
 *
 * <p>
 * Tens and units are hyphenated (sixty-four), the scales are the short ones (thousand, million,
 * billion), and the number itself carries no {@code and} - the one {@code and} joins the cents, so
 * it can never be read as part of the amount. It names the euro, the lev, the US dollar and the
 * pound sterling; any other currency renders nothing.
 */
final class EnglishAmountInWords implements AmountInWords {

    private static final BigDecimal LIMIT = new BigDecimal("1000000000000");

    private static final String[] UNDER_TWENTY = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};

    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};

    private static final long[] SCALE_UNITS = {1_000_000_000L, 1_000_000L, 1_000L};

    private static final String[] SCALE_NAMES = {"billion", "million", "thousand"};

    /** A currency as English names it: the main unit and its hundredth, each singular and plural. */
    private record Currency(String one, String many, String centOne, String centMany) {
    }

    private static final Currency EURO = new Currency("euro", "euros", "cent", "cents");

    private static final Currency LEV = new Currency("lev", "leva", "stotinka", "stotinki");

    private static final Currency DOLLAR = new Currency("dollar", "dollars", "cent", "cents");

    private static final Currency POUND = new Currency("pound", "pounds", "penny", "pence");

    @Override
    public String render(BigDecimal amount, String currencyCode) {
        Currency currency = currency(currencyCode);
        if (currency == null) {
            return null;
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal magnitude = rounded.abs();
        if (magnitude.compareTo(LIMIT) >= 0) {
            return null;
        }
        long whole = magnitude.longValue();
        int cents = magnitude.remainder(BigDecimal.ONE)
                             .movePointRight(2)
                             .intValue();
        StringBuilder text = new StringBuilder();
        if (rounded.signum() < 0) {
            text.append("minus ");
        }
        text.append(spell(whole))
            .append(' ')
            .append(whole == 1 ? currency.one() : currency.many())
            .append(" and ")
            .append(String.format(Locale.ROOT, "%02d", cents))
            .append(' ')
            .append(cents == 1 ? currency.centOne() : currency.centMany());
        return text.toString()
                   .toUpperCase(Locale.ENGLISH);
    }

    private static Currency currency(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim()
                           .toUpperCase(Locale.ROOT)) {
            case "EUR" -> EURO;
            case "BGN" -> LEV;
            case "USD" -> DOLLAR;
            case "GBP" -> POUND;
            default -> null;
        };
    }

    /** The whole amount in words. */
    private static String spell(long whole) {
        if (whole == 0) {
            return UNDER_TWENTY[0];
        }
        List<String> words = new ArrayList<>();
        long rest = whole;
        for (int i = 0; i < SCALE_UNITS.length; i++) {
            int count = (int) (rest / SCALE_UNITS[i]);
            rest %= SCALE_UNITS[i];
            if (count > 0) {
                words.add(group(count));
                words.add(SCALE_NAMES[i]);
            }
        }
        if (rest > 0) {
            words.add(group((int) rest));
        }
        return String.join(" ", words);
    }

    /** One group of three digits (1-999). */
    private static String group(int value) {
        List<String> words = new ArrayList<>();
        int hundreds = value / 100;
        int belowHundred = value % 100;
        if (hundreds > 0) {
            words.add(UNDER_TWENTY[hundreds]);
            words.add("hundred");
        }
        if (belowHundred >= 20) {
            int units = belowHundred % 10;
            words.add(units == 0 ? TENS[belowHundred / 10] : TENS[belowHundred / 10] + "-" + UNDER_TWENTY[units]);
        } else if (belowHundred > 0) {
            words.add(UNDER_TWENTY[belowHundred]);
        }
        return String.join(" ", words);
    }
}
