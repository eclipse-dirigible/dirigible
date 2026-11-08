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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A user task's claim moves its record - dirigible #8461.
 *
 * <p>
 * A create-triggered approval left its document DRAFT while the approver decided, so the lines -
 * frozen by {@code forbidWhen: "Order.Status != DRAFT"} - stayed editable and the document was
 * confirmed at a total nobody approved. {@code onClaim} / {@code onUnclaim} move the record while
 * the task is claimed, through a generated {@code assignment} task listener.
 */
class ClaimIntentTest {

    private static final String YAML =
            """
                    name: sales
                    seeds:
                      - name: order-statuses
                        entity: OrderStatus
                        rows:
                          - { id: 1, name: DRAFT }
                          - { id: 2, name: SUBMITTED }
                          - { id: 3, name: CONFIRMED }
                    entities:
                      - name: OrderStatus
                        kind: setting
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, length: 50 }
                      - name: Order
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: customer, type: string, length: 100 }
                        relations:
                          - { name: Status, kind: manyToOne, to: OrderStatus, function: EntityStatus, init: DRAFT }
                        lifecycle:
                          edges:
                            - { from: DRAFT, to: [SUBMITTED] }
                            - { from: SUBMITTED, to: [DRAFT, CONFIRMED] }
                    forms:
                      - { name: ConfirmOrder, forEntity: Order, fields: [customer], actions: [confirm] }
                    processes:
                      - name: OrderApproval
                        trigger: { onCreate: Order }
                        steps:
                          - { name: confirm, kind: userTask, args: { assignee: ADMINISTRATOR, form: ConfirmOrder, onClaim: { setRelationField: Status, value: SUBMITTED }, onUnclaim: { setRelationField: Status, value: DRAFT } } }
                          - { name: markConfirmed, kind: serviceTask, args: { setRelationField: Status, value: CONFIRMED } }
                    """;

    @Test
    void theClaimResolvesItsSeededNames() {
        List<ClaimSupport.Claim> claims = ClaimSupport.claims(IntentParser.parse(YAML));

        assertEquals(List.of(new ClaimSupport.Claim("OrderApproval", "confirm", "OrderApprovalConfirmClaimListener", "Order", "Order", "Id",
                "intValue", "Status", "2", "1")), claims);
    }

    @Test
    void theUserTaskListensToItsAssignmentAndCompletesOnlyClaimed() {
        String bpmn = bpmn(YAML);

        assertTrue(bpmn.contains(
                "<flowable:taskListener event=\"assignment\" class=\"gen.events.sales.OrderApprovalConfirmClaimListener\"></flowable:taskListener>"),
                bpmn);
        assertTrue(bpmn.contains("<flowable:property name=\"taskClaimRequired\" value=\"true\"></flowable:property>"), bpmn);
    }

    @Test
    void aTaskWithoutAClaimStaysAsItWas() {
        String bpmn = bpmn(YAML.replace(
                ", onClaim: { setRelationField: Status, value: SUBMITTED }, onUnclaim: { setRelationField: Status, value: DRAFT }", ""));

        assertFalse(bpmn.contains("taskListener"), bpmn);
        assertFalse(bpmn.contains("taskClaimRequired"), bpmn);
    }

    @Test
    void aClaimWithoutAnUnclaimIsRejected() {
        assertIssue(YAML.replace(", onUnclaim: { setRelationField: Status, value: DRAFT }", ""),
                "process [OrderApproval] step [confirm] declares onClaim without onUnclaim");
    }

    @Test
    void anUnknownRelationIsRejected() {
        assertIssue(YAML.replace("onClaim: { setRelationField: Status,", "onClaim: { setRelationField: Stage,"),
                "process [OrderApproval] step [confirm] onClaim setRelationField [Stage]");
    }

    @Test
    void anUnknownKeyIsRejected() {
        assertIssue(
                YAML.replace("onUnclaim: { setRelationField: Status, value: DRAFT }",
                        "onUnclaim: { setRelationField: Status, value: DRAFT, when: always }"),
                "process [OrderApproval] step [confirm] onUnclaim declares unknown key [when]");
    }

    @Test
    void anUnknownStatusIsRejected() {
        assertIssue(YAML.replace("value: SUBMITTED }", "value: PENDING }"),
                "names [PENDING], which is not a seeded status of [OrderStatus]");
    }

    @Test
    void theSameStatusTwiceIsRejected() {
        assertIssue(YAML.replace("value: SUBMITTED }", "value: DRAFT }"),
                "process [OrderApproval] step [confirm] onClaim and onUnclaim set the same value");
    }

    @Test
    void aMoveTheLifecycleRefusesIsRejected() {
        assertIssue(YAML.replace("{ from: SUBMITTED, to: [DRAFT, CONFIRMED] }", "{ from: SUBMITTED, to: [CONFIRMED] }"),
                "process [OrderApproval] step [confirm] onUnclaim moves the record from [SUBMITTED] to [DRAFT], which the [Order] lifecycle does not allow");
    }

    @Test
    void aClaimOnAServiceTaskIsRejected() {
        assertIssue(YAML.replace("{ name: markConfirmed, kind: serviceTask, args: { setRelationField: Status, value: CONFIRMED } }",
                "{ name: markConfirmed, kind: serviceTask, args: { setRelationField: Status, value: CONFIRMED, onClaim: { setRelationField: Status, value: SUBMITTED } } }"),
                "process [OrderApproval] step [markConfirmed] declares arg [onClaim] but is a serviceTask");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + ex.getIssues());
    }

    private static String bpmn(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/sales", "sales", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/OrderApproval.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }
}
