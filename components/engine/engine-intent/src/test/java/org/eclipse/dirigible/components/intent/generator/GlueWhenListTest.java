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

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The rendered guard of a {@code when} list (issue #6957): every term of the AND evaluates against
 * the RE-LOADED source - the numeric status comparison exactly as the scalar always did, the string
 * terms null-safely via {@code Objects.equals} - and a string term against a field the developer
 * can edit gets an actionable warning, because such a guard means a UI edit silently changes which
 * automations fire.
 */
class GlueWhenListTest {

    private static final String HEAD = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: note,       type: string }
                  - { name: resolution, type: string, readOnly: true }
                  - { name: verdict,    type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: FineLog
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
            seeds:
              - name: fine-statuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: IDENTIFIED }
            generates:
              - name: log-identified
                from: Fine
                to: FineLog
                forEntity: Fine
            """;

    private static IntentGenerationContext context() {
        return new IntentGenerationContext(null, "/users/admin/workspace/fines", "fines", "workspace", "fines", null);
    }

    @Test
    void aScalarWhenRendersTheGuardItAlwaysDid() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    event: { onTransition: Fine, when: "Status == IDENTIFIED" }
                    map:
                      Fine: id
                """), context())
                                                   .get(0);

        assertEquals("Status", g.get("guardProperty"));
        assertEquals("2", g.get("guardValue"));
        assertEquals("source.Status != null && source.Status == 2", g.get("guardCondition"));
    }

    @Test
    void aListWhenRendersTheConjunctionOverTheReloadedSource() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == IDENTIFIED"
                        - "resolution == found"
                    map:
                      Fine: id
                """), context())
                                                   .get(0);

        assertEquals("Status", g.get("guardProperty"), "the status term still names the javadoc's status");
        assertEquals("2", g.get("guardValue"));
        assertEquals("source.Status != null && source.Status == 2 && java.util.Objects.equals(source.Resolution, \"found\")",
                g.get("guardCondition"));
    }

    @Test
    void anExcludingStringTermRendersNegated() {
        Map<String, Object> g = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "resolution != 'ambiguous'"
                    map:
                      Fine: id
                """), context())
                                                   .get(0);

        assertEquals("source.Status != null && source.Status == 2 && !java.util.Objects.equals(source.Resolution, \"ambiguous\")",
                g.get("guardCondition"));
    }

    @Test
    void guardingAnEditableFieldWarnsWithTheOneLineFix() {
        IntentGenerationContext context = context();

        GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "verdict == found"
                    map:
                      Fine: id
                """), context);

        assertTrue(context.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("[verdict]") && issue.contains("readOnly")),
                "an editable guard field must be called out: " + context.getIssues());
    }

    @Test
    void guardingTheReadOnlyTraceFieldDoesNotWarn() {
        IntentGenerationContext context = context();

        GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "resolution == found"
                    map:
                      Fine: id
                """), context);

        assertTrue(context.getIssues()
                          .isEmpty(),
                "a platform-written trace field is the intended guard: " + context.getIssues());
    }

    @Test
    void aProcessTriggerListWhenRendersTheJoinedGuardExpression() {
        List<Map<String, Object>> triggers = GlueIntentGenerator.buildTriggersForTest(IntentParser.parse(HEAD + """
                    event: { onCreate: Fine }
                    map:
                      Fine: id
                processes:
                  - name: ManualIdentification
                    trigger:
                      onTransition: Fine
                      when:
                        - "Status == IDENTIFIED"
                        - "resolution == found"
                    steps:
                      - { name: done, kind: end }
                """));

        assertEquals("java.util.Objects.equals(entity.Status, 2) && java.util.Objects.equals(entity.Resolution, \"found\")", triggers.get(0)
                                                                                                                                     .get("guardExpression"));
    }
}
