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
 * A {@code number: { per: Company }} field whose Company relation carries {@code init: 1} allocates
 * in the default company's partition, never on the series' base row (#7101) - the second company's
 * partition is materialized from that base row, so numbering the default company there made both
 * companies share a counter (VAC0000009 for both).
 *
 * <p>
 * The .model half of that guarantee is the partition FK's own {@code dataDefaultValue}: the
 * generated repository assigns it as the first statement of {@code save()} (#7104), so the FK the
 * allocator reads a few lines below is already the value the row will carry. The number property
 * therefore carries no second copy of the init value (#7147) - one mechanism owns it, and a
 * duplicate could only drift from the one the row is written with.
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
    void thePartitionFkCarriesTheRelationsInitAsItsDefault() {
        Map<String, Object> fk = property(VACATIONS, "VacationRequest", "Company");
        assertEquals("1", fk.get("dataDefaultValue"), "the value save() defaults the FK to before the number is drawn");
        assertEquals("INTEGER", fk.get("dataType"),
                "a scalar type the repository's defaulting covers - it skips a date or a blob, whose DEFAULT is a SQL expression");
        assertEquals("Company", property(VACATIONS, "VacationRequest", "Number").get("numberPer"),
                "sanity: the number is partitioned by that relation");
    }

    @Test
    void theNumberCarriesNoSecondCopyOfTheDefault() {
        Map<String, Object> number = property(VACATIONS, "VacationRequest", "Number");
        assertNull(number.get("numberPerDefault"),
                "the FK is defaulted before the allocation runs - a copy of the init on the number could only drift from it");
    }

    @Test
    void anUnpartitionedNumberNamesNoRelation() {
        String yaml = VACATIONS.replace("per: Company, ", "");
        Map<String, Object> number = property(yaml, "VacationRequest", "Number");
        assertEquals("", number.get("numberPer"), "a tenant-wide series: one counter, no partition");
    }

    @Test
    void theIssueStampedNumberIsPartitionedTheSameWay() {
        // The issue path reads a row loaded from the database, so its fallback is live and rides the
        // numbering glue descriptor instead (NumberingInitPartitionTest); the .model half is the same.
        String yaml = VACATIONS.replace("stampOn: create", "stampOn: issue");
        assertEquals("Company", property(yaml, "VacationRequest", "Number").get("numberPer"));
        assertEquals("1", property(yaml, "VacationRequest", "Company").get("dataDefaultValue"));
        assertEquals("true", property(yaml, "VacationRequest", "Number").get("generatedUuid"), "sanity: still the UUID placeholder path");
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
