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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.api.http.HttpRequestFacade;
import org.eclipse.dirigible.components.api.platform.RegistryFacade;
import org.eclipse.dirigible.components.api.platform.RepositoryFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolves a generated {@code checks:} message for the language of the request (issue #7611) - the
 * refusal a generated controller or repository throws, and the warning it asks the person saving to
 * confirm.
 *
 * <p>
 * A check message takes part in the module's i18n exactly like a label: the generator writes its
 * default-language text into the module's en-US catalog ({@code i18n/en-US/<model>.model.json})
 * under {@code <catalog prefix>.checks.<key>}, and a language catalog of the module
 * ({@code i18n/bg-BG/<model>.model.json}, or any other catalog file in that language's folder)
 * translates it under the same key. The text is resolved in this order:
 * <ol>
 * <li>the request language's own catalog - an overlay a translator maintains;</li>
 * <li>the default-language text, the one the model authors.</li>
 * </ol>
 * Placeholders ({@code {count}}, {@code {match}}, a value of the record such as
 * {@code {taxRateCap}} or {@code {Category.code}} (#8665), and their i18next spelling
 * {@code {{count}}}) are interpolated AFTER translation, so a translation may move them.
 *
 * <p>
 * Two pieces of i18next's grammar work here as they do in the browser (#8777), so a catalog entry
 * reads the same on both sides:
 * <ul>
 * <li><b>plural forms</b> - when the parameters carry a whole-number {@code count}, the catalog's
 * {@code <key>_one} / {@code _few} / {@code _many} / {@code _other} entry the language's CLDR rule
 * picks wins over the plain {@code <key>}, which stays the fallback, so a catalog without suffixed
 * entries keeps working;</li>
 * <li><b>{@code {name, lowercase}}</b> (and {@code {{name, lowercase}}}) - a label that sits inside
 * a sentence, lower-cased word by word except a word in capitals (an acronym such as ДДС), the rule
 * of the browser's {@code lowercase} formatter (#8133). Only the catalog phrase that opts in is
 * affected.</li>
 * </ul>
 *
 * <p>
 * Resolution never fails the refusal it serves: a catalog that cannot be read is skipped and the
 * next source used.
 */
public final class CheckMessages {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckMessages.class);

    /**
     * {@code {{name}}} (i18next) or {@code {name}} (the authored DSL form), each with an optional
     * {@code , format}; a name may be a one-hop path, {@code {Category.code}} (#8665).
     */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*(?:,\\s*(\\w+)\\s*)?\\}\\}|\\{([\\w.]+)(?:\\s*,\\s*(\\w+))?\\}");

    /** The format of a label inside a sentence (#8777), as the browser's formatter names it. */
    private static final String LOWERCASE = "lowercase";

    /** The parameter whose value picks a plural form, as in i18next. */
    private static final String COUNT = "count";

    /** The language of the default texts: the generated code and the SDK author them in English. */
    private static final String DEFAULT_LANGUAGE = "en";

    /** Reads the request language's own catalog entry for a key. */
    @FunctionalInterface
    interface CatalogReader {

        /**
         * The catalog text of a key in one language.
         *
         * @param language the request language (e.g. {@code bg}, {@code bg-BG})
         * @param catalogKey the fully qualified key ({@code <project>:<path>})
         * @return the translated text, or {@code null} when the language's catalogs do not carry it
         */
        String read(String language, String catalogKey);
    }

    private CheckMessages() {}

    /**
     * The refusal of a failed check, its message resolved for the request's language.
     *
     * @param catalogKey the message's fully qualified catalog key
     * @param text the default-language text
     * @param params the placeholder values, as alternating name and value
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException refusal(String catalogKey, String text, Object... params) {
        Map<String, Object> values = params(params);
        return new ValidationException(resolve(language(), CheckMessages::readCatalog, catalogKey, text, values), catalogKey, values);
    }

    /**
     * The refusal of a failed check about one property - a {@code compare} or {@code requiredWhen}
     * naming its {@code field:} (dirigible #8597) - its message resolved for the request's language.
     *
     * @param field the property the check is about
     * @param catalogKey the message's fully qualified catalog key
     * @param text the default-language text
     * @param params the placeholder values, as alternating name and value
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException fieldRefusal(String field, String catalogKey, String text, Object... params) {
        Map<String, Object> values = params(params);
        return new ValidationException(resolve(language(), CheckMessages::readCatalog, catalogKey, text, values), catalogKey, values,
                field);
    }

    /**
     * The warning of a soft check, its message resolved for the request's language.
     *
     * @param code the warning's confirmation code
     * @param catalogKey the message's fully qualified catalog key
     * @param text the default-language text
     * @param params the placeholder values, as alternating name and value
     * @return the warning
     */
    public static Warning warning(String code, String catalogKey, String text, Object... params) {
        Map<String, Object> values = params(params);
        return new Warning(code, resolve(language(), CheckMessages::readCatalog, catalogKey, text, values), catalogKey, values);
    }

    /**
     * A message resolved for the request's language.
     *
     * @param catalogKey the message's fully qualified catalog key
     * @param text the default-language text
     * @param params the placeholder values, as alternating name and value
     * @return the resolved, interpolated text
     */
    public static String message(String catalogKey, String text, Object... params) {
        return resolve(language(), CheckMessages::readCatalog, catalogKey, text, params(params));
    }

    /**
     * Resolves a message: the language's catalog, else the default text - and interpolates the
     * placeholders into the winner.
     *
     * @param language the request language, may be blank
     * @param catalogs the catalog reader
     * @param catalogKey the fully qualified catalog key, may be {@code null}
     * @param text the default-language text
     * @param params the placeholder values
     * @return the resolved text
     */
    static String resolve(String language, CatalogReader catalogs, String catalogKey, String text, Map<String, Object> params) {
        // singletonMap: a text may be null (a label nobody gave), which Map.of refuses.
        return resolve(language, catalogs, catalogKey, Collections.singletonMap(PluralCategories.OTHER, text), params);
    }

    /**
     * Resolves a message whose default text has plural forms: the language's catalog - the entry of the
     * plural category {@code count} falls into, else the plain one - else the default text of that
     * category, and interpolates the placeholders into the winner.
     *
     * @param language the request language, may be blank
     * @param catalogs the catalog reader
     * @param catalogKey the fully qualified catalog key, may be {@code null}
     * @param texts the default-language text by CLDR plural category; {@code other} is required
     * @param params the placeholder values
     * @return the resolved text
     */
    static String resolve(String language, CatalogReader catalogs, String catalogKey, Map<String, String> texts,
            Map<String, Object> params) {
        Long count = wholeCount(params);
        String normalized = normalize(language);
        if (normalized != null && catalogKey != null && catalogKey.indexOf(':') > 0) {
            String category = count == null ? null : PluralCategories.of(normalized, count);
            String resolved = category == null ? null : read(catalogs, normalized, catalogKey + "_" + category);
            if (isBlank(resolved)) {
                resolved = read(catalogs, normalized, catalogKey);
            }
            if (!isBlank(resolved)) {
                return interpolate(resolved, params, normalized);
            }
        }
        String category = count == null ? null : PluralCategories.of(DEFAULT_LANGUAGE, count);
        String text = category == null ? null : texts.get(category);
        return interpolate(text == null ? texts.get(PluralCategories.OTHER) : text, params, DEFAULT_LANGUAGE);
    }

    /** One catalog entry; a catalog that cannot be read is no entry. */
    private static String read(CatalogReader catalogs, String language, String catalogKey) {
        try {
            return catalogs.read(language, catalogKey);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not read the [{}] catalogs for [{}]", language, catalogKey, e);
            return null;
        }
    }

    /**
     * The {@code count} parameter when it is a whole number - the only kind a plural form is picked by.
     */
    private static Long wholeCount(Map<String, Object> params) {
        Object count = params == null ? null : params.get(COUNT);
        if (count instanceof Integer || count instanceof Long || count instanceof Short || count instanceof Byte) {
            return ((Number) count).longValue();
        }
        if (count instanceof Number number) {
            double value = number.doubleValue();
            return value == Math.rint(value) && !Double.isInfinite(value) ? (long) value : null;
        }
        return null;
    }

    /**
     * Interpolates {@code {name}} and {@code {{name}}} placeholders in the default language.
     *
     * @param text the text
     * @param params the values
     * @return the interpolated text
     * @see #interpolate(String, Map, String)
     */
    static String interpolate(String text, Map<String, Object> params) {
        return interpolate(text, params, DEFAULT_LANGUAGE);
    }

    /**
     * Interpolates {@code {name}} and {@code {{name}}} placeholders, each optionally formatted
     * ({@code {name, lowercase}}); an unknown name is left as it is, a name present with a {@code null}
     * value (an empty field of the record) becomes the empty string, and an unknown format leaves the
     * value as it is - as i18next does.
     *
     * @param text the text
     * @param params the values
     * @param language the language of the text, which lower-cases by its own rules
     * @return the interpolated text
     */
    static String interpolate(String text, Map<String, Object> params, String language) {
        if (text == null || params == null || params.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            boolean doubled = matcher.group(1) != null;
            String name = doubled ? matcher.group(1) : matcher.group(3);
            String format = doubled ? matcher.group(2) : matcher.group(4);
            Object value = params.get(name);
            String replacement;
            if (value != null) {
                replacement = LOWERCASE.equals(format) ? lowercase(String.valueOf(value), language) : String.valueOf(value);
            } else {
                // A parameter present with no value - the record's field is empty (#8747) - reads as
                // nothing, never the text "null"; the error body's messageParams keep the JSON null.
                replacement = params.containsKey(name) ? "" : matcher.group();
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * A label lower-cased for the inside of a sentence, word by word, keeping a word in capitals - the
     * browser's {@code lowercase} formatter (#8133), so "Валута" reads "валута" and "ДДС" stays.
     *
     * @param value the label
     * @param language the language whose case rules apply
     * @return the lower-cased label
     */
    static String lowercase(String value, String language) {
        Locale locale = Locale.forLanguageTag(language);
        String[] words = value.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            words[i] = word.length() > 1 && word.equals(word.toUpperCase(locale)) ? word : word.toLowerCase(locale);
        }
        return String.join(" ", words);
    }

    /**
     * The alternating name/value arguments as a map.
     *
     * @param params name, value, name, value...
     * @return the map, in argument order
     */
    static Map<String, Object> params(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < params.length; i += 2) {
            values.put(String.valueOf(params[i]), params[i + 1]);
        }
        return values;
    }

    /**
     * The request language - the thread-bound override, else the request's {@code Accept-Language};
     * {@code null} outside a request.
     */
    static String language() {
        try {
            // Outside a request (a process step, a job) nobody reads the message in a language of
            // their own; asking would only log the facade's no-request error on every refusal.
            return HttpRequestFacade.isValid() ? UserFacade.getLanguage() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** A usable language tag, or {@code null} for none / any. */
    private static String normalize(String language) {
        if (language == null) {
            return null;
        }
        String trimmed = language.trim()
                                 .replace('_', '-');
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    /** The primary subtag of a language tag ({@code bg} of {@code bg-BG}). */
    private static String primary(String language) {
        int dash = language.indexOf('-');
        return (dash > 0 ? language.substring(0, dash) : language).toLowerCase(Locale.ROOT);
    }

    /**
     * Reads a key from the registry catalogs of its project in one language, the way the platform's
     * locales service serves them to the UI: every JSON file of {@code /<project>/i18n/<locale>/},
     * merged in listing order. The locale folder is the one named exactly like the language, else the
     * first whose primary subtag matches ({@code bg} reads {@code bg-BG}).
     */
    static String readCatalog(String language, String catalogKey) {
        int colon = catalogKey.indexOf(':');
        String project = catalogKey.substring(0, colon);
        String[] path = catalogKey.substring(colon + 1)
                                  .split("\\.");
        ICollection i18n = RepositoryFacade.getCollection(RegistryFacade.toRepositoryPath("/" + project + "/i18n"));
        if (i18n == null || !i18n.exists()) {
            return null;
        }
        String folder = localeFolder(i18n.getCollectionsNames(), language);
        if (folder == null) {
            return null;
        }
        ICollection locale = i18n.getCollection(folder);
        String found = null;
        for (String name : sorted(locale.getResourcesNames())) {
            if (!name.endsWith(".json")) {
                continue;
            }
            IResource resource = locale.getResource(name);
            byte[] content = resource.getContent();
            if (content == null) {
                continue;
            }
            String value = lookup(JsonParser.parseString(new String(content, StandardCharsets.UTF_8)), path);
            if (value != null) {
                found = value;
            }
        }
        return found;
    }

    /**
     * The locale folder for a language.
     *
     * @param folders the folder names under {@code i18n/}
     * @param language the language tag
     * @return the folder, or {@code null} when none matches
     */
    static String localeFolder(List<String> folders, String language) {
        if (folders == null) {
            return null;
        }
        for (String folder : folders) {
            if (folder.equalsIgnoreCase(language)) {
                return folder;
            }
        }
        String primary = primary(language);
        for (String folder : sorted(folders)) {
            if (primary(folder).equals(primary)) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Walks a dotted path through a parsed catalog.
     *
     * @param root the catalog
     * @param path the path segments
     * @return the string at the path, or {@code null}
     */
    static String lookup(JsonElement root, String[] path) {
        JsonElement node = root;
        for (String segment : path) {
            if (node == null || !node.isJsonObject()) {
                return null;
            }
            node = ((JsonObject) node).get(segment);
        }
        return node != null && node.isJsonPrimitive() && node.getAsJsonPrimitive()
                                                             .isString() ? node.getAsString() : null;
    }

    private static List<String> sorted(List<String> names) {
        List<String> copy = names == null ? new ArrayList<>() : new ArrayList<>(names);
        Collections.sort(copy);
        return copy;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
