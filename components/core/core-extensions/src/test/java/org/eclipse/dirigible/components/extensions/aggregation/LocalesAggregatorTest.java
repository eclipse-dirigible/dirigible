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

import static org.eclipse.dirigible.components.extensions.aggregation.Modules.json;
import static org.eclipse.dirigible.components.extensions.aggregation.Modules.stub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.extensions.modules.ExtensionModuleReader;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The locales and their merged catalogs: {@code common} from the locale's own path, then each
 * registry module's {@code i18n/<locale>/*.json}, filtered by namespace - for the registered
 * locales and for every offered language no registered locale stands for (#8774).
 */
@ExtendWith(MockitoExtension.class)
class LocalesAggregatorTest {

    private static final String POINT = "test-locales";

    private static final String COMMON = "/resources-locale/i18n/en-US/common.json";

    private static final String EN_US = "{'id': 'en-US', 'label': 'English (US)', 'common': '" + COMMON + "'}";

    private static final String LANGUAGES = DirigibleConfig.APPLICATION_LANGUAGES.getKey();

    @Mock
    private ExtensionModuleReader reader;

    @Mock
    private IRepository repository;

    private ICollection root;

    @BeforeEach
    void registry() {
        root = mock(ICollection.class);
        lenient().when(repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC))
                 .thenReturn(root);
        lenient().when(root.getCollectionsNames())
                 .thenReturn(List.of("resources-locale", "orders", "untranslated"));
        catalogs("resources-locale", "en-US", List.of("common.json"));
        catalogs("orders", "en-US", List.of("a.json", "b.json"));
        catalogs("untranslated", "en-US", null);
        catalog(COMMON, "{\"save\": \"Save\"}");
        catalog("/orders/i18n/en-US/a.json", "{\"title\": \"Orders\", \"shared\": \"from a\"}");
        catalog("/orders/i18n/en-US/b.json", "{\"shared\": \"from b\"}");
    }

    @AfterEach
    void languages() {
        Configuration.remove(LANGUAGES);
    }

    @Test
    void merges_the_catalogs_of_every_module_beside_common() {
        stub(reader, POINT, json(EN_US));

        Map<String, Object> result = new LocalesAggregator(reader, repository).aggregate(List.of(POINT), null, null);

        assertEquals(Map.of("en-US", Map.of("common", Map.of("save", "Save"), "orders", Map.of("title", "Orders", "shared", "from b"))),
                result.get("translations"));
    }

    @Test
    void includes_only_the_requested_namespaces() {
        stub(reader, POINT, json(EN_US));

        Map<String, Object> result =
                new LocalesAggregator(reader, repository).aggregate(List.of(POINT), List.of("en-US"), List.of("other"));

        assertEquals(Map.of("en-US", Map.of("common", Map.of("save", "Save"))), result.get("translations"));
    }

    @Test
    void resolves_a_language_code_to_the_locale_it_starts() {
        stub(reader, POINT, json(EN_US));

        Map<String, Object> result = new LocalesAggregator(reader, repository).aggregate(List.of(POINT), List.of("en"), List.of("other"));

        assertEquals(Map.of("en-US", Map.of("common", Map.of("save", "Save"))), result.get("translations"));
    }

    @Test
    void refuses_a_language_no_locale_stands_for() {
        stub(reader, POINT, json(EN_US));
        LocalesAggregator aggregator = new LocalesAggregator(reader, repository);
        List<String> languages = List.of("en-US", "xx");

        UnknownLocaleException refusal =
                assertThrows(UnknownLocaleException.class, () -> aggregator.aggregate(List.of(POINT), languages, null));
        assertEquals("Language 'xx' is not registered", refusal.getMessage());
    }

    @Test
    void offers_a_configured_language_as_the_locale_of_its_catalogs() {
        Configuration.set(LANGUAGES, "en,bg");
        stub(reader, POINT, json(EN_US));
        localeFolders("orders", List.of("bg-BG", "en-US"));
        localeFolders("resources-locale", List.of("en-US"));
        localeFolders("untranslated", null);
        catalogs("orders", "bg-BG", List.of("a.json"));
        catalogs("resources-locale", "bg-BG", null);
        catalogs("untranslated", "bg-BG", null);
        catalog("/orders/i18n/bg-BG/a.json", "{\"title\": \"Поръчки\"}");

        Map<String, Object> result = new LocalesAggregator(reader, repository).aggregate(List.of(POINT), List.of("bg"), null);

        assertEquals(List.of(Modules.object(EN_US), Map.of("id", "bg-BG", "label", "Български")), result.get("locales"));
        assertEquals(Map.of("bg-BG", Map.of("common", Map.of(), "orders", Map.of("title", "Поръчки"))), result.get("translations"));
    }

    @Test
    void offers_a_language_without_catalogs_under_its_own_code() {
        Configuration.set(LANGUAGES, "en,fr");
        stub(reader, POINT, json(EN_US));
        localeFolders("orders", List.of("en-US"));
        localeFolders("resources-locale", List.of("en-US"));
        localeFolders("untranslated", null);

        Map<String, Object> result = new LocalesAggregator(reader, repository).aggregate(List.of(POINT), null, List.of("other"));

        assertEquals(List.of("en-US", "fr"), Modules.values(locales(result), "id"));
        assertEquals(List.of("English (US)", "Français"), Modules.values(locales(result), "label"));
    }

    @Test
    void keeps_a_registered_locale_of_an_offered_language() {
        Configuration.set(LANGUAGES, "en,bg");
        String bgCommon = "/nav/i18n/bg-BG/common.json";
        stub(reader, POINT, json(EN_US), json("{'id': 'bg-BG', 'label': 'Bulgarian', 'common': '" + bgCommon + "'}"));
        catalogs("resources-locale", "bg-BG", null);
        catalogs("orders", "bg-BG", null);
        catalogs("untranslated", "bg-BG", null);
        catalog(bgCommon, "{\"save\": \"Запази\"}");

        Map<String, Object> result = new LocalesAggregator(reader, repository).aggregate(List.of(POINT), List.of("bg"), null);

        assertEquals(List.of("bg-BG", "en-US"), Modules.values(locales(result), "id"));
        assertEquals(List.of("Bulgarian", "English (US)"), Modules.values(locales(result), "label"));
        assertEquals(Map.of("bg-BG", Map.of("common", Map.of("save", "Запази"))), result.get("translations"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> locales(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("locales");
    }

    private void localeFolders(String module, List<String> locales) {
        ICollection folder = mock(ICollection.class);
        lenient().when(root.getCollection(module + "/i18n"))
                 .thenReturn(folder);
        lenient().when(folder.exists())
                 .thenReturn(locales != null);
        lenient().when(folder.getCollectionsNames())
                 .thenReturn(locales);
    }

    private void catalogs(String module, String locale, List<String> names) {
        ICollection folder = mock(ICollection.class);
        lenient().when(root.getCollection(module + "/i18n/" + locale))
                 .thenReturn(folder);
        lenient().when(folder.exists())
                 .thenReturn(names != null);
        lenient().when(folder.getResourcesNames())
                 .thenReturn(names);
    }

    private void catalog(String path, String content) {
        IResource resource = mock(IResource.class);
        lenient().when(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + path))
                 .thenReturn(resource);
        lenient().when(resource.exists())
                 .thenReturn(true);
        lenient().when(resource.getContent())
                 .thenReturn(content.getBytes(StandardCharsets.UTF_8));
    }
}
