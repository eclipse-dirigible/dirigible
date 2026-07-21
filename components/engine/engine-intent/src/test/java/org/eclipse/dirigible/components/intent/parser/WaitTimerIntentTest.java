/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validation of the {@code wait} step (park the process on an entity event) and the user-task
 * {@code timeout:}/{@code expire:} boundary timers.
 */
class WaitTimerIntentTest {

    private static final String YAML = """
            name: services
            entities:
              - name: Case
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
                  - { name: validUntil, type: date }
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
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void aDirectWaitOnTheTriggerEntityParsesWithoutVia() {
        String yaml =
                YAML.replace("{ onCreate: CaseMessage, via: case, when: \"internal == 0\", next: done }", "{ onUpdate: Case, next: done }");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    @Test
    void aWaitOnDeleteIsRejected() {
        String yaml = YAML.replace("onCreate: CaseMessage", "onDelete: CaseMessage");
        assertIssue(yaml, "cannot bind onDelete");
    }

    @Test
    void aWaitWithoutAnEventIsRejected() {
        String yaml = YAML.replace("onCreate: CaseMessage, via: case, when: \"internal == 0\", ", "");
        assertIssue(yaml, "must declare exactly one of onCreate/onUpdate");
    }

    @Test
    void aWaitOnAnUnknownEntityIsRejected() {
        String yaml = YAML.replace("onCreate: CaseMessage", "onCreate: Nope");
        assertIssue(yaml, "references unknown entity [Nope]");
    }

    @Test
    void aChildEventWaitWithoutViaIsRejected() {
        String yaml = YAML.replace("via: case, ", "");
        assertIssue(yaml, "must declare via");
    }

    @Test
    void aViaOnTheTriggerEntityItselfIsRejected() {
        String yaml = YAML.replace("{ onCreate: CaseMessage, via: case, when: \"internal == 0\", next: done }",
                "{ onUpdate: Case, via: case, next: done }");
        assertIssue(yaml, "must not declare via");
    }

    @Test
    void aViaThatIsNotAToOneRelationIsRejected() {
        String yaml = YAML.replace("via: case,", "via: sender,");
        assertIssue(yaml, "is not a manyToOne/oneToOne relation");
    }

    @Test
    void aWaitWithoutAProcessTriggerIsRejected() {
        String yaml = YAML.replace("    trigger: { onCreate: Case }\n", "");
        assertIssue(yaml, "needs a process trigger entity");
    }

    @Test
    void aTimerOnAServiceTaskIsRejected() {
        String yaml = YAML.replace("{ name: remind,      kind: serviceTask, args: { next: end } }",
                "{ name: remind,      kind: serviceTask, args: { next: end, timeout: { after: P1D, then: done } } }");
        assertIssue(yaml, "boundary timers attach to user tasks only");
    }

    @Test
    void aMalformedTimeoutDurationIsRejected() {
        String yaml = YAML.replace("after: P3D", "after: 3 days");
        assertIssue(yaml, "is not an ISO-8601 duration");
    }

    @Test
    void aTimeoutWithoutThenIsRejected() {
        String yaml = YAML.replace("timeout: { after: P3D, then: remind }", "timeout: { after: P3D }");
        assertIssue(yaml, "timeout must declare `then`");
    }

    @Test
    void aTimerThenToAnUnknownStepIsRejected() {
        String yaml = YAML.replace("then: markExpired", "then: nowhere");
        assertIssue(yaml, "`then` references unknown step [nowhere]");
    }

    @Test
    void anExpireUntilThatIsNotADateFieldIsRejected() {
        String yaml = YAML.replace("until: validUntil", "until: subject");
        assertIssue(yaml, "must be a date/timestamp field");
    }

    @Test
    void anExpireUntilOnAnUnknownFieldIsRejected() {
        String yaml = YAML.replace("until: validUntil", "until: nope");
        assertIssue(yaml, "is not a field of [Case]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains(expected),
                "expected issue containing [" + expected + "] but got: " + ex.getMessage());
    }
}
