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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A status write a {@code checks:} gate stands in front of runs in the transaction of the action
 * that reached it, so its rejection travels back to the person who acted instead of dead-lettering
 * as a background process incident (issue #7014). Every other service task keeps its async
 * boundary.
 */
class CheckGateBpmnTest {

    private static final String YAML =
            """
                    name: billing
                    entities:
                      - name: InvoiceStatus
                        kind: setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: Customer
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: rating, type: integer }
                      - name: Invoice
                        checks:
                          - { kind: itemsMin, count: 1, status: 2, message: "Invoice needs at least one line" }
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: note, type: string, length: 200 }
                        relations:
                          - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                          - { name: Customer, kind: manyToOne, to: Customer }
                      - name: InvoiceItem
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: quantity, type: decimal }
                        relations:
                          - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                    processes:
                      - name: InvoiceApproval
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: review,  kind: userTask,    args: { assignee: clerk, form: ApproveInvoice, next: approve } }
                          - { name: approve, kind: serviceTask, args: { setRelationField: Status, value: 2, next: archive } }
                          - { name: archive, kind: serviceTask, args: { setRelationField: Status, value: 3, next: done } }
                          - { name: done,    kind: end }
                      - name: InvoiceIssue
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: issue, kind: userTask, args: { assignee: clerk, form: ApproveInvoice, setRelationField: Status, value: 2, next: done } }
                          - { name: done,  kind: end }
                      - name: InvoiceHandover
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: hand,     kind: userTask, args: { assignee: clerk, form: ApproveInvoice, next: enrich } }
                          - { name: enrich,   kind: serviceTask, args: { delegate: custom.billing.Enrich, next: settle } }
                          - { name: settle,   kind: serviceTask, args: { setRelationField: Status, value: 2, next: over } }
                          - { name: over,     kind: end }
                      - name: InvoicePosting
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: post,     kind: userTask, args: { assignee: clerk, form: DecideInvoice } }
                          - { name: decide,   kind: decision, args: { if: "action == 'approve'", then: apply, else: reject } }
                          - name: apply
                            kind: serviceTask
                            args:
                              delegate: custom.billing.Post
                              next: activate
                          - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                          - { name: reject,   kind: serviceTask, args: { setRelationField: Status, value: 8, next: done } }
                          - { name: done,     kind: end }
                      - name: InvoiceRetrying
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: send,     kind: userTask, args: { assignee: clerk, form: ApproveInvoice, next: transmit } }
                          - name: transmit
                            kind: serviceTask
                            args:
                              delegate: custom.billing.Transmit
                              retry: { count: 3, every: PT30S }
                              next: settle
                          - { name: settle,   kind: serviceTask, args: { setRelationField: Status, value: 2, next: over } }
                          - { name: over,     kind: end }
                      - name: InvoiceDecision
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: review,   kind: userTask, args: { assignee: clerk, form: DecideInvoice } }
                          - { name: decide,   kind: decision, args: { if: "action == 'approve'", then: rated, else: reject } }
                          - { name: rated,    kind: decision, args: { if: "Customer.rating > 0", then: activate, else: reject } }
                          - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                          - { name: reject,   kind: serviceTask, args: { setRelationField: Status, value: 8, next: done } }
                          - { name: done,     kind: end }
                      - name: InvoiceConverge
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: review,   kind: userTask, args: { assignee: clerk, form: DecideInvoice } }
                          - { name: decide,   kind: decision, args: { if: "action == 'approve'", then: activate, else: rated } }
                          - { name: rated,    kind: decision, args: { if: "Customer.rating > 0", then: activate, else: reject } }
                          - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                          - { name: reject,   kind: serviceTask, args: { setRelationField: Status, value: 8, next: done } }
                          - { name: done,     kind: end }
                      - name: InvoiceTimeout
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: review,   kind: userTask, args: { assignee: clerk, form: ApproveInvoice, next: done, timeout: { after: PT24H, then: escalate } } }
                          - { name: escalate, kind: decision, args: { if: "Customer.rating > 0", then: activate, else: done } }
                          - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                          - { name: done,     kind: end }
                    forms:
                      - { name: ApproveInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [approve] }
                      - { name: DecideInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [approve, reject] }
                    permissions:
                      - { role: Clerk, description: Clerk, can: [Invoice:read] }
                    """;

    private static String bpmn(String process) {
        IntentModel model = IntentParser.parse(YAML);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/" + process + ".bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }

    private static void assertSynchronous(String bpmn, String id) {
        assertTrue(bpmn.contains("<serviceTask id=\"" + id + "\" name=\"" + name(bpmn, id) + "\" flowable:delegateExpression="),
                "[" + id + "] must run in the completing transaction (no flowable:async) in:\n" + bpmn);
    }

    private static void assertAsynchronous(String bpmn, String id) {
        assertTrue(bpmn.contains("<serviceTask id=\"" + id + "\" name=\"" + name(bpmn, id) + "\" flowable:async=\"true\""),
                "[" + id + "] must keep its async boundary in:\n" + bpmn);
    }

    private static void assertSynchronousDelegate(String bpmn, String id) {
        assertTrue(bpmn.contains("<serviceTask id=\"" + id + "\" name=\"" + name(bpmn, id) + "\" flowable:class="),
                "[" + id + "] must run in the completing transaction (no flowable:async) in:\n" + bpmn);
    }

    private static void assertAsynchronousDelegate(String bpmn, String id) {
        assertTrue(bpmn.contains("<serviceTask id=\"" + id + "\" name=\"" + name(bpmn, id) + "\" flowable:async=\"true\" flowable:class="),
                "[" + id + "] must keep its async boundary in:\n" + bpmn);
    }

    /** The emitted {@code name} of a service task, so an assertion pins the whole opening tag. */
    private static String name(String bpmn, String id) {
        String marker = "<serviceTask id=\"" + id + "\" name=\"";
        int from = bpmn.indexOf(marker);
        assertTrue(from >= 0, "no service task [" + id + "] in:\n" + bpmn);
        int start = from + marker.length();
        return bpmn.substring(start, bpmn.indexOf('"', start));
    }

    @Test
    void aCheckGatedStatusSetRunsInTheCompletingTransaction() {
        String bpmn = bpmn("InvoiceApproval");

        assertSynchronous(bpmn, "approve");
    }

    @Test
    void anUngatedStatusSetKeepsItsAsyncBoundary() {
        String bpmn = bpmn("InvoiceApproval");

        assertAsynchronous(bpmn, "archive");
    }

    @Test
    void aGatedSetterBehindADecisionCarriesTheWriterIntoTheTransaction() {
        String bpmn = bpmn("InvoiceDecision");

        // The shape every approve/reject flow has (issue #7063): the completing user task falls through
        // a decision into the service task that sets the gated status. An async writer between them
        // commits the completion, and the gate then dead-letters instead of refusing the approver.
        assertSynchronous(bpmn, "invoiceDecisionReviewWrite");
        // The resolver the second decision needs sits between the task and the gate too - and it is a
        // service task like any other, so its boundary would commit the completion just the same.
        assertSynchronous(bpmn, "resolveCustomerRating");
        assertSynchronous(bpmn, "activate");
    }

    @Test
    void aGateReachedThroughTwoDecisionRoutesTakesBothOutOfAsync() {
        String bpmn = bpmn("InvoiceConverge");

        // The gate is one hop away on the first route (decide -> activate) and two on the second
        // (decide -> rated -> activate). The approver who takes the second one is waiting on the gate
        // just as much, so the resolver the second decision needs may not commit the completion first
        // (issue #7139).
        assertSynchronous(bpmn, "invoiceConvergeReviewWrite");
        assertSynchronous(bpmn, "resolveCustomerRating");
        assertSynchronous(bpmn, "activate");
    }

    @Test
    void aGateBehindATimerBranchKeepsTheAsyncBoundary() {
        String bpmn = bpmn("InvoiceTimeout");

        // The gate is reachable only through the task's `timeout:` boundary branch - taken when the
        // wait expires, with nobody's action to refuse - so neither the branch nor the writer the task
        // leaves behind loses its boundary. The gated set itself stays synchronous either way (#7014):
        // it is the write the gate stands in front of.
        assertAsynchronous(bpmn, "invoiceTimeoutReviewWrite");
        assertAsynchronous(bpmn, "resolveCustomerRating");
        assertSynchronous(bpmn, "activate");
    }

    @Test
    void anUngatedBranchOfTheSameDecisionKeepsItsAsyncBoundary() {
        String bpmn = bpmn("InvoiceDecision");

        assertAsynchronous(bpmn, "reject");
    }

    @Test
    void authoredWorkBetweenTheTaskAndTheGateRunsInTheCompletingTransaction() {
        String bpmn = bpmn("InvoiceHandover");

        // A delegate between the completing task and the gate used to keep its boundary, on the
        // reasoning that its own completion is what the person waited for. Measured, that boundary
        // commits the completion and the gate then refuses a detached job: 200 with an empty body, the
        // task consumed, the document never moved (issue #7371). The whole stretch is one transaction.
        assertSynchronous(bpmn, "invoiceHandoverHandWrite");
        assertSynchronousDelegate(bpmn, "enrich");
        assertSynchronous(bpmn, "settle");
    }

    @Test
    void aGatedSetterBehindADecisionAndACustomDelegateRunsInTheCompletingTransaction() {
        String bpmn = bpmn("InvoicePosting");

        // base-inventory's six posting flows, exactly: userTask -> decision -> custom posting delegate
        // -> gated status setter. The delegate is bound with flowable:class rather than ${JavaTask}, so
        // it is emitted on its own path - which used to hard-code flowable:async (issue #7371).
        assertSynchronousDelegate(bpmn, "apply");
        assertSynchronous(bpmn, "activate");
        // The ungated branch of the same decision is nobody's gate, and keeps its boundary.
        assertAsynchronous(bpmn, "reject");
    }

    @Test
    void aRetryingStepBetweenTheTaskAndTheGateKeepsItsAsyncBoundary() {
        String bpmn = bpmn("InvoiceRetrying");

        // The one step the completing transaction may not swallow: a Flowable failed-job retry cycle
        // re-runs a JOB, and there is no job without the boundary. The gate behind it still refuses
        // into a dead-letter incident - the generator logs that rather than dropping the declared
        // re-attempts silently.
        assertAsynchronousDelegate(bpmn, "transmit");
        assertAsynchronous(bpmn, "invoiceRetryingSendWrite");
        // The gated set itself is synchronous either way (#7014): it is the write the gate stands
        // in front of.
        assertSynchronous(bpmn, "settle");
    }

    @Test
    void aGatedSetterOnAUserTaskCarriesTheWriterBeforeItIntoTheTransaction() {
        String bpmn = bpmn("InvoiceIssue");

        // The setter runs as a delegate inserted after the task, behind the writer that persists the
        // reviewer's edits - an async writer would commit the completion before the gate is reached.
        assertSynchronous(bpmn, "invoiceIssueIssueWrite");
        assertSynchronous(bpmn, "invoiceIssueIssue");
    }
}
