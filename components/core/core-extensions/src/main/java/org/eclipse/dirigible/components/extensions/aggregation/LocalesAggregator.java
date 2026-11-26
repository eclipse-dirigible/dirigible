/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.dirigible.commons.config.ApplicationLanguages;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModule;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModuleReader;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Aggregates the locales of extension points and the translation catalogs of each.
 *
 * <p>
 * A locale carries {@code id}, {@code label}, an optional {@code order}, and an optional
 * {@code common}: the registry path of its common catalog, served under the {@code common}
 * namespace (empty when the locale declares none). Every other namespace is a top-level registry
 * folder, whose catalogs are the {@code i18n/<locale>/*.json} files in it, merged.
 *
 * <p>
 * Beside the registered locales, every language the instance offers ({@link ApplicationLanguages})
 * is a locale of its own, so offering a language is enough for its catalogs to load (#8774). Such a
 * language takes the id of the catalog folders that exist for it (exact, else the first by its
 * primary subtag: {@code bg} becomes {@code bg-BG}) and its own name as the label. A registered
 * locale the language resolves to is used as it is - its id, label and common catalog win.
 */
@Component
class LocalesAggregator {

    /** The extension point read when the request names none. */
    static final String DEFAULT_EXTENSION_POINT = "platform-locales";

    private static final List<String> EXPORTS = List.of("getLocale");

    private static final String ID = "id";

    private static final String LABEL = "label";

    private static final String COMMON = "common";

    private static final JsonMapper JSON = JsonMapper.builder()
                                                     .build();

    private static final TypeReference<Map<String, Object>> OBJECT = new TypeReference<>() {};

    private final ExtensionModuleReader reader;

    private final IRepository repository;

    /**
     * Instantiates a new locales aggregator.
     *
     * @param reader the extension module reader
     * @param repository the repository
     */
    LocalesAggregator(ExtensionModuleReader reader, IRepository repository) {
        this.reader = reader;
        this.repository = repository;
    }

    /**
     * Aggregates the locales of the given extension points and the offered languages with their
     * translations.
     *
     * @param extensionPoints the extension points
     * @param languages the locales to translate, each a locale id or a language code a locale id starts
     *        with, or {@code null} for every locale
     * @param namespaces the namespaces to include besides {@code common}, or {@code null} for every one
     * @return the locales and the translations, keyed by locale id and then by namespace
     * @throws UnknownLocaleException when a requested language resolves to no locale
     * @throws IllegalStateException when a common catalog a locale declares does not exist
     */
    Map<String, Object> aggregate(List<String> extensionPoints, List<String> languages, List<String> namespaces) {
        ICollection registry = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        List<Map<String, Object>> registered = reader.read(extensionPoints, EXPORTS)
                                                     .stream()
                                                     .map(ExtensionModule::content)
                                                     .toList();
        List<Map<String, Object>> locales = Stream.concat(registered.stream(), offered(registered, registry).stream())
                                                  .sorted(ExtensionOrdering.byOrderThenExactText(LABEL))
                                                  .toList();
        Map<String, Object> translations = new LinkedHashMap<>();
        if (languages == null) {
            for (Map<String, Object> locale : locales) {
                translations.put(String.valueOf(locale.get(ID)), translations(locale, namespaces, registry));
            }
        } else {
            for (String language : languages) {
                Map<String, Object> locale = resolve(locales, language).orElseThrow(() -> new UnknownLocaleException(language));
                translations.put(String.valueOf(locale.get(ID)), translations(locale, namespaces, registry));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("locales", locales);
        result.put("translations", translations);
        return result;
    }

    /** A locale for every offered language that no registered locale stands for. */
    private static List<Map<String, Object>> offered(List<Map<String, Object>> registered, ICollection registry) {
        SortedSet<String> catalogLocales = null;
        List<Map<String, Object>> offered = new ArrayList<>();
        for (String language : ApplicationLanguages.get()) {
            if (resolve(registered, language).isPresent() || resolve(offered, language).isPresent()) {
                continue;
            }
            if (catalogLocales == null) {
                catalogLocales = catalogLocales(registry);
            }
            Map<String, Object> locale = new LinkedHashMap<>();
            locale.put(ID, catalogLocale(catalogLocales, language));
            locale.put(LABEL, ownName(language));
            offered.add(locale);
        }
        return offered;
    }

    /**
     * The locale a language stands for: the one with that id, else the first whose id starts with it.
     */
    private static Optional<Map<String, Object>> resolve(List<Map<String, Object>> locales, String language) {
        String prefix = language + "-";
        return locales.stream()
                      .filter(locale -> language.equals(locale.get(ID)))
                      .findFirst()
                      .or(() -> locales.stream()
                                       .filter(locale -> String.valueOf(locale.get(ID))
                                                               .startsWith(prefix))
                                       .findFirst());
    }

    /** The locale folders the published modules ship catalogs in ({@code <module>/i18n/<locale>}). */
    private static SortedSet<String> catalogLocales(ICollection registry) {
        SortedSet<String> catalogLocales = new TreeSet<>();
        for (String module : registry.getCollectionsNames()) {
            ICollection i18n = registry.getCollection(module + "/i18n");
            if (i18n.exists()) {
                catalogLocales.addAll(i18n.getCollectionsNames());
            }
        }
        return catalogLocales;
    }

    /**
     * The catalog folder of a language: its exact code, else the first by its primary subtag, else the
     * code.
     */
    private static String catalogLocale(SortedSet<String> catalogLocales, String language) {
        if (catalogLocales.contains(language)) {
            return language;
        }
        String prefix = language + "-";
        return catalogLocales.stream()
                             .filter(catalogLocale -> catalogLocale.startsWith(prefix))
                             .findFirst()
                             .orElse(language);
    }

    /** The language's name in the language itself, capitalized ({@code bg} is "Български"). */
    private static String ownName(String language) {
        Locale locale = Locale.forLanguageTag(language);
        String name = locale.getDisplayLanguage(locale);
        if (name.isEmpty()) {
            return language;
        }
        int first = name.offsetByCodePoints(0, 1);
        return name.substring(0, first)
                   .toUpperCase(locale)
                + name.substring(first);
    }

    private Map<String, Object> translations(Map<String, Object> locale, List<String> namespaces, ICollection registry) {
        String language = String.valueOf(locale.get(ID));
        Object common = locale.get(COMMON);
        String commonPath = common == null ? null : String.valueOf(common);
        Map<String, Object> translations = new LinkedHashMap<>();
        translations.put(COMMON, commonPath == null ? new LinkedHashMap<>() : readCatalog(commonPath));
        for (String module : registry.getCollectionsNames()) {
            if (namespaces != null && !namespaces.contains(module)) {
                continue;
            }
            ICollection catalogs = registry.getCollection(module + "/i18n/" + language);
            if (!catalogs.exists()) {
                continue;
            }
            for (String catalog : catalogs.getResourcesNames()) {
                String catalogPath = "/" + module + "/i18n/" + language + "/" + catalog;
                if (!catalogPath.equals(commonPath)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> merged = (Map<String, Object>) translations.computeIfAbsent(module, key -> new LinkedHashMap<>());
                    merged.putAll(readCatalog(catalogPath));
                }
            }
        }
        return translations;
    }

    private Map<String, Object> readCatalog(String registryPath) {
        IResource resource = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + registryPath);
        if (!resource.exists()) {
            throw new IllegalStateException("Translation catalog [" + registryPath + "] does not exist in the registry");
        }
        return JSON.readValue(resource.getContent(), OBJECT);
    }

}
