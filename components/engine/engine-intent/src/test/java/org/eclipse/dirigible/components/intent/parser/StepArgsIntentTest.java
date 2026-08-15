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

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The map-shaped half of "an unknown key is an error" - dirigible #6749, the follow-up to #6541.
 *
 * <p>
 * A step's {@code args:}, a process {@code trigger:}, a glue {@code event:} binding and the maps
 * nested inside them have no typed class to reflect a key set off, so they were the one family the
 * #6748 walk could not cover: an invented or mis-cased key was accepted and discarded, the BPMN was
 * emitted, the task existed, and only the behaviour the author asked for was missing.
 *
 * <p>
 * Two shapes of mistake are caught for a step: a key no kind knows (a typo) and a key that belongs
 * to another kind (a misplacement) - the second is the same silent drop, since the step never reads
 * it.
 */
class StepArgsIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: validUntil, type: date }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            forms:
              - { name: ApproveInvoice, forEntity: Invoice, fields: [validUntil], actions: [approve] }
            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: manager, form: ApproveInvoice } }
                  - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                  - { name: done, kind: end }
            """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void anInventedArgIsRejectedAndNamesTheNearestOne() {
        String issue = assertIssue(YAML.replace("assignee: manager", "assigne: manager"), "declares unknown arg [assigne]");
        assertTrue(issue.contains("step [approve]"), "the message must locate the step: " + issue);
        assertTrue(issue.contains("did you mean [assignee]?"), "the message must name the nearest arg: " + issue);
    }

    @Test
    void aMisCasedArgIsRejected() {
        String issue = assertIssue(YAML.replace("form: ApproveInvoice", "Form: ApproveInvoice"), "declares unknown arg [Form]");
        assertTrue(issue.contains("case-sensitive"), "a pure case slip must say so: " + issue);
    }

    /** A key that exists, on the wrong kind: read by nothing, so it does nothing. */
    @Test
    void anArgOfAnotherKindIsRejectedAndNamesTheKindItBelongsTo() {
        String issue = assertIssue(YAML.replace("assignee: manager", "if: \"1 == 1\""), "declares arg [if]");
        assertTrue(issue.contains("but is a userTask"), "the message must name the step's kind: " + issue);
        assertTrue(issue.contains("a decision argument"), "the message must name where the arg belongs: " + issue);
    }

    @Test
    void aWaitArgOnAServiceTaskIsRejected() {
        assertIssue(YAML.replace("setRelationField: Status, value: 2, next: done", "via: Invoice, next: done"), "declares arg [via]");
    }

    /**
     * The keys whose misplacement already has a validator of its own keep that message - and only that
     * one, so the author is not told the same thing twice in two different registers.
     */
    @Test
    void aMisplacedSetFieldKeepsItsOwnMessageOnly() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("assignee: manager, form: ApproveInvoice", "setField: validUntil, value: x")));
        List<String> issues = ex.getIssues();
        assertTrue(issues.stream()
                         .anyMatch(i -> i.contains("uses setField but is not a serviceTask")),
                "the dedicated message must still be the one reported: " + issues);
        assertTrue(issues.stream()
                         .noneMatch(i -> i.contains("declares arg [setField]")),
                "the generic misplacement line must not double up: " + issues);
    }

    @Test
    void anUnknownKeyInsideABoundaryTimerIsRejected() {
        String yaml = YAML.replace("args: { assignee: manager, form: ApproveInvoice }",
                "args: { assignee: manager, form: ApproveInvoice, timeout: { afterr: P3D, then: done } }");
        String issue = assertIssue(yaml, "timeout declares unknown key [afterr]");
        assertTrue(issue.contains("did you mean [after]?"), "the message must name the nearest key: " + issue);
    }

    @Test
    void anUnknownKeyInsideAStepNotifyBlockIsRejected() {
        String yaml = YAML.replace("args: { setRelationField: Status, value: 2, next: done }",
                "args: { notify: { to: ops@example.com, subjekt: Approved, body: Done }, next: done }");
        assertIssue(yaml, "notify declares unknown key [subjekt]");
    }

    /**
     * A delegate's `fields:` are the delegate's own names - the DSL has no vocabulary to check them.
     */
    @Test
    void aDelegatesInjectedFieldsStayOpaque() {
        String yaml = YAML.replace("args: { setRelationField: Status, value: 2, next: done }",
                "args: { delegate: custom.sales.Numbering, fields: { type: \"Sales Invoice\", anything: 1 }, next: done }");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    @Test
    void anUnknownKeyInTheProcessTriggerIsRejected() {
        String issue = assertIssue(YAML.replace("trigger: { onCreate: Invoice }", "trigger: { onCreate: Invoice, businesskey: id }"),
                "unknown key [businesskey]");
        assertTrue(issue.contains("processes[InvoiceApproval].trigger"), "the message must locate the key: " + issue);
        assertTrue(issue.contains("did you mean [businessKey]?"), "the message must name the nearest key: " + issue);
    }

    @Test
    void anUnknownKeyInTheAbortOnBlockIsRejected() {
        assertIssue(
                YAML.replace("trigger: { onCreate: Invoice }", "trigger: { onCreate: Invoice }\n    abortOn: { statuses: [2], then: end }"),
                "unknown key [statuses]");
    }

    @Test
    void anUnknownKeyInAGlueEventBindingIsRejected() {
        String yaml = YAML + """
                notifications:
                  - name: invoiceCreated
                    event: { onCraete: Invoice }
                    to: ops@example.com
                    subject: New invoice
                    body: A new invoice was created.
                """;
        String issue = assertIssue(yaml, "unknown key [onCraete]");
        assertTrue(issue.contains("notifications[invoiceCreated].event"), "the message must locate the key: " + issue);
        assertTrue(issue.contains("did you mean [onCreate]?"), "the message must name the nearest key: " + issue);
    }

    @Test
    void anUnknownKeyInAStepEventBindingIsRejected() {
        String yaml = YAML + """
                notifications:
                  - name: approved
                    event: { onStepCompleted: { process: InvoiceApproval, stepp: approve } }
                    to: ops@example.com
                    subject: Approved
                    body: The invoice was approved.
                """;
        String issue = assertIssue(yaml, "unknown key [stepp]");
        assertTrue(issue.contains("event.onStepCompleted"), "the message must locate the nested key: " + issue);
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
