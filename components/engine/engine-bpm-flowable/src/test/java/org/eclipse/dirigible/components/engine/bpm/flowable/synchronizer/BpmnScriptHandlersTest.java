/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The script handler a process definition runs is found wherever a step can name one, so a
 * definition the script engine cannot run is refused before it is deployed (#8174).
 */
class BpmnScriptHandlersTest {

    private static final String LOCATION = "/p/process.bpmn";

    @Test
    void aServiceTaskBoundToJsTaskByExpression() {
        String step = """
                <serviceTask id="notify" flowable:async="true" flowable:delegateExpression="${JSTask}">
                  <extensionElements>
                    <flowable:field name="handler"><flowable:string><![CDATA[p/tasks/notify.ts]]></flowable:string></flowable:field>
                  </extensionElements>
                </serviceTask>""";

        assertEquals(Optional.of("p/tasks/notify.ts"), BpmnScriptHandlers.find(LOCATION, process(step)));
    }

    @Test
    void aServiceTaskBoundToTheScriptDelegateByClass() {
        String step =
                """
                        <serviceTask id="notify" flowable:class="org.eclipse.dirigible.components.engine.bpm.flowable.delegate.DirigibleCallDelegate">
                          <extensionElements>
                            <flowable:field name="handler" stringValue="p/tasks/notify.mjs"/>
                          </extensionElements>
                        </serviceTask>""";

        assertEquals(Optional.of("p/tasks/notify.mjs"), BpmnScriptHandlers.find(LOCATION, process(step)));
    }

    @Test
    void aTaskListenerBoundToJsListener() {
        String step = """
                <userTask id="approve" name="Approve">
                  <extensionElements>
                    <flowable:taskListener event="create" delegateExpression="${JSListener}">
                      <flowable:field name="handler"><flowable:string>p/listeners/on-create.ts</flowable:string></flowable:field>
                    </flowable:taskListener>
                  </extensionElements>
                </userTask>""";

        assertEquals(Optional.of("p/listeners/on-create.ts"), BpmnScriptHandlers.find(LOCATION, process(step)));
    }

    @Test
    void aScriptStepWithoutAHandlerIsNamedByItsId() {
        String step = """
                <serviceTask id="nameless" flowable:delegateExpression="${JSTask}"/>""";

        assertEquals(Optional.of("nameless"), BpmnScriptHandlers.find(LOCATION, process(step)));
    }

    @Test
    void aDefinitionRunningJavaOnlyRunsNoScript() {
        String step = """
                <serviceTask id="java" flowable:delegateExpression="${JavaTask}">
                  <extensionElements>
                    <flowable:field name="handler"><flowable:string>app.Handler</flowable:string></flowable:field>
                  </extensionElements>
                </serviceTask>
                <userTask id="approve" name="Approve"/>""";

        assertEquals(Optional.empty(), BpmnScriptHandlers.find(LOCATION, process(step)));
    }

    @Test
    void anUnreadableDefinitionIsLeftToTheDeployment() {
        assertEquals(Optional.empty(), BpmnScriptHandlers.find(LOCATION, "<definitions"));
    }

    private static String process(String steps) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn"
                    targetNamespace="http://www.flowable.org/processdef">
                  <process id="probe" name="Probe" isExecutable="true">
                    <startEvent id="start"/>
                """ + steps + """
                  </process>
                </definitions>
                """;
    }
}
