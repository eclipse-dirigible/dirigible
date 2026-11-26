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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.api.platform.RepositoryFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.sdk.security.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * How a check message is resolved for the request's language (issue #7611): the language's catalog,
 * else the default text, with the placeholders interpolated after translation.
 */
class CheckMessagesTest {

    private static final String KEY = "billing:billing-model.checks.Invoice_zeroLines";
    private static final String TEXT = "{count} line(s) at price zero";
    private static final CheckMessages.CatalogReader NO_CATALOG = (language, key) -> null;
    private static final String BG_CATALOG = "{\"billing-model\":{\"checks\":{\"Invoice_zeroLines\":\"Нулеви редове: {{count}}\"}}}";

    @AfterEach
    void clearLanguage() {
        User.clearLanguage();
    }

    /**
     * Work that runs without a request - a notification send, a process step, an event's handler -
     * binds the language of the user it serves, and a catalog message resolved there is in it (#8776).
     */
    @Test
    void without_a_request_the_bound_language_resolves_the_catalog_text() {
        User.setLanguage("bg");
        try (MockedStatic<RepositoryFacade> registry = registryWithBgCatalog()) {
            assertEquals("Нулеви редове: 2", CheckMessages.message(KEY, TEXT, "count", 2));
            assertEquals("Нулеви редове: 2", CheckMessages.refusal(KEY, TEXT, "count", 2)
                                                          .getMessage());
        }
    }

    /**
     * Without a request and without a bound language nobody reads the message in a language of their
     * own: the default text, and the facade is not asked - it would log its no-request error on every
     * refusal.
     */
    @Test
    void without_a_request_or_a_bound_language_the_default_text_is_resolved_silently() {
        Logger facadeLogger = (Logger) LoggerFactory.getLogger(UserFacade.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        facadeLogger.addAppender(logged);
        try (MockedStatic<RepositoryFacade> registry = registryWithBgCatalog()) {
            assertEquals("2 line(s) at price zero", CheckMessages.message(KEY, TEXT, "count", 2));
        } finally {
            facadeLogger.detachAppender(logged);
        }
        assertTrue(logged.list.stream()
                              .noneMatch(event -> event.getLevel()
                                                       .isGreaterOrEqual(Level.ERROR)),
                () -> "the facade logged " + logged.list);
    }

    /** The registry of the {@code billing} project: its bg-BG catalog translates {@link #KEY}. */
    private static MockedStatic<RepositoryFacade> registryWithBgCatalog() {
        IResource catalog = mock(IResource.class);
        when(catalog.getContent()).thenReturn(BG_CATALOG.getBytes(StandardCharsets.UTF_8));
        ICollection bg = mock(ICollection.class);
        when(bg.getResourcesNames()).thenReturn(List.of("billing.model.json"));
        when(bg.getResource("billing.model.json")).thenReturn(catalog);
        ICollection i18n = mock(ICollection.class);
        when(i18n.exists()).thenReturn(true);
        when(i18n.getCollectionsNames()).thenReturn(List.of("en-US", "bg-BG"));
        when(i18n.getCollection("bg-BG")).thenReturn(bg);
        MockedStatic<RepositoryFacade> registry = mockStatic(RepositoryFacade.class);
        registry.when(() -> RepositoryFacade.getCollection(anyString()))
                .thenReturn(i18n);
        return registry;
    }

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
}
