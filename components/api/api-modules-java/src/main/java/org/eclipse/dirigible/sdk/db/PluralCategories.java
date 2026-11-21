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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;

/**
 * The CLDR cardinal plural category of a whole count in a language - the {@code _one} /
 * {@code _other} / ... suffix i18next picks in the browser, so that a catalog key reads the same on
 * both sides (dirigible #8777).
 *
 * <p>
 * The JDK has no plural API, and a count of records is always a whole number, so this is the CLDR
 * rule of an integer for the languages a Dirigible application is translated into - not a general
 * implementation. A language outside the table has no category, and its catalog is read by the
 * plain key.
 */
final class PluralCategories {

    /** CLDR {@code one}. */
    static final String ONE = "one";

    /** CLDR {@code few}. */
    static final String FEW = "few";

    /** CLDR {@code many}. */
    static final String MANY = "many";

    /** CLDR {@code other}, the category every language has. */
    static final String OTHER = "other";

    /** {@code one} for exactly 1 (English, Bulgarian, German, ...). */
    private static final LongFunction<String> ONE_IF_ONE = n -> n == 1 ? ONE : OTHER;

    /** {@code one} for 0 and 1 (French, Portuguese). */
    private static final LongFunction<String> ONE_IF_ZERO_OR_ONE = n -> n == 0 || n == 1 ? ONE : OTHER;

    /** Russian, Ukrainian, Belarusian: 1/21/31 one, 2-4/22-24 few, the rest many. */
    private static final LongFunction<String> EAST_SLAVIC = n -> {
        long mod10 = n % 10;
        long mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) {
            return ONE;
        }
        return mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) ? FEW : MANY;
    };

    /** Polish: 1 one, 2-4/22-24 few, the rest many. */
    private static final LongFunction<String> POLISH = n -> {
        if (n == 1) {
            return ONE;
        }
        long mod10 = n % 10;
        long mod100 = n % 100;
        return mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) ? FEW : MANY;
    };

    /** Czech, Slovak: 1 one, 2-4 few, the rest other (many is for fractions only). */
    private static final LongFunction<String> CZECH = n -> {
        if (n == 1) {
            return ONE;
        }
        return n >= 2 && n <= 4 ? FEW : OTHER;
    };

    /** Languages without a grammatical number: always {@code other}. */
    private static final LongFunction<String> ALWAYS_OTHER = n -> OTHER;

    private static final Map<String, LongFunction<String>> RULES = rules();

    private PluralCategories() {}

    /**
     * The plural category of a whole count in a language.
     *
     * @param language a language tag ({@code bg}, {@code bg-BG}, {@code pt_BR})
     * @param count the count
     * @return the CLDR category, or {@code null} for a language outside the table
     */
    static String of(String language, long count) {
        if (language == null) {
            return null;
        }
        String primary = language.trim()
                                 .replace('_', '-');
        int dash = primary.indexOf('-');
        if (dash > 0) {
            primary = primary.substring(0, dash);
        }
        LongFunction<String> rule = RULES.get(primary.toLowerCase(Locale.ROOT));
        return rule == null ? null : rule.apply(Math.abs(count));
    }

    private static Map<String, LongFunction<String>> rules() {
        Map<String, LongFunction<String>> rules = new HashMap<>();
        for (String language : Set.of("en", "bg", "de", "nl", "sv", "da", "nb", "nn", "no", "it", "es", "ca", "el", "fi", "et", "hu",
                "tr")) {
            rules.put(language, ONE_IF_ONE);
        }
        rules.put("fr", ONE_IF_ZERO_OR_ONE);
        rules.put("pt", ONE_IF_ZERO_OR_ONE);
        for (String language : Set.of("ru", "uk", "be")) {
            rules.put(language, EAST_SLAVIC);
        }
        rules.put("pl", POLISH);
        rules.put("cs", CZECH);
        rules.put("sk", CZECH);
        for (String language : Set.of("ja", "zh", "ko", "vi", "th", "id")) {
            rules.put(language, ALWAYS_OTHER);
        }
        return Map.copyOf(rules);
    }
}
