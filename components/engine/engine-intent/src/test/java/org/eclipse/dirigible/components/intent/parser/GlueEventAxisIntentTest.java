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
 * Validation of the two axes a declarative-glue entry may bind to - an entity lifecycle event or a
 * process step event - and of the inbound arrivals (HTTP path, queue/topic, polled folder).
 */
class GlueEventAxisIntentTest {

    private static final String YAML = """
            name: library
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Loan
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            processes:
              - name: LoanApproval
                trigger: { onCreate: Loan }
                steps:
                  - { name: librarianReview, kind: userTask,    args: { assignee: librarian, next: activate } }
                  - { name: activate,        kind: serviceTask, args: { setField: status, value: ACTIVE, next: decide } }
                  - { name: decide,          kind: decision,    args: { if: "status == 'ACTIVE'", then: done, else: done } }
                  - { name: done,            kind: end }
            notifications:
              - name: reviewPending
                event: { onStepReached: { process: LoanApproval, step: librarianReview } }
                to: member.email
                subject: "Loan {id} is waiting"
                body: "A librarian must approve it."
            integrations:
              - name: pushActivation
                event: { onStepCompleted: { process: LoanApproval, step: activate } }
                method: POST
                url: "@config:PARTNER_URL"
            inbound:
              - { name: leadHook,  path: /webhooks/loan, create: Loan }
              - { name: loanQueue, source: { queue: loans.inbound }, create: Loan }
              - { name: loanDrop,  source: { folder: /data/inbox/loans, cron: "0 */5 * * * ?" }, create: Loan }
            permissions:
              - { role: Librarian, description: Librarian, can: [Loan:read] }
            """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void aStepEventMustNameAKnownProcessAndStep() {
        assertTrue(issuesOf(YAML.replace("step: librarianReview", "step: noSuchStep")).contains("unknown step [noSuchStep]"));
        assertTrue(issuesOf(YAML.replace("process: LoanApproval, step: librarianReview", "process: NoSuchProcess, step: x")).contains(
                "unknown process [NoSuchProcess]"));
    }

    @Test
    void aStepEventNeedsAProcessAndAStep() {
        assertTrue(issuesOf(YAML.replace("{ process: LoanApproval, step: librarianReview }", "LoanApproval")).contains(
                "must name a process and a step"));
    }

    @Test
    void onlyATaskHasAMomentToObserve() {
        assertTrue(issuesOf(YAML.replace("step: activate }", "step: decide }")).contains("of kind [decision]"));
    }

    @Test
    void aStepEventNeedsTheProcessTriggerEntity() {
        assertTrue(issuesOf(YAML.replace("    trigger: { onCreate: Loan }\n", "")).contains("has no trigger entity"));
    }

    @Test
    void exactlyOneEventOfTheAxis() {
        String twoAxes = YAML.replace("event: { onStepReached: { process: LoanApproval, step: librarianReview } }",
                "event: { onCreate: Loan, onStepReached: { process: LoanApproval, step: librarianReview } }");
        assertTrue(issuesOf(twoAxes).contains("must declare exactly one of"));
    }

    @Test
    void anInboundArrivesExactlyOneWay() {
        assertTrue(issuesOf(YAML.replace("{ name: loanQueue, source: { queue: loans.inbound }, create: Loan }",
                "{ name: loanQueue, path: /x, source: { queue: loans.inbound }, create: Loan }")).contains(
                        "declares both a path and a source"));
        assertTrue(issuesOf(YAML.replace("{ name: loanQueue, source: { queue: loans.inbound }, create: Loan }",
                "{ name: loanQueue, create: Loan }")).contains("has no path and no source"));
        assertTrue(
                issuesOf(YAML.replace("source: { queue: loans.inbound }", "source: { queue: loans.inbound, topic: loans.feed }")).contains(
                        "exactly one of queue/topic/folder"));
    }

    @Test
    void aPolledFolderNeedsItsCron() {
        assertTrue(issuesOf(YAML.replace(", cron: \"0 */5 * * * ?\"", "")).contains("has no cron to poll it on"));
        assertTrue(issuesOf(
                YAML.replace("source: { queue: loans.inbound }", "source: { queue: loans.inbound, cron: \"0 0 * * * ?\" }")).contains(
                        "only a folder source polls on"));
    }

    private static String issuesOf(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getMessage();
    }
}
