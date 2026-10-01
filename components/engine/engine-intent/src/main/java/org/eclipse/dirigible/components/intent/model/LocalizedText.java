/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads an authored user-facing text that may be written in one language or in several (issue
 * #7611): a check's or a pickable rule's {@code message:} is either a plain string - the message in
 * the module's default language, exactly as before - or a map of {@code languages:} codes to the
 * message in that language ({@code message: { en: "...", bg: "..." }}).
 *
 * <p>
 * The default-language value is what every surface that knows one text renders (the generated en-US
 * catalog, the baked fallback): {@code en} when the map carries it, otherwise its first entry. The
 * other entries are the per-language values the generated code resolves for the request's language
 * when the language's own catalog does not translate the message.
 */
public final class LocalizedText {

    /** The language the default-language value is taken from when a map carries it. */
    public static final String DEFAULT_LANGUAGE = "en";

    private LocalizedText() {}

    /**
     * The default-language value of an authored text.
     *
     * @param authored a string, a language map, or {@code null}
     * @return the text, or {@code null} when nothing usable is authored
     */
    public static String defaultText(Object authored) {
        if (authored == null) {
            return null;
        }
        if (authored instanceof Map<?, ?> map) {
            Object preferred = map.get(DEFAULT_LANGUAGE);
            if (preferred instanceof String text && !text.isBlank()) {
                return text;
            }
            for (Object value : map.values()) {
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
            return null;
        }
        return authored instanceof String text ? text : String.valueOf(authored);
    }

    /**
     * The per-language values of an authored text - empty for a plain string.
     *
     * @param authored a string, a language map, or {@code null}
     * @return language code to text, in authored order; never {@code null}
     */
    public static Map<String, String> translations(Object authored) {
        if (!(authored instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<String, String> translations = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() instanceof String text && !text.isBlank()) {
                translations.put(String.valueOf(entry.getKey())
                                       .trim(),
                        text);
            }
        }
        return translations;
    }

    /**
     * Whether an authored text is neither a string nor a map of strings - the shape the parser refuses.
     *
     * @param authored the authored value
     * @return {@code true} for a malformed value
     */
    public static boolean malformed(Object authored) {
        if (authored == null || authored instanceof String) {
            return false;
        }
        if (authored instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (!(value instanceof String)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
