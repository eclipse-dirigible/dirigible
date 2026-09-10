/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActivityStatusData;
import org.eclipse.dirigible.repository.api.IRepository;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Where a process instance sits, against a REAL Flowable engine (in-memory), for every state an
 * asynchronous step passes through.
 *
 * <p>
 * Flowable deactivates an execution the moment an asynchronous attempt fails, so from the first
 * failure on, a step parked between retry attempts is reported by nothing the engine calls "active"
 * - which is why an intent-generated process, whose every step is {@code flowable:async}, used to
 * answer "no step is running" for its whole life
 * (<a href="https://github.com/eclipse-dirigible/dirigible/issues/7193">#7193</a>).
 *
 * <p>
 * The async executor is deliberately OFF: {@code ManagementService#executeJob} runs the attempt on
 * the calling thread and Flowable's own failed-job listener then applies the retry decision in its
 * own transaction, so every state below is reached synchronously, with no sleeps and no polling.
 */
class BpmProviderFlowableActiveActivitiesTest {

    private static final String TENANT_ID = "active-activities-tenant";

    /** A database of this test's own; the statement count is read back over the same one. */
    private static final String JDBC_URL = "jdbc:h2:mem:active-activities-test";

    private static final String DOOMED_PROCESS = "parked-step";
    private static final String DOOMED_STEP = "doomedStep";
    private static final String WAITING_PROCESS = "waiting-step";
    private static final String WAITING_STEP = "approve";

    /**
     * Two total attempts, an hour apart: the first failure parks the job in the timer-job table and
     * nothing re-acquires it, the second exhausts the cycle and dead-letters.
     */
    private static final String RETRY_CYCLE = "R2/PT1H";

    private static final String DOOMED_PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="Parked Step" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <serviceTask id="%s" name="Doomed Step" flowable:async="true" flowable:class="%s">
                          <extensionElements>
                            <flowable:failedJobRetryTimeCycle>%s</flowable:failedJobRetryTimeCycle>
                          </extensionElements>
                        </serviceTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_step" sourceRef="start" targetRef="%s"></sequenceFlow>
                        <sequenceFlow id="flow_step_end" sourceRef="%s" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static final String WAITING_PROCESS_XML =
            """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.flowable.org/processdef">
                      <process id="%s" name="Waiting Step" isExecutable="true">
                        <startEvent id="start"></startEvent>
                        <userTask id="%s" name="Approve"></userTask>
                        <endEvent id="end"></endEvent>
                        <sequenceFlow id="flow_start_task" sourceRef="start" targetRef="%s"></sequenceFlow>
                        <sequenceFlow id="flow_task_end" sourceRef="%s" targetRef="end"></sequenceFlow>
                      </process>
                    </definitions>
                    """;

    private static ProcessEngine engine;
    private static BpmProviderFlowable bpmProvider;

    /** Always fails, so the step never leaves its retry cycle. */
    public static class DoomedDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            throw new IllegalStateException("refused");
        }
    }

    @BeforeAll
    static void startEngine() {
        StandaloneInMemProcessEngineConfiguration configuration = new StandaloneInMemProcessEngineConfiguration();
        // A database of this test's own: another engine test in the same surefire JVM keeps its
        // in-memory one alive past the class that built it.
        configuration.setJdbcUrl(JDBC_URL + ";DB_CLOSE_DELAY=-1");
        engine = configuration.buildProcessEngine();

        deploy(DOOMED_PROCESS, DOOMED_PROCESS_XML.formatted(DOOMED_PROCESS, DOOMED_STEP, DoomedDelegate.class.getName(), RETRY_CYCLE,
                DOOMED_STEP, DOOMED_STEP));
        deploy(WAITING_PROCESS, WAITING_PROCESS_XML.formatted(WAITING_PROCESS, WAITING_STEP, WAITING_STEP, WAITING_STEP));

        bpmProvider = new BpmProviderFlowable(mock(IRepository.class), tenantContext(), mock(FlowableArtefactsValidator.class), engine);
    }

    @AfterAll
    static void stopEngine() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void aPendingAsynchronousStepIsReportedOnce() {
        ProcessInstance instance = start(DOOMED_PROCESS);

        // The execution is still active AND the async job exists, so the two sources of evidence
        // overlap - the step must not be counted twice.
        assertEquals(List.of(DOOMED_STEP), activeActivityIds(instance), "the untried step is active in the engine's own view");
        assertStatus(instance, DOOMED_STEP, 1, 0);
    }

    @Test
    void aStepParkedBetweenRetryAttemptsIsReported() {
        ProcessInstance instance = start(DOOMED_PROCESS);
        failNextAttempt(instance);

        assertEquals(List.of(), activeActivityIds(instance),
                "Flowable deactivates the execution while a retry is pending - the reason the answer used to be empty");
        assertEquals(1, timerJobs(instance), "the failed job must be parked in the timer-job table");
        assertStatus(instance, DOOMED_STEP, 1, 0);
        assertEquals(List.of(DOOMED_STEP), bpmProvider.getProcessInstanceActivityIds(instance),
                "the parked step is what the instance diagram must highlight");
    }

    @Test
    void aDeadLetteredStepStaysNegative() {
        ProcessInstance instance = deadLetter(start(DOOMED_PROCESS));

        assertStatus(instance, DOOMED_STEP, 0, 1);
    }

    @Test
    void aRetriedDeadLetterJobIsReportedAgain() {
        ProcessInstance instance = deadLetter(start(DOOMED_PROCESS));

        // What the Monitoring shell's Retry action does. It hands the job back to the executor
        // without reactivating the execution, so the job is again the only evidence of the position.
        engine.getManagementService()
              .moveDeadLetterJobToExecutableJob(deadLetterJob(instance).getId(), 1);

        assertEquals(List.of(), activeActivityIds(instance), "a retried job does not reactivate the execution by itself");
        assertStatus(instance, DOOMED_STEP, 1, 0);
    }

    @Test
    void aWaitStateIsReportedAsBefore() {
        ProcessInstance instance = start(WAITING_PROCESS);

        assertStatus(instance, WAITING_STEP, 1, 0);
    }

    @Test
    void aListingIsResolvedForEveryInstanceAtOnce() {
        ProcessInstance parked = start(DOOMED_PROCESS);
        failNextAttempt(parked);
        ProcessInstance untried = start(DOOMED_PROCESS);
        ProcessInstance waiting = start(WAITING_PROCESS);

        Map<String, List<String>> activityIds = bpmProvider.getProcessInstanceActivityIds(List.of(parked, untried, waiting));

        assertEquals(List.of(DOOMED_STEP), activityIds.get(parked.getId()), "a parked step is named by its timer job alone");
        assertEquals(List.of(DOOMED_STEP), activityIds.get(untried.getId()), "an untried step is active and job-bearing - named once");
        assertEquals(List.of(WAITING_STEP), activityIds.get(waiting.getId()), "a wait state is named by its active execution");
    }

    @Test
    void anInstanceOutsideTheListingIsNotReported() {
        ProcessInstance listed = start(WAITING_PROCESS);
        // Job-bearing, and the batch reads the jobs of the whole tenant: the instance ids asked for
        // are what narrows the answer.
        start(DOOMED_PROCESS);

        Map<String, List<String>> activityIds = bpmProvider.getProcessInstanceActivityIds(List.of(listed));

        assertEquals(Set.of(listed.getId()), activityIds.keySet(), "only the listed instance may be answered for");
    }

    @Test
    void anEmptyListingAsksTheEngineNothing() {
        assertEquals(Map.of(), bpmProvider.getProcessInstanceActivityIds(List.of()));
    }

    /**
     * The point of the batch. Resolved one by one this costs three queries per instance, and the
     * Monitoring shell polls the listing unbounded every 30 s
     * (<a href="https://github.com/eclipse-dirigible/dirigible/issues/7250">#7250</a>).
     */
    @Test
    void theQueryCountDoesNotGrowWithTheListing() throws SQLException {
        List<ProcessInstance> few = start(WAITING_PROCESS, 2);
        List<ProcessInstance> many = start(WAITING_PROCESS, 8);

        long forFew = queriesResolving(few);
        long forMany = queriesResolving(many);

        assertEquals(forFew, forMany, "resolving four times as many instances must not cost a query more");
        assertEquals(3, forMany, "one execution query and the two job queries");
    }

    /**
     * The queries H2 runs while the given instances are resolved; the commits around them are not what
     * grows with the listing.
     */
    private static long queriesResolving(List<ProcessInstance> instances) throws SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", ""); Statement statement = connection.createStatement()) {
            // Switching collection on discards whatever was collected before.
            statement.execute("SET QUERY_STATISTICS TRUE");
            try {
                bpmProvider.getProcessInstanceActivityIds(instances);

                return countQueries(statement);
            } finally {
                statement.execute("SET QUERY_STATISTICS FALSE");
            }
        }
    }

    private static long countQueries(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery(
                "SELECT SUM(EXECUTION_COUNT) FROM INFORMATION_SCHEMA.QUERY_STATISTICS WHERE SQL_STATEMENT LIKE 'SELECT%'")) {
            return result.next() ? result.getLong(1) : 0;
        }
    }

    private static void assertStatus(ProcessInstance instance, String activityId, int positive, int negative) {
        Map<String, ActivityStatusData> statuses = bpmProvider.getProcessInstanceActiveActivityIds(instance.getId());

        assertEquals(Set.of(activityId), statuses.keySet(), "exactly the occupied activity must be reported");
        assertEquals(positive, statuses.get(activityId).positive, "positive count of [" + activityId + "]");
        assertEquals(negative, statuses.get(activityId).negative, "negative count of [" + activityId + "]");
    }

    /** Runs the pending attempt on this thread; it fails, and Flowable applies the retry decision. */
    private static void failNextAttempt(ProcessInstance instance) {
        Job job = engine.getManagementService()
                        .createJobQuery()
                        .processInstanceId(instance.getId())
                        .singleResult();

        assertThrows(RuntimeException.class, () -> engine.getManagementService()
                                                         .executeJob(job.getId()),
                "the doomed delegate must fail the attempt");
    }

    /** Exhausts the retry cycle: the first attempt parks the job, the second dead-letters it. */
    private static ProcessInstance deadLetter(ProcessInstance instance) {
        failNextAttempt(instance);
        engine.getManagementService()
              .moveTimerToExecutableJob(engine.getManagementService()
                                              .createTimerJobQuery()
                                              .processInstanceId(instance.getId())
                                              .singleResult()
                                              .getId());
        failNextAttempt(instance);

        return instance;
    }

    private static Job deadLetterJob(ProcessInstance instance) {
        return engine.getManagementService()
                     .createDeadLetterJobQuery()
                     .processInstanceId(instance.getId())
                     .singleResult();
    }

    private static long timerJobs(ProcessInstance instance) {
        return engine.getManagementService()
                     .createTimerJobQuery()
                     .processInstanceId(instance.getId())
                     .count();
    }

    private static List<String> activeActivityIds(ProcessInstance instance) {
        return engine.getRuntimeService()
                     .getActiveActivityIds(instance.getId());
    }

    private static ProcessInstance start(String processKey) {
        return engine.getRuntimeService()
                     .startProcessInstanceByKeyAndTenantId(processKey, null, Map.of(), TENANT_ID);
    }

    private static List<ProcessInstance> start(String processKey, int count) {
        return IntStream.range(0, count)
                        .mapToObj(each -> start(processKey))
                        .toList();
    }

    private static void deploy(String processKey, String xml) {
        engine.getRepositoryService()
              .createDeployment()
              .addString(processKey + ".bpmn20.xml", xml)
              .tenantId(TENANT_ID)
              .deploy();
    }

    /** The provider resolves its tenant per call; it must match the one the instances run in. */
    private static TenantContext tenantContext() {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(TENANT_ID);

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);

        return tenantContext;
    }
}
