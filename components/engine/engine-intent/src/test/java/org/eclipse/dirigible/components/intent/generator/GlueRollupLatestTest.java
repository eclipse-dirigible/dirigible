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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code rollup op: latest} glue: create/update/delete handlers over the child, and
 * the latest aggregate block that copies the {@code of} value of the child row with the greatest
 * {@code by} date onto the parent field (the "keep Currency.rate equal to the latest CurrencyRate"
 * shape).
 */
class GlueRollupLatestTest {

    private static final String YAML = """
            name: fx
            entities:
              - name: Currency
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, unique: true, required: true, length: 3 }
                  - { name: rate, type: decimal, precision: 18, scale: 6 }
                relations:
                  - { name: rates, kind: oneToMany, to: CurrencyRate }
              - name: CurrencyRate
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date, required: true }
                  - { name: rate, type: decimal, precision: 18, scale: 6, required: true }
                relations:
                  - { name: Currency, kind: manyToOne, to: Currency, composition: true, required: true }
            rollups:
              - { name: latestRate, entity: CurrencyRate, via: Currency, field: rate, op: latest, of: rate, by: date }
            """;

    @Test
    void rendersTheLatestRollupHandlers() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);
        // create + update + delete (a new/edited/removed rate can change which row is latest).
        assertEquals(3, rollups.size(), "latest must recompute on create, update and delete");
        Map<String, Object> create = rollups.get(0);
        assertEquals("latest", create.get("op"));
        assertEquals("Rate", create.get("countField")); // parent field
        assertEquals("Rate", create.get("ofField")); // child value copied
        assertEquals("Date", create.get("byField")); // child ordering field
        assertEquals("Currency", create.get("fkProperty"));
        assertEquals("CurrencyRate", create.get("childEntity"));
        assertEquals("Currency", create.get("parentEntity"));
        assertTrue(rollups.stream()
                          .anyMatch(r -> String.valueOf(r.get("topicSuffix"))
                                               .equals("-updated")),
                "latest must have an update handler");
    }
}
