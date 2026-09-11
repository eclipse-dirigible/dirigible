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
 * Declarative step resilience - dirigible #6762 and #7056: {@code retry: { count, every }} and
 * {@code onError: <step | end>} on a {@code delegate:} or a {@code notify:} service task, and the
 * {@code {error}} placeholder a {@code setField} on the error route reads. The parser must reject a
 * malformed retry cycle, a dangling error route, resilience on a service-task shape it does not
 * apply to, resilience on a fan-out send (which never fails, so neither key could fire), and an
 * {@code {error}} nothing would ever populate. Also the routing self-loops that spin a
 * wait-state-free flow: a step's {@code next} and a decision's {@code then}/{@code else} naming
 * their own step.
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

    /**
     * A step whose {@code next} names itself would emit a self-targeting sequence flow the engine spins
     * on; it names an existing step, so only a dedicated check catches it (dirigible #7226).
     */
    @Test
    void aNextTargetingItselfIsRejected() {
        String issue = assertIssue(YAML.replace("onError: recordFailure, next: done", "onError: recordFailure, next: provisionApp"),
                "`next` targets itself - a self-loop that never advances");
        assertTrue(issue.contains("step [provisionApp]"), "the message must locate the step: " + issue);
    }

    /**
     * The same spin one key over: an exclusive gateway has no wait state, so a decision branching back
     * to itself emits a self-targeting sequence flow Flowable spins on. It names an existing step, so
     * only a dedicated check catches it (dirigible #7292).
     */
    @Test
    void aDecisionThenTargetingItselfIsRejected() {
        String issue = assertIssue(withDecision("rated", "settle"), "`then` targets itself - a self-loop that never advances");
        assertTrue(issue.contains("decision [rated]"), "the message must locate the decision: " + issue);
    }

    @Test
    void aDecisionElseTargetingItselfIsRejected() {
        String issue = assertIssue(withDecision("settle", "rated"), "`else` targets itself - a self-loop that never advances");
        assertTrue(issue.contains("decision [rated]"), "the message must locate the decision: " + issue);
    }

    /**
     * A branch back through a wait state is a legitimate cycle - only the direct self-edge is refused.
     */
    @Test
    void aDecisionBranchingBackThroughAWaitStateIsAccepted() {
        assertDoesNotThrow(() -> IntentParser.parse(withDecision("reconsider", "settle")));
    }

    /** The showcase plus a decision whose two branches take the given targets. */
    private static String withDecision(String thenTarget, String elseTarget) {
        return YAML + """
                  - name: Review
                    trigger: { onCreate: TenantApplication }
                    steps:
                      - { name: reconsider, kind: userTask, args: { assignee: owner, next: rated } }
                      - { name: rated,      kind: decision, args: { if: "id > 0", then: %s, else: %s } }
                      - { name: settle,     kind: serviceTask, args: { setField: failureMessage, value: settled, next: end } }
                """.formatted(thenTarget, elseTarget);
    }

    /** The literal `end` routes the failure to the process end, like a decision branch. */
    @Test
    void onErrorMayRouteToEnd() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML.replace("value: \"{error}\"", "value: failed")
                                                        .replace("onError: recordFailure }", "onError: end }")
                                                        .replace("onError: recordFailure,", "onError: end,")));
    }

    /**
     * A bare service task (its work a hand-written {@code custom.<Step>} handler) is not one of the two
     * shapes the keys apply to; the message names both, since that is what the author's next move is.
     */
    @Test
    void resilienceOnNeitherADelegateNorANotifyIsRejected() {
        String yaml = YAML.replace("delegate: custom.SchemaProvisioner, produces: [dbPassword], ", "");
        String issue = assertIssue(yaml, "declares retry but is neither a `delegate:` nor a `notify:` service task");
        assertTrue(issue.contains("applies to those two shapes"), "the message must name the two supported shapes: " + issue);
        assertIssue(yaml, "declares onError but is neither a `delegate:` nor a `notify:` service task");
    }

    /**
     * The #7056 widening: a send's generated handler fails the task on a delivery error, so the send
     * may now say what happens next instead of leaving that failure to a dead-letter incident.
     */
    @Test
    void aSendMayDeclareItsOwnResilience() {
        assertDoesNotThrow(() -> IntentParser.parse(withSendStep("retry: { count: 1, every: PT5S }, onError: recordFailure, ")));
    }

    /**
     * A setter step is refused, and the message says why rather than only that it is: a check-gated
     * status write is emitted without the async boundary on purpose (#7014 / #7063) so its refusal
     * reaches the person who acted - routing it away would take that 400 out of the Inbox.
     */
    @Test
    void resilienceOnASetterStepIsRejected() {
        String issue = assertIssue(
                YAML.replace("setField: failureMessage, value: \"{error}\", next: markFailed",
                        "setField: failureMessage, value: failed, next: markFailed, retry: { count: 1, every: PT5S }"),
                "declares retry but is neither a `delegate:` nor a `notify:` service task");
        assertTrue(issue.contains("refused synchronously to the person who acted"),
                "the message must state why a status write is not routed away: " + issue);
    }

    /**
     * A fan-out send is per-row fail-soft by construction - it never fails the task - so a declared
     * cycle could not fire and a boundary could not be reached. Refused rather than authored and
     * silently inert, with the observable alternative named.
     */
    @Test
    void resilienceOnAFanOutSendIsRejected() {
        String issue = assertIssue(fanOutSend("retry: { count: 1, every: PT5S }, "), "declares retry on a fan-out notify (forEach)");
        assertTrue(issue.contains("`outcome:`") && issue.contains("onNotifyFailed"),
                "the message must name the observable alternative: " + issue);
        assertIssue(fanOutSend("onError: recordFailure, "), "declares onError on a fan-out notify (forEach)");
    }

    /** The fan-out send itself stays authorable - only its resilience keys are refused. */
    @Test
    void aFanOutSendWithoutResilienceKeysParses() {
        assertDoesNotThrow(() -> IntentParser.parse(fanOutSend("")));
    }

    /** The showcase plus a non-fan-out send step carrying the given resilience keys. */
    private static String withSendStep(String resilienceKeys) {
        return YAML.replace("      - { name: recordFailure,",
                "      - { name: notifyOwner, kind: serviceTask, args: { notify: { to: owner@example.com, subject: \"Tenant provisioned\","
                        + " body: \"Ready.\" }, " + resilienceKeys + "next: done } }\n      - { name: recordFailure,");
    }

    /** The showcase plus a contacts child and a send that fans out over it, carrying the given keys. */
    private static String fanOutSend(String resilienceKeys) {
        return YAML.replace("processes:", """
                  - name: TenantContact
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: email, type: string }
                    relations:
                      - { name: tenant, kind: manyToOne, to: TenantApplication }
                processes:""")
                   .replace("      - { name: recordFailure,",
                           "      - { name: notifyContacts, kind: serviceTask, args: { notify: { forEach: TenantContact, to: email,"
                                   + " subject: \"Provisioned\", body: \"Ready.\" }, " + resilienceKeys + "next: done } }\n"
                                   + "      - { name: recordFailure,");
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
