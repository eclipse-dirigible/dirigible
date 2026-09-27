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
 * "Сума словом": the whole amount in Bulgarian words, the cents as two digits, both nouns spelled
 * out, in capitals - {@code ПЕТ ХИЛЯДИ ДВЕСТА ШЕСТДЕСЕТ И ЧЕТИРИ ЕВРО И 44 ЕВРОЦЕНТА}.
 *
 * <p>
 * The grammar it follows: a one or a two agrees in gender with the noun it counts (едно евро, един
 * лев, две хиляди, два милиона), a single thousand is the bare {@code хиляда}, and {@code и} stands
 * before the last word of every group of three digits (сто двадесет и пет) and before the last
 * group when that group is one word (две хиляди и сто). Only the currencies it can name are
 * rendered - the euro and the lev; any other renders nothing, as the other systems do.
 */
final class BulgarianAmountInWords implements AmountInWords {

    private static final Locale BULGARIAN = Locale.forLanguageTag("bg");

    private static final BigDecimal LIMIT = new BigDecimal("1000000000000");

    private static final String[] MASCULINE = {"нула", "един", "два", "три", "четири", "пет", "шест", "седем", "осем", "девет"};

    private static final String[] TEENS = {"десет", "единадесет", "дванадесет", "тринадесет", "четиринадесет", "петнадесет", "шестнадесет",
            "седемнадесет", "осемнадесет", "деветнадесет"};

    private static final String[] TENS =
            {"", "", "двадесет", "тридесет", "четиридесет", "петдесет", "шестдесет", "седемдесет", "осемдесет", "деветдесет"};

    private static final String[] HUNDREDS =
            {"", "сто", "двеста", "триста", "четиристотин", "петстотин", "шестстотин", "седемстотин", "осемстотин", "деветстотин"};

    private enum Gender {
        MASCULINE, FEMININE, NEUTER
    }

    /** A currency as Bulgarian names it: the main unit and its hundredth, each singular and plural. */
    private record Currency(Gender gender, String one, String many, String centOne, String centMany) {
    }

    private static final Currency EURO = new Currency(Gender.NEUTER, "евро", "евро", "евроцент", "евроцента");

    private static final Currency LEV = new Currency(Gender.MASCULINE, "лев", "лева", "стотинка", "стотинки");

    /** One scale step of the whole amount: its power of a thousand and how it is named. */
    private record Scale(long unit, Gender gender, String one, String many) {
    }

    private static final Scale[] SCALES = {new Scale(1_000_000_000L, Gender.MASCULINE, "милиард", "милиарда"),
            new Scale(1_000_000L, Gender.MASCULINE, "милион", "милиона"), new Scale(1_000L, Gender.FEMININE, "хиляда", "хиляди")};

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
            text.append("минус ");
        }
        text.append(spell(whole, currency.gender()))
            .append(' ')
            .append(whole == 1 ? currency.one() : currency.many())
            .append(" и ")
            .append(String.format(Locale.ROOT, "%02d", cents))
            .append(' ')
            .append(cents == 1 ? currency.centOne() : currency.centMany());
        return text.toString()
                   .toUpperCase(BULGARIAN);
    }

    private static Currency currency(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim()
                           .toUpperCase(Locale.ROOT)) {
            case "EUR" -> EURO;
            case "BGN" -> LEV;
            default -> null;
        };
    }

    /** The whole amount in words, the units agreeing with the currency noun. */
    private static String spell(long whole, Gender unitsGender) {
        if (whole == 0) {
            return MASCULINE[0];
        }
        List<Group> groups = new ArrayList<>();
        long rest = whole;
        for (Scale scale : SCALES) {
            int count = (int) (rest / scale.unit());
            rest %= scale.unit();
            if (count == 0) {
                continue;
            }
            if (count == 1 && scale.gender() == Gender.FEMININE) {
                groups.add(new Group(List.of(scale.one()), true));
            } else {
                List<String> counted = group(count, scale.gender());
                List<String> words = new ArrayList<>(counted);
                words.add(count == 1 ? scale.one() : scale.many());
                groups.add(new Group(words, counted.size() == 1));
            }
        }
        if (rest > 0) {
            List<String> counted = group((int) rest, unitsGender);
            groups.add(new Group(counted, counted.size() == 1));
        }
        List<String> words = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            if (i > 0 && i == groups.size() - 1 && group.oneWord()) {
                words.add("и");
            }
            words.addAll(group.words());
        }
        return String.join(" ", words);
    }

    /** One group of the whole amount, its scale noun included; one-word when it counts in one word. */
    private record Group(List<String> words, boolean oneWord) {
    }

    /** One group of three digits (1-999), with {@code и} before its last word. */
    private static List<String> group(int value, Gender gender) {
        List<String> words = new ArrayList<>();
        int hundreds = value / 100;
        int tens = value % 100 / 10;
        int units = value % 10;
        if (hundreds > 0) {
            words.add(HUNDREDS[hundreds]);
        }
        if (tens == 1) {
            words.add(TEENS[units]);
        } else {
            if (tens > 1) {
                words.add(TENS[tens]);
            }
            if (units > 0) {
                words.add(unit(units, gender));
            }
        }
        if (words.size() > 1) {
            words.add(words.size() - 1, "и");
        }
        return words;
    }

    private static String unit(int digit, Gender gender) {
        return switch (gender) {
            case FEMININE -> digit == 1 ? "една" : digit == 2 ? "две" : MASCULINE[digit];
            case NEUTER -> digit == 1 ? "едно" : digit == 2 ? "две" : MASCULINE[digit];
            case MASCULINE -> MASCULINE[digit];
        };
    }
}
