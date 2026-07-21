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
 * Verifies the {@code waits} and {@code timerLoaders} glue collections: the correlation coordinates
 * of a {@code wait} step's listener (message name, event topic binding, the {@code via:}
 * back-reference to the ProcessId-carrying trigger entity) and the expire date loader's re-read
 * expression for a user task's {@code expire:} boundary timer.
 */
class GlueWaitsTest {

    private static final String YAML = """
            name: services
            entities:
              - name: Case
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
                  - { name: validUntil, type: date }
                  - { name: dueAt, type: timestamp }
                relations:
                  - { name: messages, kind: oneToMany, to: CaseMessage }
              - name: CaseMessage
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
                  - { name: internal, type: integer }
                relations:
                  - { name: case, kind: manyToOne, to: Case, composition: true }
            processes:
              - name: CaseHandling
                trigger: { onCreate: Case }
                steps:
                  - name: work
                    kind: userTask
                    args:
                      assignee: agent
                      timeout: { after: P3D, then: remind }
                      expire: { until: validUntil, then: markExpired }
                      next: done
                  - { name: remind,      kind: serviceTask, args: { next: end } }
                  - { name: markExpired, kind: serviceTask, args: { setField: subject, value: EXPIRED, next: end } }
                  - { name: awaitReply,  kind: wait, args: { onCreate: CaseMessage, via: case, when: "internal == 0", next: done } }
                  - { name: done,        kind: end }
            """;

    @Test
    void rendersTheWaitCorrelation() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> waits = GlueIntentGenerator.buildWaitsForTest(model);
        assertEquals(1, waits.size());
        Map<String, Object> wait = waits.get(0);

        assertEquals("CaseHandling", wait.get("process"));
        assertEquals("CaseHandlingAwaitReply", wait.get("className"));
        assertEquals("CaseHandlingAwaitReply", wait.get("messageName"));
        assertEquals("CaseMessage", wait.get("eventEntity"));
        assertEquals("Id", wait.get("eventKeyProperty"));
        assertEquals("", wait.get("topicSuffix"));
        assertEquals("java.util.Objects.equals(entity.Internal, 0)", wait.get("guardExpression"));
        assertEquals("Case", wait.get("viaFkProperty"));
        assertEquals("Case", wait.get("parentEntity"));
    }

    @Test
    void aDirectWaitOnTheTriggerEntityHasNoVia() {
        String yaml =
                YAML.replace("{ onCreate: CaseMessage, via: case, when: \"internal == 0\", next: done }", "{ onUpdate: Case, next: done }");
        Map<String, Object> wait = GlueIntentGenerator.buildWaitsForTest(IntentParser.parse(yaml))
                                                      .get(0);
        assertEquals("Case", wait.get("eventEntity"));
        assertEquals("-updated", wait.get("topicSuffix"));
        // Blank via -> the template's direct branch: the event record itself carries the ProcessId.
        assertEquals("", wait.get("viaFkProperty"));
        assertEquals("true", wait.get("guardExpression"));
    }

    @Test
    void rendersTheExpireDateLoader() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> loaders = GlueIntentGenerator.buildTimerLoadersForTest(model);
        assertEquals(1, loaders.size());
        Map<String, Object> loader = loaders.get(0);

        assertEquals("CaseHandling", loader.get("process"));
        assertEquals("LoadCaseHandlingWorkExpire", loader.get("handler"));
        assertEquals("Case", loader.get("ownerEntity"));
        assertEquals("Id", loader.get("ownerKeyProperty"));
        assertEquals("intValue", loader.get("ownerKeyAccessor"));
        assertEquals("__workExpireDate", loader.get("variable"));
        // A `date` field names the LAST valid day - the timer arms at the start of the day after it.
        String due = String.valueOf(loader.get("dueExpression"));
        assertTrue(due.contains("entity.ValidUntil == null"), due);
        assertTrue(due.contains("plusDays(1).atStartOfDay"), due);
        assertTrue(due.contains("9999-12-31"), due);
    }

    @Test
    void aTimestampExpireFieldArmsAtItsInstant() {
        String yaml = YAML.replace("until: validUntil", "until: dueAt");
        Map<String, Object> loader = GlueIntentGenerator.buildTimerLoadersForTest(IntentParser.parse(yaml))
                                                        .get(0);
        String due = String.valueOf(loader.get("dueExpression"));
        assertTrue(due.contains("java.util.Date.from(entity.DueAt)"), due);
        assertTrue(!due.contains("plusDays"), due);
    }

    @Test
    void aTimeoutOnlyTaskNeedsNoLoader() {
        String yaml = YAML.replace("expire: { until: validUntil, then: markExpired }\n          ", "");
        assertEquals(0, GlueIntentGenerator.buildTimerLoadersForTest(IntentParser.parse(yaml))
                                           .size());
    }
}
