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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/**
 * How a check message is resolved for the request's language (issue #7611): the language's catalog,
 * else the default text, with the placeholders interpolated after translation.
 */
class CheckMessagesTest {

    private static final String KEY = "billing:billing-model.checks.Invoice_zeroLines";
    private static final String TEXT = "{count} line(s) at price zero";
    private static final CheckMessages.CatalogReader NO_CATALOG = (language, key) -> null;

    @Test
    void the_language_catalog_translates_the_default_text() {
        CheckMessages.CatalogReader catalogs =
                (language, key) -> "bg".equals(language) && KEY.equals(key) ? "Нулеви редове: {{count}}" : null;
        assertEquals("Нулеви редове: 2", CheckMessages.resolve("bg", catalogs, KEY, TEXT, Map.of("count", 2)));
        // The reader is handed the request language as sent; the folder match (bg -> bg-BG) is its own.
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("bg-BG", catalogs, KEY, TEXT, Map.of("count", 2)));
    }

    @Test
    void an_untranslated_language_falls_back_to_the_default_text() {
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("de", NO_CATALOG, KEY, TEXT, Map.of("count", 2)));
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("", NO_CATALOG, KEY, TEXT, Map.of("count", 2)));
        assertEquals("2 line(s) at price zero", CheckMessages.resolve(null, NO_CATALOG, KEY, TEXT, Map.of("count", 2)));
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("*", NO_CATALOG, KEY, TEXT, Map.of("count", 2)));
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("bg", NO_CATALOG, null, TEXT, Map.of("count", 2)));
    }

    @Test
    void a_failing_catalog_never_fails_the_refusal() {
        CheckMessages.CatalogReader broken = (language, key) -> {
            throw new IllegalStateException("registry down");
        };
        assertEquals("2 line(s) at price zero", CheckMessages.resolve("bg", broken, KEY, TEXT, Map.of("count", 2)));
    }

    @Test
    void placeholders_are_interpolated_in_both_spellings_and_unknown_ones_are_kept() {
        assertEquals("Acme and 3 and {other}",
                CheckMessages.interpolate("{match} and {{ count }} and {other}", Map.of("match", "Acme", "count", 3)));
        assertEquals("$1 \\ literal", CheckMessages.interpolate("{value} literal", Map.of("value", "$1 \\")));
    }

    /** A message may name a value of the record one hop away (#8665). */
    @Test
    void a_one_hop_placeholder_is_interpolated() {
        assertEquals("max 20% for 2.1 (and {Other.code})", CheckMessages.interpolate(
                "max {taxRateCap}% for {Category.code} (and {Other.code})", Map.of("taxRateCap", 20, "Category.code", "2.1")));
    }

    /** A value of the record that is empty renders as nothing, not the text "null" (#8747). */
    @Test
    void a_present_but_null_placeholder_renders_empty() {
        Map<String, Object> params = new HashMap<>();
        params.put("Category.code", null);
        params.put("count", null);
        assertEquals("refused for category [] and [] and {other}",
                CheckMessages.interpolate("refused for category [{Category.code}] and [{{ count }}] and {other}", params));
        ValidationException refusal = CheckMessages.refusal(KEY, "for category {Category.code}", "Category.code", null);
        assertEquals("for category ", refusal.getMessage());
        assertTrue(refusal.getMessageParams()
                          .containsKey("Category.code"));
        assertNull(refusal.getMessageParams()
                          .get("Category.code"));
    }

    @Test
    void the_alternating_arguments_become_ordered_parameters() {
        assertEquals(Map.of("count", 2, "match", "Acme"), CheckMessages.params("count", 2, "match", "Acme"));
        assertEquals(Map.of(), CheckMessages.params());
    }

    @Test
    void a_refusal_carries_the_key_and_the_parameters() {
        ValidationException refusal = CheckMessages.refusal(KEY, TEXT, "count", 2);
        assertEquals("2 line(s) at price zero", refusal.getMessage());
        assertEquals(KEY, refusal.getMessageKey());
        assertEquals(Map.of("count", 2), refusal.getMessageParams());
        Warning warning = CheckMessages.warning("Invoice.itemsCompare.0", KEY, TEXT, "count", 2);
        assertEquals("Invoice.itemsCompare.0", warning.code());
        assertEquals(KEY, warning.messageKey());
        assertEquals(Map.of("count", 2), warning.params());
        Warning literal = new Warning("code", "text");
        assertNull(literal.messageKey());
        assertEquals(Map.of(), literal.params());
    }

    @Test
    void the_locale_folder_matches_exactly_or_by_primary_subtag() {
        List<String> folders = List.of("en-US", "bg-BG");
        assertEquals("bg-BG", CheckMessages.localeFolder(folders, "bg"));
        assertEquals("bg-BG", CheckMessages.localeFolder(folders, "BG-bg"));
        assertEquals("en-US", CheckMessages.localeFolder(folders, "en-GB"));
        assertNull(CheckMessages.localeFolder(folders, "de"));
    }

    @Test
    void a_catalog_path_is_walked_through_the_nested_objects() {
        String catalog = "{\"billing-model\":{\"checks\":{\"Invoice_zeroLines\":\"x\"},\"t\":{}}}";
        assertEquals("x", CheckMessages.lookup(JsonParser.parseString(catalog), "billing-model.checks.Invoice_zeroLines".split("\\.")));
        assertNull(CheckMessages.lookup(JsonParser.parseString(catalog), "billing-model.checks.Missing".split("\\.")));
        assertNull(CheckMessages.lookup(JsonParser.parseString(catalog), "billing-model.checks".split("\\.")));
    }

    /** dirigible #8777: the count picks the plural form the language's CLDR rule gives. */
    @Test
    void the_count_picks_the_plural_form_of_the_language() {
        Map<String, String> catalog = Map.of( //
                "bg:" + KEY + "_one", "{count} ред с нулева цена", //
                "bg:" + KEY + "_other", "{count} реда с нулева цена", //
                "en:" + KEY + "_one", "{count} line at price zero", //
                "en:" + KEY + "_other", "{count} lines at price zero");
        CheckMessages.CatalogReader catalogs = (language, key) -> catalog.get(language + ":" + key);

        assertEquals("1 ред с нулева цена", CheckMessages.resolve("bg", catalogs, KEY, TEXT, Map.of("count", 1)));
        assertEquals("5 реда с нулева цена", CheckMessages.resolve("bg", catalogs, KEY, TEXT, Map.of("count", 5)));
        assertEquals("1 line at price zero", CheckMessages.resolve("en", catalogs, KEY, TEXT, Map.of("count", 1)));
        assertEquals("5 lines at price zero", CheckMessages.resolve("en", catalogs, KEY, TEXT, Map.of("count", 5L)));
        assertEquals("0 lines at price zero", CheckMessages.resolve("en", catalogs, KEY, TEXT, Map.of("count", 0)));
    }

    /** A catalog without plural forms - one written before #8777 - is read by its plain key. */
    @Test
    void a_missing_plural_form_falls_back_to_the_plain_key() {
        Map<String, String> catalog = Map.of( //
                "bg:" + KEY, "Нулеви редове: {count}", //
                "ru:" + KEY + "_one", "{count} строка");
        CheckMessages.CatalogReader catalogs = (language, key) -> catalog.get(language + ":" + key);

        assertEquals("Нулеви редове: 5", CheckMessages.resolve("bg", catalogs, KEY, TEXT, Map.of("count", 5)));
        // ru 5 is "many", which this catalog lacks, and it has no plain entry: the default text answers.
        assertEquals("5 line(s) at price zero", CheckMessages.resolve("ru", catalogs, KEY, TEXT, Map.of("count", 5)));
        assertEquals("21 строка", CheckMessages.resolve("ru", catalogs, KEY, TEXT, Map.of("count", 21)));
        // A count that is not a whole number picks no form.
        assertEquals("Нулеви редове: 1.5", CheckMessages.resolve("bg", catalogs, KEY, TEXT, Map.of("count", 1.5)));
    }

    /** The default text has plural forms of its own, picked by the English rule. */
    @Test
    void the_default_text_takes_its_plural_form_too() {
        Map<String, String> texts = Map.of("one", "{count} line", "other", "{count} lines");
        assertEquals("1 line", CheckMessages.resolve(null, NO_CATALOG, KEY, texts, Map.of("count", 1)));
        assertEquals("5 lines", CheckMessages.resolve("bg", NO_CATALOG, KEY, texts, Map.of("count", 5)));
        assertEquals("? lines", CheckMessages.resolve(null, NO_CATALOG, KEY, texts, Map.of("count", "?")));
    }

    /** dirigible #8777: a label inside a sentence opts into lower case, as the browser's formatter. */
    @Test
    void a_label_opting_into_lowercase_is_lower_cased_but_an_acronym_stays() {
        Map<String, Object> params = Map.of("entity", "Валута", "label", "Ставка ДДС", "code", "Код");
        assertEquals("Вече съществува валута с тази ставка ДДС, Код",
                CheckMessages.interpolate("Вече съществува {entity, lowercase} с тази {{ label , lowercase }}, {code}", params, "bg"));
        assertEquals("a VAT rate", CheckMessages.lowercase("A VAT Rate", "en"));
        // An unknown format leaves the value as it is; an unknown name with a format is kept.
        assertEquals("Валута {missing, lowercase}", CheckMessages.interpolate("{entity, upper} {missing, lowercase}", params, "bg"));
    }

    @Test
    void the_lowercase_rule_mirrors_the_browser() {
        assertEquals("валута", CheckMessages.lowercase("Валута", "bg"));
        assertEquals("ДДС", CheckMessages.lowercase("ДДС", "bg"));
        assertEquals("ставка ДДС", CheckMessages.lowercase("Ставка ДДС", "bg"));
        // A single capital is a word, not an acronym.
        assertEquals("a", CheckMessages.lowercase("A", "en"));
        assertEquals("ırmak", CheckMessages.lowercase("Irmak", "tr"));
    }
}
