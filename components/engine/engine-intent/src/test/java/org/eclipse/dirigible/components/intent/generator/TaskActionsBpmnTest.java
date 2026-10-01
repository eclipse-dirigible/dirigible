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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A user task declares the actions its form completes it with, so the inbox can refuse a completion
 * carrying any other {@code action} before a decision downstream branches on it (issue #7551): the
 * form's {@code actions:} minus {@code close}, which leaves the task open. A task without a form
 * declares nothing and stays unchecked.
 */
class TaskActionsBpmnTest {

    private static final String YAML = """
            name: helpdesk
            entities:
              - name: Ticket
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string, length: 20 }
            processes:
              - name: TicketTriage
                trigger: { onCreate: Ticket }
                steps:
                  - { name: triage,   kind: userTask, args: { assignee: agent, form: TriageTicket } }
                  - { name: accepted, kind: decision, args: { if: "action == 'confirm'", then: confirm, else: cancel } }
                  - { name: confirm,  kind: serviceTask, args: { setField: status, value: CONFIRMED, next: followUp } }
                  - { name: cancel,   kind: serviceTask, args: { setField: status, value: CANCELLED, next: followUp } }
                  - { name: followUp, kind: userTask, args: { assignee: agent, next: done } }
                  - { name: done,     kind: end }
            forms:
              - { name: TriageTicket, forEntity: Ticket, fields: [status], actions: [confirm, reject, close] }
            permissions:
              - { role: Agent, description: Agent, can: [Ticket:read] }
            """;

    @Test
    void aFormTaskDeclaresTheActionsItsFormCompletesItWith() {
        String triage = userTask(bpmn(), "triage");

        assertTrue(triage.contains("<flowable:property name=\"taskActions\" value=\"confirm,reject\"></flowable:property>"),
                "the triage task must declare its form's completing actions, close excluded: " + triage);
    }

    @Test
    void aTaskWithoutAFormDeclaresNoActions() {
        String followUp = userTask(bpmn(), "followUp");

        assertEquals(-1, followUp.indexOf("taskActions"), "a task with no form has no actions to declare: " + followUp);
    }

    /** The whole {@code <userTask>} element with the given id. */
    private static String userTask(String bpmn, String id) {
        Matcher matcher = Pattern.compile("<userTask id=\"" + id + "\".*?(?:/>|</userTask>)", Pattern.DOTALL)
                                 .matcher(bpmn);
        assertTrue(matcher.find(), "user task [" + id + "] not found in: " + bpmn);
        return matcher.group();
    }

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
                     .endsWith("/TicketTriage.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }
}
