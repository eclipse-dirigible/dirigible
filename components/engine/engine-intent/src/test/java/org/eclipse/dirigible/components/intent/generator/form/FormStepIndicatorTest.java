/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the status-flow step-indicator metadata the {@link FormIntentGenerator} emits on a BPM
 * task form: the statuses THE FORM'S OWN FLOW walks ({@code steps}, in seed order) plus the
 * current-status variable ({@code statusVar}). A status the flow never writes is not a step of it
 * (issue #7085), a terminal (cancel/void/…) status is excluded, translation seeds are ignored, a
 * flow that writes no status at all falls back to the whole non-terminal nomenclature, and a
 * non-task form gets no steps. The first step is the status the flow ENTERS at, read from its
 * trigger - the relation's {@code init:} for an {@code onCreate}, the status a {@code when} guard
 * pins for an {@code onUpdate}/{@code onTransition}, and none at all for a bare one (issue #7239).
 */
class FormStepIndicatorTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: OrderStatus
                function: Setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: description, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: 1 }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [Status], actions: [confirm] }
              - { name: PlainOrder, forEntity: SalesOrder, fields: [Status] }
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: DRAFT, description: Being prepared }
                  - { id: 2, name: APPROVED, description: Ready to ship }
                  - { id: 3, name: SHIPPED }
                  - { id: 4, name: CANCELLED, description: Called off }
              - name: order-statuses-bg
                entity: OrderStatus
                language: bg
                rows:
                  - { id: 1, name: "Чернова" }
            """;

    /**
     * A process whose steps write no status says nothing about the walk, so the whole non-terminal
     * nomenclature is still the best available reading.
     */
    @SuppressWarnings("unchecked")
    @Test
    void aFlowThatWritesNoStatusKeepsTheWholeNomenclature() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("ConfirmOrder")
                                                              .get("metadata");
        assertEquals(Boolean.TRUE, meta.get("taskForm"));
        // CANCELLED is terminal (excluded); the bg translation seed is ignored; order is seed order.
        // Each step carries its label plus the optional `description` seed column (absent -> no key).
        assertEquals(List.of(Map.of("label", "DRAFT", "description", "Being prepared"),
                Map.of("label", "APPROVED", "description", "Ready to ship"), Map.of("label", "SHIPPED")), meta.get("steps"));
        // The current-status model variable is the EntityStatus relation name.
        assertEquals("Status", meta.get("statusVar"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noStepsOnANonTaskForm() {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("PlainOrder")
                                                              .get("metadata");
        assertFalse(meta.containsKey("taskForm"));
        assertFalse(meta.containsKey("steps"));
        assertTrue(meta.get("statusVar") == null);
    }

    /**
     * The sta shape (issue #7085): an approval flow that walks DRAFT -> APPROVED -> ISSUED -> CONFIRMED
     * next to the settlement statuses PARTIAL / PAID, which no process step writes - they are reached
     * from ISSUED on payment, by the paid roll-up. A second flow on the same document walks exactly
     * those two.
     */
    private static final String BILLING_YAML =
            """
                    name: billing
                    entities:
                      - name: InvoiceStatus
                        function: Setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string }
                      - name: Invoice
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: total, type: decimal }
                        relations:
                          - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                    processes:
                      - name: InvoiceApproval
                        trigger: { onCreate: Invoice }
                        steps:
                          - { name: approve, kind: userTask, args: { assignee: approver, form: ApproveInvoice } }
                          - { name: approveDecision, kind: decision, args: { if: "action == 'approve'", then: activate, else: cancel } }
                          - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: issue } }
                          - { name: issue, kind: userTask, args: { assignee: issuer, form: IssueInvoice, setRelationField: Status, value: 3, next: confirm } }
                          - { name: confirm, kind: userTask, args: { assignee: issuer, form: ConfirmInvoice } }
                          - { name: confirmDecision, kind: decision, args: { if: "action == 'confirm'", then: confirmed, else: cancel } }
                          - { name: confirmed, kind: serviceTask, args: { setRelationField: Status, value: 5, next: end } }
                          - { name: cancel, kind: serviceTask, args: { setRelationField: Status, value: 8, next: end } }
                          - { name: end, kind: end }
                      - name: InvoiceSettlement
                        trigger: { onUpdate: Invoice, when: "Status == ISSUED" }
                        steps:
                          - { name: record, kind: userTask, args: { assignee: cashier, form: RecordPayment } }
                          - { name: settledDecision, kind: decision, args: { if: "action == 'settle'", then: paid, else: partial } }
                          - { name: paid, kind: serviceTask, args: { setRelationField: Status, value: 7, next: end } }
                          - { name: partial, kind: serviceTask, args: { setRelationField: Status, value: 6, next: end } }
                          - { name: end, kind: end }
                    forms:
                      - { name: ApproveInvoice, forEntity: Invoice, fields: [Status, total], actions: [approve, reject] }
                      - { name: IssueInvoice, forEntity: Invoice, fields: [Status, total], actions: [issue] }
                      - { name: ConfirmInvoice, forEntity: Invoice, fields: [Status, total], actions: [confirm, amend] }
                      - { name: RecordPayment, forEntity: Invoice, fields: [Status, total], actions: [settle] }
                    seeds:
                      - name: invoice-statuses
                        entity: InvoiceStatus
                        rows:
                          - { id: 1, name: DRAFT }
                          - { id: 2, name: APPROVED }
                          - { id: 3, name: ISSUED }
                          - { id: 5, name: CONFIRMED }
                          - { id: 6, name: PARTIAL }
                          - { id: 7, name: PAID }
                          - { id: 8, name: CANCELLED }
                    """;

    @SuppressWarnings("unchecked")
    @Test
    void theStepsAreTheStatusesTheOwnFlowWalks() {
        IntentModel model = IntentParser.parse(BILLING_YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        // Every task form of the approval flow: the entry status (init) plus the statuses the flow's own
        // steps write - APPROVED, ISSUED, CONFIRMED. PARTIAL/PAID are settlement states the paid roll-up
        // writes from ISSUED, so they are steps of no approval; CANCELLED is terminal.
        List<Map<String, Object>> approvalSteps =
                List.of(Map.of("label", "DRAFT"), Map.of("label", "APPROVED"), Map.of("label", "ISSUED"), Map.of("label", "CONFIRMED"));
        for (String form : List.of("ApproveInvoice", "IssueInvoice", "ConfirmInvoice")) {
            Map<String, Object> meta = (Map<String, Object>) forms.get(form)
                                                                  .get("metadata");
            assertEquals(approvalSteps, meta.get("steps"), form + " should show the approval flow's own statuses");
            assertEquals("Status", meta.get("statusVar"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void anotherFlowOnTheSameDocumentGetsItsOwnSteps() {
        IntentModel model = IntentParser.parse(BILLING_YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        // The settlement flow walks the two statuses IT writes - the approval's are not steps of it -
        // and it ENTERS at the status its trigger guard pins (ISSUED), not at the relation's `init:`
        // DRAFT, which is where an onCreate flow starts and a document under settlement has long left
        // (issue #7239).
        Map<String, Object> meta = (Map<String, Object>) forms.get("RecordPayment")
                                                              .get("metadata");
        assertEquals(List.of(Map.of("label", "ISSUED"), Map.of("label", "PARTIAL"), Map.of("label", "PAID")), meta.get("steps"));
    }

    /**
     * A flow that is not started by a creation and whose trigger pins no status enters wherever the
     * record happens to stand: it has no first step to claim, and the stepper says only what the flow
     * writes rather than inventing DRAFT.
     */
    @SuppressWarnings("unchecked")
    @Test
    void aBareOnUpdateFlowClaimsNoEntryStep() {
        IntentModel model = IntentParser.parse(BILLING_YAML.replace(", when: \"Status == ISSUED\"", ""));
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("RecordPayment")
                                                              .get("metadata");
        assertEquals(List.of(Map.of("label", "PARTIAL"), Map.of("label", "PAID")), meta.get("steps"));
    }

    /**
     * An {@code onTransition} flow reads its entry status from the guard exactly as an {@code onUpdate}
     * one does - the status axis is where a {@code when: "Status == ..."} guard is the norm rather than
     * the exception.
     */
    @SuppressWarnings("unchecked")
    @Test
    void anOnTransitionFlowEntersAtTheGuardedStatus() {
        IntentModel model = IntentParser.parse(BILLING_YAML.replace("onUpdate: Invoice", "onTransition: Invoice"));
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);

        Map<String, Object> meta = (Map<String, Object>) forms.get("RecordPayment")
                                                              .get("metadata");
        assertEquals(List.of(Map.of("label", "ISSUED"), Map.of("label", "PARTIAL"), Map.of("label", "PAID")), meta.get("steps"));
    }
}
