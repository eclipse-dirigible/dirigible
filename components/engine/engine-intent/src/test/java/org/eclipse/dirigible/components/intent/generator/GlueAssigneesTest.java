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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the {@code assignees} glue collection and the BPMN it pairs with: a user task whose
 * {@code assignee} is a relation walk gets a delegate inserted right before it that walks the
 * trigger record to the person the task belongs to, and the task binds its
 * {@code flowable:assignee} to the variable that delegate publishes while keeping the declared
 * fallback candidate group.
 */
class GlueAssigneesTest {

    private static final String YAML = """
            name: expenses
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: email, type: string, unique: true }
                relations:
                  - { name: manager, kind: manyToOne, to: Employee }
              - name: Expense
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: employee, kind: manyToOne, to: Employee }
            permissions:
              - { role: Manager, can: [Expense:read] }
            processes:
              - name: ExpenseApproval
                trigger: { onCreate: Expense }
                steps:
                  - name: approve
                    kind: userTask
                    args:
                      assignee: { path: employee.manager, fallback: manager }
                      next: done
                  - { name: done, kind: end }
            """;

    @Test
    void theWalkBecomesAChainOfHops() {
        List<Map<String, Object>> assignees = GlueIntentGenerator.buildAssigneesForTest(IntentParser.parse(YAML));
        assertEquals(1, assignees.size());
        Map<String, Object> assignee = assignees.get(0);

        assertEquals("ExpenseApproval", assignee.get("process"));
        assertEquals("approve", assignee.get("step"));
        assertEquals("ResolveExpenseApprovalApproveAssignee", assignee.get("handler"));
        assertEquals("__assignee_approve", assignee.get("variable"));
        assertEquals("employee.manager", assignee.get("path"));
        // The delegate starts from the trigger record, which the id-only process context locates by id.
        assertEquals("Expense", assignee.get("ownerEntity"));
        assertEquals("Id", assignee.get("ownerKeyProperty"));
        assertEquals("intValue", assignee.get("ownerKeyAccessor"));
        assertEquals("Employee", assignee.get("firstFkProperty"));
        assertEquals("Email", assignee.get("identityProperty"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hops = (List<Map<String, Object>>) assignee.get("hops");
        assertEquals(2, hops.size());
        assertEquals("hop0", hops.get(0)
                                 .get("local"));
        assertEquals("Employee", hops.get(0)
                                     .get("entity"));
        // The FK read off the first hop to reach the second.
        assertEquals("Manager", hops.get(0)
                                    .get("nextFkProperty"));
        assertEquals("hop1", hops.get(1)
                                 .get("local"));
        assertEquals("", hops.get(1)
                             .get("nextFkProperty"));
        // The identity is read off the LAST hop.
        assertEquals("hop1", assignee.get("identityLocal"));
    }

    @Test
    void aSingleHopWalkNeedsNoIntermediateFk() {
        Map<String, Object> assignee =
                GlueIntentGenerator.buildAssigneesForTest(IntentParser.parse(YAML.replace("path: employee.manager", "path: employee")))
                                   .get(0);
        assertEquals("Employee", assignee.get("firstFkProperty"));
        assertEquals("hop0", assignee.get("identityLocal"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hops = (List<Map<String, Object>>) assignee.get("hops");
        assertEquals(1, hops.size());
        assertEquals("", hops.get(0)
                             .get("nextFkProperty"));
    }

    @Test
    void aRoleAssigneeGeneratesNoResolver() {
        assertTrue(GlueIntentGenerator
                                      .buildAssigneesForTest(IntentParser.parse(
                                              YAML.replace("assignee: { path: employee.manager, fallback: manager }", "assignee: manager")))
                                      .isEmpty());
    }

    @Test
    void theTaskBindsTheVariableAndKeepsTheFallbackGroup() {
        String bpmn = bpmn(YAML);
        assertTrue(bpmn.contains("<userTask id=\"approve\""), bpmn);
        assertTrue(bpmn.contains("flowable:assignee=\"${__assignee_approve}\""), bpmn);
        // The fallback resolves to the DECLARED role name, so the Inbox's candidate-group match (which
        // compares against the user's role names) lines up - a task nobody resolved stays claimable.
        assertTrue(bpmn.contains("flowable:candidateGroups=\"Manager,ADMINISTRATOR\""), bpmn);
    }

    @Test
    void theResolverRunsRightBeforeTheTask() {
        String bpmn = bpmn(YAML);
        assertTrue(bpmn.contains("<serviceTask id=\"resolveExpenseApprovalApproveAssignee\""), bpmn);
        assertTrue(bpmn.contains("gen.events.expenses.ResolveExpenseApprovalApproveAssignee"), bpmn);
        assertTrue(bpmn.contains("sourceRef=\"resolveExpenseApprovalApproveAssignee\" targetRef=\"approve\""), bpmn);
    }

    /**
     * A jump - a decision's {@code then}/{@code else}, another step's {@code next} - must land on the
     * delegate, not on the task: it is what publishes the variable the task's assignee expression
     * reads, and an unresolvable expression fails task creation outright.
     */
    @Test
    void aJumpToTheTaskLandsOnTheResolverFirst() {
        String bpmn = bpmn(YAML.replace("      - name: approve\n",
                "      - { name: screen, kind: decision, args: { if: \"amount > 100\", then: approve, else: done } }\n"
                        + "      - name: approve\n"));
        assertTrue(bpmn.contains("sourceRef=\"screen\" targetRef=\"resolveExpenseApprovalApproveAssignee\""), bpmn);
        assertTrue(bpmn.contains("sourceRef=\"resolveExpenseApprovalApproveAssignee\" targetRef=\"approve\""), bpmn);
    }

    private static String bpmn(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
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
                     .endsWith("/ExpenseApproval.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }
}
