/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.csvim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the CSV body rendering, in particular that a seed row may set a to-one relation's FK by
 * the relation's authored name ({@code Country: 34} on a City row) - the FK column is emitted with
 * the same {@code <ENTITY>_<RELATION>} dataName the EDM generator gives the FK property, and only
 * when a row actually references the relation (relation-free seeds keep their previous shape).
 */
class CsvimIntentGeneratorTest {

    private static final String YAML = """
            name: countries
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: City
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country, required: true }
            seeds:
              - name: cities
                entity: City
                rows:
                  - { id: 1, name: Sofia, Country: 34 }
                  - { id: 2, name: Rome,  Country: 110 }
              - name: countries
                entity: Country
                rows:
                  - { id: 34, name: Bulgaria }
            """;

    @Test
    void seedRowsMaySetAToOneRelationForeignKey() {
        IntentModel model = IntentParser.parse(YAML);
        String csv = CsvimIntentGenerator.renderCsvForTest(model.getEntities()
                                                                .get(1),
                model.getSeeds()
                     .get(0));
        assertEquals("""
                CITY_ID,CITY_NAME,CITY_COUNTRY
                1,Sofia,34
                2,Rome,110
                """, csv, "a row's relation-name key should become the FK column");
    }

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
                  - { name: iso, type: string, length: 10 }
                  - { name: numerator, type: decimal }
            seeds:
              - name: uoms-bg
                entity: UoM
                language: bg
                rows:
                  - { id: 1, name: "Килограм" }
                  - { id: 2, name: "Литър", iso: "Л" }
            """;

    @Test
    void languageSeedRendersTheLangTableShape() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_YAML);
        String csv = CsvimIntentGenerator.renderCsvForTest(model.getEntities()
                                                                .get(0),
                model.getSeeds()
                     .get(0));
        assertEquals("""
                GUID,Id,Name,Iso,Language
                1,1,Килограм,,bg
                2,2,Литър,Л,bg
                """, csv,
                "a language seed should render GUID (auto-numbered) + Id + the referenced translatable columns + the Language code");
    }

    @Test
    void relationFreeSeedsKeepTheirShape() {
        IntentModel model = IntentParser.parse(YAML);
        String csv = CsvimIntentGenerator.renderCsvForTest(model.getEntities()
                                                                .get(0),
                model.getSeeds()
                     .get(1));
        assertEquals("""
                COUNTRY_ID,COUNTRY_NAME
                34,Bulgaria
                """, csv, "a seed that never references a relation must not gain FK columns");
    }
}
