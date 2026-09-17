/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * End-to-end test for the "which step is this instance on" answer of a process parked on an
 * asynchronous step, i.e. every step of an intent-generated process
 * (<a href="https://github.com/eclipse-dirigible/dirigible/issues/7193">#7193</a>).
 *
 * <p>
 * Drops a {@code .java} delegate that always fails and a {@code .bpmn} whose single service task is
 * {@code flowable:async} with a retry cycle into the registry, starts the process through the
 * public BPM REST endpoint, and asserts that once the first attempt has failed - the state a
 * retrying process spends its whole retry interval in, and the one Flowable's own
 * {@code getActiveActivityIds} reports nothing for - both
 * {@code /bpm-processes/instance/{id}/active} and the instance listing name that step.
 */
// One Dirigible boot for the whole class: the test cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add boot time.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BpmActiveActivitiesIT extends IntegrationTest {

    private static final String PROJECT = "bpm-active-activities-it";
    private static final String PROCESS_KEY = "bpm-active-activities-it-process";
    private static final String BUSINESS_KEY = "bpm-active-activities-it-key";

    /** No hyphen: the id is read as a GPath property of the answered map. */
    private static final String STEP_ID = "doomedStep";

    private static final String DELEGATE_FQN = "com.acme.DoomedTask";
    private static final String FAILURE_MESSAGE = "doomed step refused the attempt";

    /**
     * Five attempts an hour apart. The cycle is load-bearing: without one, a failed job is parked for
     * the default failed-job wait time (10 s) and the executor dead-letters it within half a minute, so
     * the state under test would expire mid-test. An hour makes it hold.
     */
    private static final String RETRY_CYCLE = "R5/PT1H";

    private static final String DELEGATE_REGISTRY_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/" + DELEGATE_FQN.replace('.', '/') + ".java";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/process.bpmn";

    private static final Duration PARKING_TIMEOUT = Duration.ofSeconds(90);

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private ProcessEngine processEngine;

    @Test
    void aStepParkedBetweenRetryAttemptsIsReportedAsTheCurrentActivity() {
        write(DELEGATE_REGISTRY_PATH, delegateSource(), "text/x-java");
        write(BPMN_REGISTRY_PATH, bpmnSource(), "application/xml");
        synchronizationProcessor.forceProcessSynchronizers();

        String processInstanceId = startProcess();
        Job parkedJob = awaitParkedJob(processInstanceId);

        // The delegate's own message proves the attempt really ran the compiled client class and
        // failed in it - without this the test would also pass on a job that never resolved it.
        assertThat(parkedJob.getExceptionMessage()).contains(FAILURE_MESSAGE);
        assertThat(processEngine.getRuntimeService()
                                .getActiveActivityIds(processInstanceId)).describedAs(
                                        "Flowable reports no active activity while a retry is pending - the reason the endpoint used to answer {}")
                                                                         .isEmpty();

        assertReportsCurrentStep(processInstanceId);
        assertListingReportsCurrentStep(processInstanceId);
    }

    @AfterEach
    void cleanup() {
        boolean removed = false;
        for (String path : new String[] {BPMN_REGISTRY_PATH, DELEGATE_REGISTRY_PATH}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                removed = true;
            }
        }
        if (removed) {
            // Undeploying the process cascades, dropping the parked instance and its timer job.
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void assertReportsCurrentStep(String processInstanceId) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instance/" + processInstanceId + "/active")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(1))
                                                 .body(STEP_ID + ".positive", equalTo(1))
                                                 .body(STEP_ID + ".negative", equalTo(0)));
    }

    private void assertListingReportsCurrentStep(String processInstanceId) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instances?key=" + PROCESS_KEY)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.id == '" + processInstanceId + "' }.activityId", equalTo(STEP_ID)));
    }

    /**
     * Waits for the instance to be parked between attempts - the compile pass plus the first
     * asynchronous acquisition take seconds - and answers the job holding it there.
     *
     * @param processInstanceId the process instance id
     * @return the parked timer job
     */
    private Job awaitParkedJob(String processInstanceId) {
        await().atMost(PARKING_TIMEOUT)
               .pollInterval(Duration.ofMillis(500))
               .until(() -> parkedJob(processInstanceId) != null);

        return parkedJob(processInstanceId);
    }

    private Job parkedJob(String processInstanceId) {
        return processEngine.getManagementService()
                            .createTimerJobQuery()
                            .processInstanceId(processInstanceId)
                            .singleResult();
    }

    private void write(String path, String content, String contentType) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, contentType, true);
    }

    private String startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + BUSINESS_KEY + "\",\"parameters\":\"{}\"}";
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString());
    }

    private static String delegateSource() {
        return """
                package com.acme;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;
                public class DoomedTask implements JavaDelegate {
                    @Override
                    public void execute(DelegateExecution execution) {
                        throw new IllegalStateException("%s");
                    }
                }
                """.formatted(FAILURE_MESSAGE);
    }

    private static String bpmnSource() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="BPM Active Activities IT" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="%s"/>
                    <serviceTask id="%s" name="Doomed Step" flowable:async="true" flowable:class="%s">
                      <extensionElements>
                        <flowable:failedJobRetryTimeCycle>%s</flowable:failedJobRetryTimeCycle>
                      </extensionElements>
                    </serviceTask>
                    <sequenceFlow id="f2" sourceRef="%s" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY, STEP_ID, STEP_ID, DELEGATE_FQN, RETRY_CYCLE, STEP_ID);
    }

}
