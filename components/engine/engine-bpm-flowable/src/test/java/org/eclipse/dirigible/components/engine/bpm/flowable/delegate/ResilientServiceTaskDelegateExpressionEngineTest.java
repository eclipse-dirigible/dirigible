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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The resilience conversion on the {@code flowable:delegateExpression} path against a REAL Flowable
 * engine (in-memory, async executor on) - the second of the two hooks, added for dirigible #7056 so
 * a {@code notify:} step (emitted as {@code ${JavaTask}}, which never passes through
 * {@code ClassDelegate}) can declare {@code retry:} / {@code onError:} at all.
 *
 * <p>
 * Three properties, and the third is the one the wrapper is responsible for not breaking: the
 * declared retry cycle re-runs the resolved delegate and its FINAL failed attempt converts to the
 * caught intent error (routing through the boundary with the message published for
 * {@code {error}}); a delegate that recovers within its cycle completes with no conversion at all;
 * and a delegate that raises a {@link BpmnError} <em>itself</em> still reaches its own boundary,
 * because the wrapper converts only what the superclass rethrew.
 */
class ResilientServiceTaskDelegateExpressionEngineTest {

    /**
     * One shape for all three cases: an async delegate-expression task carrying a retry cycle, with two
     * boundaries - the intent one the conversion raises, and a business one a delegate raises itself -
     * each routing to a step that records what it saw.
     */
    private static final String PROCESS_XML_TEMPLATE =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <error id="intentStepError" name="Intent Step Error" errorCode="INTENT_STEP_FAILED"></error>
                      <error id="businessError" name="Business Error" errorCode="BUSINESS_REFUSED"></error>
                      <process id="%s" name="Resilience" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="call" name="Call" flowable:async="true" flowable:delegateExpression="${%s}">
                          <extensionElements>
                            <flowable:failedJobRetryTimeCycle>%s</flowable:failedJobRetryTimeCycle>
                          </extensionElements>
                        </serviceTask>
                        <boundaryEvent id="callError" attachedToRef="call" cancelActivity="true">
                          <errorEventDefinition errorRef="intentStepError"></errorEventDefinition>
                        </boundaryEvent>
                        <boundaryEvent id="callRefused" attachedToRef="call" cancelActivity="true">
                          <errorEventDefinition errorRef="businessError"></errorEventDefinition>
                        </boundaryEvent>
                        <serviceTask id="recordFailure" name="Record Failure" flowable:delegateExpression="${recordFailure}"></serviceTask>
                        <serviceTask id="recordRefusal" name="Record Refusal" flowable:delegateExpression="${recordRefusal}"></serviceTask>
                        <endEvent id="end"></endEvent>
                        <endEvent id="failedEnd"></endEvent>
                        <endEvent id="refusedEnd"></endEvent>
                        <sequenceFlow id="flow_start_call" sourceRef="start" targetRef="call"></sequenceFlow>
                        <sequenceFlow id="flow_call_end" sourceRef="call" targetRef="end"></sequenceFlow>
                        <sequenceFlow id="flow_callError_then" sourceRef="callError" targetRef="recordFailure"></sequenceFlow>
                        <sequenceFlow id="flow_recordFailure_end" sourceRef="recordFailure" targetRef="failedEnd"></sequenceFlow>
                        <sequenceFlow id="flow_callRefused_then" sourceRef="callRefused" targetRef="recordRefusal"></sequenceFlow>
                        <sequenceFlow id="flow_recordRefusal_end" sourceRef="recordRefusal" targetRef="refusedEnd"></sequenceFlow>
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

    /** Raises its own BPMN error - which the wrapper must leave to its own boundary, unconverted. */
    public static class RefusingDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            throw new BpmnError("BUSINESS_REFUSED", "the customer is on hold");
        }
    }

    /**
     * The intent error route: records what {@code {error}} reads, like the generated setField delegate.
     */
    public static class RecordFailureDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("recorded", String.valueOf(execution.getVariable(IntentStepResilience.ERROR_MESSAGE_VARIABLE)));
        }
    }

    /** The business error route. */
    public static class RecordRefusalDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("refused", "yes");
        }
    }

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        configuration.setJdbcUrl("jdbc:h2:mem:resilience-delegate-expression-test;DB_CLOSE_DELAY=1000");
        configuration.setActivityBehaviorFactory(new ResilientActivityBehaviorFactory(new ResilientClassDelegateFactory()));
        // The beans a ${...} delegate expression resolves against - the standalone counterpart of the
        // Spring bean factory the platform's engine is configured with (where ${JavaTask} lives).
        Map<Object, Object> beans = new HashMap<>();
        beans.put("doomed", new DoomedDelegate());
        beans.put("flaky", new FlakyDelegate());
        beans.put("refusing", new RefusingDelegate());
        beans.put("recordFailure", new RecordFailureDelegate());
        beans.put("recordRefusal", new RecordRefusalDelegate());
        configuration.setBeans(beans);
        configuration.setAsyncExecutorActivate(true);
        // Tight acquire cycles so the PT1S retry waits dominate the test's wall clock.
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(100));
        configuration.getAsyncExecutorConfiguration()
                     .setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(100));
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
        deploy("doomed-expression", "doomed", "R2/PT1S");
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("doomed-expression");

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
        deploy("flaky-expression", "flaky", "R3/PT1S");
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("flaky-expression");

        assertEquals("OK-3", historicVariable(instance, "result"),
                "the declared cycle must re-run the delegate until it succeeds on its last attempt");
        assertNull(historicVariable(instance, "recorded"), "a recovered step must not have routed through the error boundary");
    }

    /**
     * The must-not-break case of wrapping {@code handleException}: a {@link BpmnError} the delegate
     * raises itself is the superclass's business, and the wrapper sees nothing to convert - so it
     * reaches ITS OWN boundary on the first attempt rather than being retried or re-coded as the intent
     * error.
     */
    @Test
    void aDelegateRaisingItsOwnBpmnErrorStillReachesItsOwnBoundary() {
        deploy("refusing-expression", "refusing", "R3/PT1S");
        ProcessInstance instance = engine.getRuntimeService()
                                         .startProcessInstanceByKey("refusing-expression");

        assertEquals("yes", historicVariable(instance, "refused"), "the delegate's own BPMN error must take its own route");
        assertNull(historicVariable(instance, "recorded"), "it must not be converted into the intent error");
    }

    private static void deploy(String processId, String delegateBean, String retryCycle) {
        String xml = PROCESS_XML_TEMPLATE.formatted(processId, delegateBean, retryCycle);
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
