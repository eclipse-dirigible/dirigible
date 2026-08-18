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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Declarative step resilience - dirigible #6762: {@code retry: { count, every }} and
 * {@code onError: <step | end>} on a delegate service task, and the {@code {error}} placeholder a
 * {@code setField} on the error route reads. The parser must reject a malformed retry cycle, a
 * dangling error route, resilience without a delegate (v1 - the runtime conversion lives on the
 * {@code flowable:class} path), and an {@code {error}} nothing would ever populate.
 */
class StepResilienceIntentTest {

    private static final String YAML =
            """
                    name: provisioning
                    entities:
                      - name: ProvisioningStatus
                        function: Setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: TenantApplication
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: failureMessage, type: string }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
                    processes:
                      - name: TenantProvisioning
                        trigger: { onCreate: TenantApplication }
                        vars:
                          - { name: dbPassword, clearAfter: provisionApp }
                        steps:
                          - { name: createSchema, kind: serviceTask, args: { delegate: custom.SchemaProvisioner, produces: [dbPassword], retry: { count: 3, every: PT30S }, onError: recordFailure } }
                          - { name: provisionApp, kind: serviceTask, args: { delegate: custom.AppProvisioner, uses: [dbPassword], retry: { count: 5, every: PT1M }, onError: recordFailure, next: done } }
                          - { name: recordFailure, kind: serviceTask, args: { setField: failureMessage, value: "{error}", next: markFailed } }
                          - { name: markFailed, kind: serviceTask, args: { setRelationField: Status, value: 3, next: end } }
                          - { name: done, kind: end }
                    """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void anUnknownRetryKeyIsRejectedAndNamesTheNearestOne() {
        String issue = assertIssue(YAML.replace("retry: { count: 3, every: PT30S }", "retry: { cout: 3, every: PT30S }"),
                "retry declares unknown key [cout]");
        assertTrue(issue.contains("did you mean [count]?"), "the message must name the nearest key: " + issue);
    }

    @Test
    void aNonMapRetryIsRejected() {
        assertIssue(YAML.replace("retry: { count: 3, every: PT30S }", "retry: 3"), "retry must be a map");
    }

    @Test
    void aMissingCountIsRejected() {
        assertIssue(YAML.replace("count: 3, every: PT30S", "every: PT30S"), "retry must declare `count`");
    }

    @Test
    void aFractionalCountIsRejected() {
        assertIssue(YAML.replace("count: 3", "count: 2.5"), "retry `count` [2.5] must be an integer >= 1");
    }

    @Test
    void aZeroCountIsRejected() {
        assertIssue(YAML.replace("count: 3", "count: 0"), "retry `count` [0] must be an integer >= 1");
    }

    @Test
    void aMissingEveryIsRejected() {
        assertIssue(YAML.replace("count: 3, every: PT30S", "count: 3"), "retry must declare `every`");
    }

    @Test
    void aMalformedEveryIsRejected() {
        assertIssue(YAML.replace("every: PT30S", "every: 30seconds"), "retry `every` [30seconds] is not an ISO-8601 duration");
    }

    @Test
    void anUnknownOnErrorTargetIsRejected() {
        String issue = assertIssue(YAML.replace("onError: recordFailure }", "onError: recordFailur }"),
                "`onError` references unknown step [recordFailur]");
        assertTrue(issue.contains("step [createSchema]"), "the message must locate the step: " + issue);
    }

    /** The literal `end` routes the failure to the process end, like a decision branch. */
    @Test
    void onErrorMayRouteToEnd() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("value: \"{error}\"", "value: failed")
                                                        .replace("onError: recordFailure }", "onError: end }")
                                                        .replace("onError: recordFailure,", "onError: end,")));
    }

    @Test
    void retryWithoutADelegateIsRejected() {
        String yaml = YAML.replace("delegate: custom.SchemaProvisioner, produces: [dbPassword], ", "");
        String issue = assertIssue(yaml, "declares retry but no delegate");
        assertTrue(issue.contains("delegate service tasks only"), "the message must state the v1 rule: " + issue);
        assertIssue(yaml, "declares onError but no delegate");
    }

    /** A misplaced retry keeps the by-kind vocabulary message and gets no second, blunter line. */
    @Test
    void retryOnANonServiceTaskIsRejectedByTheKindGate() {
        String yaml = YAML + """
                  - name: Review
                    trigger: { onCreate: TenantApplication }
                    steps:
                      - { name: check, kind: decision, args: { if: "id > 0", then: end, retry: { count: 1, every: PT10S } } }
                """;
        String issue = assertIssue(yaml, "declares arg [retry]");
        assertTrue(issue.contains("but is a decision"), "the message must name the step's kind: " + issue);
        assertTrue(issue.contains("a serviceTask argument"), "the message must name where the arg belongs: " + issue);
    }

    @Test
    void anErrorTokenNoOnErrorRouteReachesIsRejected() {
        String yaml = YAML.replace(", onError: recordFailure }", " }")
                          .replace(", onError: recordFailure,", ",");
        assertIssue(yaml, "setField value {error} is only resolvable on a step reachable from an onError route");
    }

    @Test
    void anErrorTokenMixedIntoALargerValueIsRejected() {
        assertIssue(YAML.replace("value: \"{error}\"", "value: \"Failed: {error}\""),
                "setField value [Failed: {error}] may use {error} only as the whole value");
    }

    /** A step the error route flows ON TO (via `next`) may read {@code {error}} too. */
    @Test
    void anErrorTokenDownstreamOfTheOnErrorTargetIsAccepted() {
        String yaml = YAML.replace("value: \"{error}\"", "value: failed")
                          .replace("setRelationField: Status, value: 3", "setField: failureMessage, value: \"{error}\"");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    private static String assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String issue = ex.getIssues()
                         .stream()
                         .filter(i -> i.contains(expected))
                         .findFirst()
                         .orElse(null);
        assertEquals(true, issue != null, "expected an issue containing [" + expected + "] but got " + ex.getIssues());
        return issue;
    }
}
