/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A {@code number: { per: Company }} field whose Company relation carries {@code init: 1} resolves
 * a null FK to that default at allocation time (#7101). The init is a database default the insert
 * applies AFTER a {@code stampOn: create} number is drawn, so without the fallback the default
 * company numbered on the series' base row and the second company's partition forked from it.
 */
class EdmPartitionedNumberInitTest {

    private static final String VACATIONS = """
            name: vacations
            entities:
              - name: Company
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: VacationRequest
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100, number: { series: Vacation Request, per: Company, stampOn: create } }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, init: 1 }
            """;

    @Test
    void thePartitionFallsBackToTheRelationsInit() {
        Map<String, Object> number = property(VACATIONS, "VacationRequest", "Number");
        assertEquals("Company", number.get("numberPer"));
        assertEquals("1", number.get("numberPerDefault"),
                "a row that leaves the FK unset WILL carry the init value - the allocator must partition by it, not by the base row");
        assertEquals("1", property(VACATIONS, "VacationRequest", "Company").get("dataDefaultValue"), "sanity: the FK's database default");
    }

    @Test
    void aRelationWithoutInitLeavesNoFallback() {
        String yaml = VACATIONS.replace(", init: 1", "");
        Map<String, Object> number = property(yaml, "VacationRequest", "Number");
        assertEquals("Company", number.get("numberPer"));
        assertNull(number.get("numberPerDefault"), "no default to fall back to - a null FK is the tenant-wide base row, as before");
    }

    @Test
    void anUnpartitionedNumberNeverCarriesOne() {
        String yaml = VACATIONS.replace("per: Company, ", "");
        Map<String, Object> number = property(yaml, "VacationRequest", "Number");
        assertEquals("", number.get("numberPer"));
        assertNull(number.get("numberPerDefault"));
    }

    @Test
    void theIssueStampedNumberCarriesTheSameFallback() {
        // Both stamp paths read the same marker off the .model property; the issue path additionally
        // rides the numbering glue (NumberingInitPartitionTest) - they must agree on the partition.
        String yaml = VACATIONS.replace("stampOn: create", "stampOn: issue");
        Map<String, Object> number = property(yaml, "VacationRequest", "Number");
        assertEquals("1", number.get("numberPerDefault"));
        assertEquals("true", number.get("generatedUuid"), "sanity: still the UUID placeholder path");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String entityName, String propertyName) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "vacations");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entities.stream()
                                                                                   .filter(entity -> entityName.equals(entity.get("name")))
                                                                                   .findFirst()
                                                                                   .orElseThrow()
                                                                                   .get("properties");
        return properties.stream()
                         .filter(property -> propertyName.equals(property.get("name")))
                         .findFirst()
                         .orElseThrow(() -> new AssertionError("no property [" + propertyName + "] on [" + entityName + "]"));
    }
}
