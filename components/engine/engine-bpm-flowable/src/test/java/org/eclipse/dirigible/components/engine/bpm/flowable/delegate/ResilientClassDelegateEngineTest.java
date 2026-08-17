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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The whole resilience conversion against a REAL Flowable engine (in-memory, async executor on):
 * the declared retry cycle re-runs a failing {@code flowable:class} delegate, the FINAL failed
 * attempt converts to the caught intent error (routing the token through the {@code onError}
 * boundary with the failure message published for {@code {error}}), and a delegate that recovers
 * within its cycle completes normally with no conversion at all. This is what proves the
 * final-attempt arithmetic against the engine's own {@code JobRetryCmd}, not a mirror of it.
 */
class ResilientClassDelegateEngineTest {

    private static final String PROCESS_XML_TEMPLATE =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <error id="intentStepError" name="Intent Step Error" errorCode="INTENT_STEP_FAILED"></error>
                      <process id="%s" name="Resilience" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="call" name="Call" flowable:async="true" flowable:class="%s">
                          <extensionElements>
                            <flowable:failedJobRetryTimeCycle>%s</flowable:failedJobRetryTimeCycle>
                          </extensionElements>
                        </serviceTask>
                        <boundaryEvent id="callError" attachedToRef="call" cancelActivity="true">
                          <errorEventDefinition errorRef="intentStepError"></errorEventDefinition>
                        </boundaryEvent>
                        <serviceTask id="recordFailure" name="Record Failure" flowable:class="%s"></serviceTask>
                        <endEvent id="end"></endEvent>
                        <endEvent id="failedEnd"></endEvent>
                        <sequenceFlow id="flow_start_call" sourceRef="start" targetRef="call"></sequenceFlow>
                        <sequenceFlow id="flow_call_end" sourceRef="call" targetRef="end"></sequenceFlow>
                        <sequenceFlow id="flow_callError_then" sourceRef="callError" targetRef="recordFailure"></sequenceFlow>
                        <sequenceFlow id="flow_recordFailure_end" sourceRef="recordFailure" targetRef="failedEnd"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static ProcessEngine engine;

    /** Always fails; carries the attempt number so the recorded message pins WHICH attempt routed. */
    public static class DoomedDelegate implements JavaDelegate {

        static final AtomicInteger ATTEMPTS = new AtomicInteger();

        @Override
        public void execute(DelegateExecution execution) {
            throw new IllegalStateException("refused (attempt " + ATTEMPTS.incrementAndGet() + ")");
        }
    }

    /** Fails twice, succeeds on the third attempt - within its declared R3 cycle. */
    public static class FlakyDelegate implements JavaDelegate {

        static final AtomicInteger ATTEMPTS = new AtomicInteger();

        @Override
        public void execute(DelegateExecution execution) {
            int attempt = ATTEMPTS.incrementAndGet();
            if (attempt < 3) {
                throw new IllegalStateException("flaky (attempt " + attempt + ")");
            }
            execution.setVariable("result", "OK-" + attempt);
        }
    }

    /** The error route: records what {@code {error}} reads, like the generated setField delegate. */
    public static class RecordFailureDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("recorded", String.valueOf(execution.getVariable(IntentStepResilience.ERROR_MESSAGE_VARIABLE)));
        }
    }

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:resilience-test;DB_CLOSE_DELAY=1000");
        configuration.setActivityBehaviorFactory(new DefaultActivityBehaviorFactory(new ResilientClassDelegateFactory()));
        configuration.setAsyncExecutorActivate(true);
        configuration.setAsyncExecutorDefaultAsyncJobAcquireWaitTime(100);
        configuration.setAsyncExecutorDefaultTimerJobAcquireWaitTime(100);
        engine = configuration.buildProcessEngine();
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void anExhaustedRetryCycleRoutesTheFinalAttemptsMessageThroughTheErrorBoundary() {
        deploy("doomed", DoomedDelegate.class.getName(), "R2/PT1S");
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("doomed");

        // R2 = two total attempts; the SECOND one converts instead of dead-lettering, so the flow
        // ends through the error route with that attempt's message recorded.
        assertEquals("refused (attempt 2)", historicVariable(instance, "recorded"),
                "the error route must record the FINAL attempt's failure message");
        assertEquals(0, engine.getManagementService()
                              .createDeadLetterJobQuery()
                              .count(),
                "the converted failure must never dead-letter");
    }

    @Test
    void aDelegateThatRecoversWithinItsCycleCompletesWithNoConversion() {
        deploy("flaky", FlakyDelegate.class.getName(), "R3/PT1S");
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("flaky");

        assertEquals("OK-3", historicVariable(instance, "result"),
                "the declared cycle must re-run the delegate until it succeeds on its last attempt");
    }

    private static void deploy(String processId, String delegateClass, String retryCycle) {
        String xml = PROCESS_XML_TEMPLATE.formatted(processId, delegateClass, retryCycle, RecordFailureDelegate.class.getName());
        engine.getRepositoryService()
              .createDeployment()
              .addString(processId + ".bpmn20.xml", xml)
              .deploy();
    }

    /** Wait for the instance to end and return the historic value of the named variable. */
    private static String historicVariable(ProcessInstance instance, String name) {
        HistoryService history = engine.getHistoryService();
        await().atMost(Duration.ofSeconds(60))
               .pollInterval(Duration.ofMillis(250))
               .until(() -> history.createHistoricProcessInstanceQuery()
                                   .processInstanceId(instance.getId())
                                   .finished()
                                   .count() == 1);
        HistoricVariableInstance variable = history.createHistoricVariableInstanceQuery()
                                                   .processInstanceId(instance.getId())
                                                   .variableName(name)
                                                   .singleResult();
        return variable == null ? null : String.valueOf(variable.getValue());
    }
}
