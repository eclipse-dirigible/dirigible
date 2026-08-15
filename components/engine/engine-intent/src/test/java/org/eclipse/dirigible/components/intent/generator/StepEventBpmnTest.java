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

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Where a step-event emitter lands in the flow: an {@code onStepReached} emitter runs immediately
 * before the observed step (so the notification goes out the moment the execution arrives), an
 * {@code onStepCompleted} emitter immediately after it - behind the writer that persists the task's
 * edits, and carrying the step's routing so nothing can be bypassed.
 */
class StepEventBpmnTest {

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
                  - { name: status, type: string, length: 20 }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            processes:
              - name: LoanApproval
                trigger: { onCreate: Loan }
                steps:
                  - { name: librarianReview, kind: userTask,    args: { assignee: librarian, form: ApproveLoan, next: activate } }
                  - { name: activate,        kind: serviceTask, args: { setField: status, value: ACTIVE, next: done } }
                  - { name: done,            kind: end }
            forms:
              - { name: ApproveLoan, forEntity: Loan, fields: [note, status], editable: [note], actions: [approve] }
            notifications:
              - name: reviewPending
                event: { onStepReached: { process: LoanApproval, step: librarianReview } }
                to: member.email
                subject: "Loan {id} awaits review"
                body: "A librarian must approve it."
            integrations:
              - name: pushActivation
                event: { onStepCompleted: { process: LoanApproval, step: activate } }
                method: POST
                url: "@config:PARTNER_URL"
            permissions:
              - { role: Librarian, description: Librarian, can: [Loan:read] }
            """;

    private static String bpmn() {
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
                     .endsWith("/LoanApproval.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }

    private static void assertFlow(String bpmn, String source, String target) {
        assertTrue(bpmn.contains("sourceRef=\"" + source + "\" targetRef=\"" + target + "\""),
                "expected a flow " + source + " -> " + target + " in:\n" + bpmn);
    }

    @Test
    void aReachedEmitterRunsImmediatelyBeforeTheObservedStep() {
        String bpmn = bpmn();

        assertTrue(bpmn.contains("<serviceTask id=\"loanApprovalLibrarianReviewReached\""), "the emitter must be a service task");
        assertTrue(bpmn.contains("gen.events.library.LoanApprovalLibrarianReviewReached"), "bound to the generated delegate class");
        assertFlow(bpmn, "start", "loanApprovalLibrarianReviewReached");
        assertFlow(bpmn, "loanApprovalLibrarianReviewReached", "librarianReview");
        assertFalse(bpmn.contains("sourceRef=\"start\" targetRef=\"librarianReview\""), "the flow must run through the emitter");
    }

    @Test
    void aCompletedEmitterRunsAfterTheStepAndCarriesItsRouting() {
        String bpmn = bpmn();

        // The user task's edits are written first (the writer), then the flow continues to the next
        // step - the reached emitter of THIS task already fired, so nothing else is inserted here.
        assertFlow(bpmn, "librarianReview", "loanApprovalLibrarianReviewWrite");
        assertFlow(bpmn, "loanApprovalLibrarianReviewWrite", "activate");
        // The observed service task hands over to its emitter, which carries the step's `next: done`
        // so the routing cannot be bypassed.
        assertFlow(bpmn, "activate", "loanApprovalActivateCompleted");
        assertFlow(bpmn, "loanApprovalActivateCompleted", "end");
        assertFalse(bpmn.contains("sourceRef=\"activate\" targetRef=\"end\""), "the step must route through its emitter");
    }
}
