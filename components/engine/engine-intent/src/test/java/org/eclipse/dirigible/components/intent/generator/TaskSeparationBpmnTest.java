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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A user task's candidates and the users it keeps out (issue #8111): the deployment's
 * {@code access.extraRoles} - every tenant member - are no task's candidates unless the step opts
 * in, and an approval is separated from the tasks its submitter completed.
 */
class TaskSeparationBpmnTest {

    private static final String YAML = """
            name: expenses
            entities:
              - name: ExpenseClaim
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
            processes:
              - name: ExpenseApproval
                trigger: { onCreate: ExpenseClaim }
                steps:
                  - { name: submit,    kind: userTask, args: { assignee: employee, form: SubmitExpense } }
                  - { name: approve,   kind: userTask, args: { assignee: approver, form: ApproveExpense } }
                  - { name: reimburse, kind: userTask, args: { assignee: payer, form: ReimburseExpense, extraRoles: true } }
            forms:
              - { name: SubmitExpense,    forEntity: ExpenseClaim, fields: [amount], actions: [submit] }
              - { name: ApproveExpense,   forEntity: ExpenseClaim, fields: [amount], actions: [approve] }
              - { name: ReimburseExpense, forEntity: ExpenseClaim, fields: [amount], actions: [reimburse] }
            permissions:
              - { role: Approver, can: [ExpenseClaim:read] }
            """;

    private static final String SETTINGS = """
            {"userTasks": {"candidateGroupsExtra": ["ADMINISTRATOR", "Owner", "User"]},
             "access": {"extraRoles": ["Owner", "User"]}}
            """;

    @Test
    void theDeploymentsExtraRolesAreNoTasksCandidatesUnlessTheStepOptsIn() {
        String bpmn = bpmn(YAML, SETTINGS, null);

        assertTrue(userTask(bpmn, "approve").contains("flowable:candidateGroups=\"Approver,ADMINISTRATOR\""),
                "every tenant member would be a candidate for the approval: " + userTask(bpmn, "approve"));
        assertTrue(userTask(bpmn, "reimburse").contains("flowable:candidateGroups=\"Payer,ADMINISTRATOR,Owner,User\""),
                "a step declaring extraRoles: true admits the deployment's roles: " + userTask(bpmn, "reimburse"));
    }

    @Test
    void aCandidateGroupsExtraNoAccessSettingNamesIsKept() {
        String bpmn = bpmn(YAML, "{\"userTasks\": {\"candidateGroupsExtra\": [\"ADMINISTRATOR\", \"Owner\", \"User\"]}}", null);

        assertTrue(userTask(bpmn, "approve").contains("flowable:candidateGroups=\"Approver,ADMINISTRATOR,Owner,User\""),
                userTask(bpmn, "approve"));
    }

    @Test
    void aRoleNoTenantMemberHoldsIsReported() {
        IntentGenerationContext[] context = new IntentGenerationContext[1];
        bpmn(YAML, SETTINGS, context);

        assertTrue(context[0].getAdvisories()
                             .stream()
                             .anyMatch(advisory -> advisory.contains("step [submit] assignee [employee] names no declared role")),
                "the submit task is left to nobody in the tenant: " + context[0].getAdvisories());
        assertFalse(context[0].getAdvisories()
                              .stream()
                              .anyMatch(advisory -> advisory.contains("step [approve]") || advisory.contains("step [reimburse]")),
                "a declared role and an opted-in step are claimable: " + context[0].getAdvisories());
    }

    @Test
    void anApprovalIsSeparatedFromEveryOtherUserTaskOfItsProcess() {
        String approve = userTask(bpmn(YAML, SETTINGS, null), "approve");

        assertTrue(approve.contains("<flowable:property name=\"taskSeparateFrom\" value=\"submit,reimburse\"></flowable:property>"),
                approve);
        assertTrue(approve.contains(
                "<flowable:property name=\"taskSeparationMessage\" value=\"You cannot approve your own expense claim\"></flowable:property>"),
                approve);
    }

    @Test
    void aTaskThatApprovesNothingIsNotSeparated() {
        String bpmn = bpmn(YAML, SETTINGS, null);

        assertFalse(userTask(bpmn, "submit").contains("taskSeparateFrom"), userTask(bpmn, "submit"));
        assertFalse(userTask(bpmn, "reimburse").contains("taskSeparateFrom"), userTask(bpmn, "reimburse"));
    }

    @Test
    void separateFromNamesTheStepsOrNone() {
        String yaml = YAML.replace("form: ReimburseExpense, extraRoles: true", "form: ReimburseExpense, separateFrom: approve")
                          .replace("form: ApproveExpense }", "form: ApproveExpense, separateFrom: [] }");
        String bpmn = bpmn(yaml, SETTINGS, null);

        assertTrue(
                userTask(bpmn, "reimburse").contains("<flowable:property name=\"taskSeparateFrom\" value=\"approve\"></flowable:property>"),
                userTask(bpmn, "reimburse"));
        assertFalse(userTask(bpmn, "approve").contains("taskSeparateFrom"),
                "an empty list turns the default off: " + userTask(bpmn, "approve"));
    }

    @Test
    void separateFromMustNameAnotherUserTask() {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                YAML.replace("form: ApproveExpense }", "form: ApproveExpense, separateFrom: [approve, sbumit] }")));

        assertTrue(refused.getMessage()
                          .contains("step [approve] separateFrom names [approve], which is not another user task of the process"),
                refused.getMessage());
        assertTrue(refused.getMessage()
                          .contains("separateFrom names [sbumit]"),
                refused.getMessage());
    }

    @Test
    void extraRolesIsAFlag() {
        IntentValidationException refused = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("extraRoles: true", "extraRoles: yes please")));

        assertTrue(refused.getMessage()
                          .contains("step [reimburse] extraRoles must be true or false"),
                refused.getMessage());
    }

    /** The whole {@code <userTask>} element with the given id. */
    private static String userTask(String bpmn, String id) {
        Matcher matcher = Pattern.compile("<userTask id=\"" + id + "\".*?(?:/>|</userTask>)", Pattern.DOTALL)
                                 .matcher(bpmn);
        assertTrue(matcher.find(), "user task [" + id + "] not found in: " + bpmn);
        return matcher.group();
    }

    private static String bpmn(String yaml, String settings, IntentGenerationContext[] contextOut) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.parse(settings));
        if (contextOut != null) {
            contextOut[0] = context;
        }

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
