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
 * Placeholders ({@code {count}}, {@code {match}}, and their i18next spelling {@code {{count}}}) are
 * interpolated AFTER translation, so a translation may move them.
 *
 * <p>
 * Resolution never fails the refusal it serves: a catalog that cannot be read is skipped and the
 * next source used.
 */
public final class CheckMessages {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckMessages.class);

    /** {@code {{name}}} (i18next) or {@code {name}} (the authored DSL form). */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}|\\{(\\w+)\\}");

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
        String resolved = null;
        String normalized = normalize(language);
        if (normalized != null && catalogKey != null && catalogKey.indexOf(':') > 0) {
            try {
                resolved = catalogs.read(normalized, catalogKey);
            } catch (RuntimeException e) {
                LOGGER.debug("Could not read the [{}] catalogs for [{}]: {}", normalized, catalogKey, e.getMessage());
            }
        }
        return interpolate(isBlank(resolved) ? text : resolved, params);
    }

    /**
     * Interpolates {@code {name}} and {@code {{name}}} placeholders; an unknown name is left as it is.
     *
     * @param text the text
     * @param params the values
     * @return the interpolated text
     */
    static String interpolate(String text, Map<String, Object> params) {
        if (text == null || params == null || params.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Object value = params.get(name);
            matcher.appendReplacement(out,
                    Matcher.quoteReplacement(value == null && !params.containsKey(name) ? matcher.group() : String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
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
    private static String language() {
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
