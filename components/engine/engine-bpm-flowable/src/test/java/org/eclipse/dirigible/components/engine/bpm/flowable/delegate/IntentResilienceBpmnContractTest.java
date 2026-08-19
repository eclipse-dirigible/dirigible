/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

/**
 * The BPMN shape the intent generator emits for step resilience (dirigible #6762), parsed by the
 * REAL Flowable converter this engine runs: the {@code failedJobRetryTimeCycle} extension element
 * must land on the service task (or the declared retry silently never runs), and the error boundary
 * must resolve to the {@code INTENT_STEP_FAILED} code {@code IntentStepResilience} raises. This is
 * the string contract between {@code engine-intent}'s {@code BpmnIntentGenerator} and this module -
 * neither side depends on the other in code, so this test is where a drift surfaces.
 */
class IntentResilienceBpmnContractTest {

    private static final String GENERATED_SHAPE =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:flowable="http://flowable.org/bpmn" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" typeLanguage="http://www.w3.org/2001/XMLSchema" expressionLanguage="http://www.w3.org/1999/XPath" targetNamespace="http://www.flowable.org/processdef">
                      <error id="intentStepError" name="Intent Step Error" errorCode="INTENT_STEP_FAILED"></error>
                      <process id="TenantProvisioning" name="Tenant Provisioning" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="flakyCall" name="Flaky Call" flowable:async="true" flowable:class="custom.FlakyProvisioner">
                          <extensionElements>
                            <flowable:failedJobRetryTimeCycle>R3/PT2S</flowable:failedJobRetryTimeCycle>
                            <flowable:executionListener event="end" expression="${execution.removeVariable('apiKey')}"></flowable:executionListener>
                          </extensionElements>
                        </serviceTask>
                        <boundaryEvent id="flakyCallError" attachedToRef="flakyCall" cancelActivity="true">
                          <errorEventDefinition errorRef="intentStepError"></errorEventDefinition>
                        </boundaryEvent>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_flakyCall" sourceRef="start" targetRef="flakyCall"></sequenceFlow>
                        <sequenceFlow id="flow_flakyCall_end" sourceRef="flakyCall" targetRef="end"></sequenceFlow>
                        <sequenceFlow id="flow_flakyCallError_then" sourceRef="flakyCallError" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    @Test
    void theGeneratedResilienceShapeParsesIntoWhatTheRuntimeReads() throws Exception {
        XMLStreamReader reader = XMLInputFactory.newInstance()
                                                .createXMLStreamReader(new StringReader(GENERATED_SHAPE));
        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(reader);

        ServiceTask task = (ServiceTask) model.getMainProcess()
                                              .getFlowElement("flakyCall");
        assertEquals("R3/PT2S", task.getFailedJobRetryTimeCycleValue(),
                "the retry cycle must land on the task - JobRetryCmd and the finality check both read it there");
        assertEquals(1, task.getExecutionListeners()
                            .size(),
                "the clearAfter end-listener must parse");

        BoundaryEvent boundary = (BoundaryEvent) model.getMainProcess()
                                                      .getFlowElement("flakyCallError");
        assertEquals("flakyCall", boundary.getAttachedToRefId());
        ErrorEventDefinition definition = (ErrorEventDefinition) boundary.getEventDefinitions()
                                                                         .get(0);
        String resolved = model.getErrors()
                               .get(definition.getErrorCode());
        assertEquals(IntentStepResilience.ERROR_CODE, resolved != null ? resolved : definition.getErrorCode(),
                "the boundary must resolve to the code IntentStepResilience raises");
        // No isCancelActivity assertion: the parsed model does not reflect the attribute for error
        // boundary events (a converter quirk) - the engine treats them as interrupting regardless,
        // which ResilientClassDelegateEngineTest proves against a live instance.
    }
}
