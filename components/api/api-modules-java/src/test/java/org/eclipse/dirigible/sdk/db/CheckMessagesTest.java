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
}
