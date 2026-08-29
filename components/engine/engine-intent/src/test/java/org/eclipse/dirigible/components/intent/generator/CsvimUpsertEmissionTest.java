/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.generator.csvim.CsvimIntentGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.SeedIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The emitted .csvim always carries an explicit {@code upsert}, because the two seed kinds want
 * opposite re-import semantics: starter content must never touch a row the user edited after the
 * first import (#6980), while a language seed is release-maintained nomenclature that must keep
 * updating. An authored {@code upsert:} overrides either way.
 *
 * <p>
 * Lives in this package (not beside {@code CsvimIntentGeneratorTest}) because
 * {@link IntentGenerationContext}'s constructor is package-private.
 */
class CsvimUpsertEmissionTest {

    private static final String YAML = """
            name: countries
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 34, name: Bulgaria }
            """;

    private static final String MULTILINGUAL_YAML = """
            name: uoms
            languages: [en, bg]
            entities:
              - name: UoM
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
            seeds:
              - name: uoms-bg
                entity: UoM
                language: bg
                rows:
                  - { id: 1, name: "Килограм" }
            """;

    @Test
    void entitySeedDeclaresCreateIfAbsentSemantics() {
        IntentModel model = IntentParser.parse(YAML);
        String csvim = renderCsvim(model, model.getSeeds()
                                               .get(0));
        assertTrue(csvim.contains("\"upsert\": false"),
                "a starter seed's .csvim must pin upsert OFF so a re-import never stomps user-edited rows: " + csvim);
    }

    @Test
    void languageSeedDeclaresReleaseMaintainedSemantics() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_YAML);
        String csvim = renderCsvim(model, model.getSeeds()
                                               .get(0));
        assertTrue(csvim.contains("\"upsert\": true"),
                "a language seed's .csvim must pin upsert ON - translations are release-maintained: " + csvim);
    }

    @Test
    void authoredUpsertOverridesTheSeedKindDefault() {
        IntentModel model = IntentParser.parse(YAML.replace("""
                  - name: countries
                    entity: Country
                """, """
                  - name: countries
                    entity: Country
                    upsert: true
                """));
        String csvim = renderCsvim(model, model.getSeeds()
                                               .get(0));
        assertTrue(csvim.contains("\"upsert\": true"), "upsert: true on the seed must override the starter-content default: " + csvim);
    }

    private static String renderCsvim(IntentModel model, SeedIntent seed) {
        IntentGenerationContext context =
                new IntentGenerationContext(model, "/users/admin/workspace/proj", "proj", "workspace", "proj", null);
        EntityIntent entity = model.getEntities()
                                   .stream()
                                   .filter(e -> e.getName()
                                                 .equals(seed.getEntity()))
                                   .findFirst()
                                   .orElseThrow();
        return CsvimIntentGenerator.renderCsvimForTest(context, seed, entity, seed.getName());
    }
}
