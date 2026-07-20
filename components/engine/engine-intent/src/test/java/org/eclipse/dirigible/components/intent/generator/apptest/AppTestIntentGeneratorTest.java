/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.apptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the pure manifest-building core of {@link AppTestIntentGenerator}: module-level
 * coordinates (module id, standalone shell, sanitized REST base, id property, languages) and the
 * per-entity mapping (label/plural/layout/route/nav-group/api/table from the EDM metadata; fields
 * and dropdown relations from the intent; cross-model relations and composition details excluded;
 * the multilingual sample derived from inline seeds). Repository-free — it feeds a hand-built EDM
 * metadata map, exactly the shape the {@code .model} carries.
 */
class AppTestIntentGeneratorTest {

    private static final String INTENT = """
            name: kf-mod-countries
            languages: [en, bg]
            uses:
              - { model: kf-mod-currencies }
            entities:
              - name: Country
                kind: setting
                group: master-data
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, unique: true, length: 255 }
                  - { name: code2, type: string, required: true, unique: true, length: 2 }
              - name: City
                group: master-data
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 200 }
                  - { name: uuid, type: uuid }
                  - { name: slug, type: string, calculatedOnCreate: "1" }
                  - { name: total, type: decimal, aggregate: true }
                relations:
                  - { name: Country, kind: manyToOne, to: Country, required: true }
                  - { name: Currency, kind: manyToOne, to: Currency, model: kf-mod-currencies, required: true }
              - name: Account
                group: master-data
                hierarchy: Parent
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 200 }
                relations:
                  - { name: Parent, kind: manyToOne, to: Account }
            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Albania, code2: AL }
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 1, name: "Албания", code2: AL }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);

    @SuppressWarnings("unchecked")
    @Test
    void buildsTheModuleLevelCoordinates() {
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm());

        assertEquals("kf-mod-countries", manifest.get("module"));
        assertEquals("/services/web/kf-mod-countries/gen/kf-mod-countries/index.html", manifest.get("standaloneShell"));
        // the REST base uses the sanitized (java-identifier) gen folder, not the raw hyphenated name
        assertEquals("/services/java/kf-mod-countries/gen/kf_mod_countries/api", manifest.get("restBase"));
        assertEquals("Id", manifest.get("idProperty"));
        assertEquals(List.of("en", "bg"), manifest.get("languages"));
        assertEquals(3, ((List<Object>) manifest.get("entities")).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void marksAutoReadOnlyFieldsAndHierarchyEntities() {
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm());

        // a uuid and a calculated field render without an editable input - the runner must not fill them
        Map<String, Object> city = entity(manifest, "City");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) city.get("fields");
        Map<String, Object> uuid = fields.stream()
                                         .filter(f -> "Uuid".equals(f.get("name")))
                                         .findFirst()
                                         .orElseThrow();
        assertEquals(Boolean.TRUE, uuid.get("readOnly"));
        Map<String, Object> slug = fields.stream()
                                         .filter(f -> "Slug".equals(f.get("name")))
                                         .findFirst()
                                         .orElseThrow();
        assertEquals(Boolean.TRUE, slug.get("readOnly"));
        Map<String, Object> total = fields.stream()
                                          .filter(f -> "Total".equals(f.get("name")))
                                          .findFirst()
                                          .orElseThrow();
        assertEquals(Boolean.TRUE, total.get("readOnly"), "an aggregate renders in the totals footer, not as an input");

        // a hierarchy entity lists as a tree - the runner branches on the flag
        Map<String, Object> account = entity(manifest, "Account");
        assertEquals(Boolean.TRUE, account.get("hierarchy"));
        assertNull(city.get("hierarchy"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapsEntityMetadataAndFields() {
        Map<String, Object> country =
                entity(AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm()), "Country");

        assertEquals("Country", country.get("label"));
        assertEquals("Countries", country.get("labelPlural"));
        assertEquals("manage-list", country.get("layout"));
        assertEquals("#/Country", country.get("route"));
        assertEquals("master-data", country.get("navGroup"));
        // api uses the sanitized perspective (Settings -> settings)
        assertEquals("/settings/CountryController", country.get("api"));
        assertEquals("KF_MOD_COUNTRIES_COUNTRY", country.get("table"));
        assertEquals(Boolean.TRUE, country.get("multilingual"));
        assertEquals(Boolean.TRUE, country.get("expectSeedData"));

        // fields: PascalCased names, required/unique/length flags, no PK
        List<Map<String, Object>> fields = (List<Map<String, Object>>) country.get("fields");
        assertEquals(2, fields.size());
        Map<String, Object> name = fields.get(0);
        assertEquals("Name", name.get("name"));
        assertEquals("string", name.get("type"));
        assertEquals(Boolean.TRUE, name.get("required"));
        assertEquals(Boolean.TRUE, name.get("unique"));
        assertEquals(255.0, ((Number) name.get("length")).doubleValue());
        assertEquals(Boolean.TRUE, name.get("major"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void derivesTheMultilingualSampleFromInlineSeeds() {
        Map<String, Object> country =
                entity(AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm()), "Country");
        Map<String, Object> sample = (Map<String, Object>) country.get("multilingualSample");
        assertNotNull(sample, "an inline base + bg seed pair yields a multilingual sample");
        assertEquals("bg", sample.get("language"));
        assertEquals("Albania", sample.get("base"));
        assertEquals("Албания", sample.get("translated"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void emitsToOneRelationsAsDropdowns() {
        Map<String, Object> city =
                entity(AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm()), "City");
        List<Map<String, Object>> relations = (List<Map<String, Object>>) city.get("relations");
        assertNotNull(relations);
        assertEquals(2, relations.size());
        Map<String, Object> country = relations.get(0);
        assertEquals("Country", country.get("name"));
        assertEquals("manyToOne", country.get("kind"));
        assertEquals("Country", country.get("to"));
        assertEquals(Boolean.TRUE, country.get("required"));
        assertEquals("dropdown", country.get("widget"));
        assertEquals("Name", country.get("labelFrom"));
        assertNull(country.get("crossModel"));

        // the cross-model relation resolves an absolute controller URL in the OWNER module (naming
        // convention here - no generation context; the real pass resolves against the owner's .model)
        Map<String, Object> currency = relations.get(1);
        assertEquals("Currency", currency.get("name"));
        assertEquals(Boolean.TRUE, currency.get("crossModel"));
        assertEquals("/services/java/kf-mod-currencies/gen/kf_mod_currencies/api/currency/CurrencyController", currency.get("apiAbsolute"));
        assertEquals("Name", currency.get("labelFrom"));
    }

    @Test
    void skipsProjectionAndDetailEntities() {
        Map<String, Map<String, Object>> edm = edm();
        edm.put("Extra", edmEntity("Extra", "Extra", "Extras", "MANAGE_DETAILS", "Extras", "master-data", "KF_MOD_COUNTRIES_EXTRA", false));
        // still only Country + City + Account — the detail child is excluded
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("kf-mod-countries", "kf-mod-countries", model, edm);
        List<?> entities = (List<?>) manifest.get("entities");
        assertEquals(3, entities.size());
        assertNull(entityOrNull(manifest, "Extra"));
    }

    // ---- helpers: a minimal .model-shaped metadata map -------------------------------------------

    private static Map<String, Map<String, Object>> edm() {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        byName.put("Country",
                edmEntity("Country", "Country", "Countries", "MANAGE_MASTER", "Settings", "master-data", "KF_MOD_COUNTRIES_COUNTRY", true));
        byName.put("City", edmEntity("City", "City", "Cities", "MANAGE_MASTER", "Settings", "master-data", "KF_MOD_COUNTRIES_CITY", false));
        byName.put("Account",
                edmEntity("Account", "Account", "Accounts", "MANAGE_LIST", "Accounts", "master-data", "KF_MOD_COUNTRIES_ACCOUNT", false));
        return byName;
    }

    private static Map<String, Object> edmEntity(String name, String label, String plural, String layoutType, String perspective,
            String navId, String table, boolean multilingual) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("entityLabel", label);
        entity.put("menuLabel", plural);
        entity.put("layoutType", layoutType);
        entity.put("perspectiveName", perspective);
        entity.put("perspectiveNavId", navId);
        entity.put("dataName", table);
        entity.put("multilingual", String.valueOf(multilingual));
        Map<String, Object> pk = new LinkedHashMap<>();
        pk.put("name", "Id");
        pk.put("dataPrimaryKey", "true");
        entity.put("properties", List.of(pk));
        return entity;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Map<String, Object> manifest, String name) {
        Map<String, Object> found = entityOrNull(manifest, name);
        assertTrue(found != null, "entity " + name + " present");
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityOrNull(Map<String, Object> manifest, String name) {
        for (Object entity : (List<Object>) manifest.get("entities")) {
            Map<String, Object> map = (Map<String, Object>) entity;
            if (name.equals(map.get("name"))) {
                return map;
            }
        }
        return null;
    }
}
