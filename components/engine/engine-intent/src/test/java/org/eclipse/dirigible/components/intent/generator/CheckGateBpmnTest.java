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
                      - name: Invoice
                        checks:
                          - { kind: itemsMin, count: 1, status: 2, message: "Invoice needs at least one line" }
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: note, type: string, length: 200 }
                        relations:
                          - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
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
                    forms:
                      - { name: ApproveInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [approve] }
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
    void aGatedSetterOnAUserTaskCarriesTheWriterBeforeItIntoTheTransaction() {
        String bpmn = bpmn("InvoiceIssue");

        // The setter runs as a delegate inserted after the task, behind the writer that persists the
        // reviewer's edits - an async writer would commit the completion before the gate is reached.
        assertSynchronous(bpmn, "invoiceIssueIssueWrite");
        assertSynchronous(bpmn, "invoiceIssueIssue");
    }
}
