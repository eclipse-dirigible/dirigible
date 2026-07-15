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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * End-to-end test for the Java BPMN handler integration: drops a {@code .java} delegate and a
 * {@code .bpmn} process into the registry, lets the synchronizers compile + deploy, then starts the
 * process through the public BPM REST endpoint and asserts that both delegate paths ran.
 *
 * <p>
 * Two service tasks exercise both handler styles within the same process:
 * <ul>
 * <li>{@code flowable:delegateExpression="${JavaTask}"} with a {@code handler} field carrying the
 * FQN — resolved by {@code DirigibleJavaCallDelegate} via {@code ClientClassLoaderHolder}.</li>
 * <li>{@code flowable:class="com.acme.PureJavaTask"} — resolved through Flowable's own
 * {@code ReflectUtil.loadClass} via the {@code ClientAwareClassLoader} installed on the engine
 * config.</li>
 * </ul>
 * Each delegate writes a process variable; the test asserts both variables made it into the
 * historic record, proving end-to-end execution.
 */
class JavaBpmnIT extends IntegrationTest {

    private static final String PROJECT = "java-bpmn-it";
    private static final String PROCESS_KEY = "java-bpmn-it-process";

    private static final String JAVA_TASK_FQN = "com.acme.MyJavaTask";
    private static final String PURE_TASK_FQN = "com.acme.PureJavaTask";

    private static final String JAVA_TASK_SOURCE_LOCATION = "/" + PROJECT + "/" + JAVA_TASK_FQN.replace('.', '/') + ".java";
    private static final String PURE_TASK_SOURCE_LOCATION = "/" + PROJECT + "/" + PURE_TASK_FQN.replace('.', '/') + ".java";
    private static final String BPMN_LOCATION = "/" + PROJECT + "/process.bpmn";

    private static final String JAVA_TASK_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + JAVA_TASK_SOURCE_LOCATION;
    private static final String PURE_TASK_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + PURE_TASK_SOURCE_LOCATION;
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + BPMN_LOCATION;

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void start_process_executes_both_java_delegate_styles() {
        write(JAVA_TASK_REGISTRY_PATH, javaTaskSource(), "text/x-java");
        write(PURE_TASK_REGISTRY_PATH, pureTaskSource(), "text/x-java");
        write(BPMN_REGISTRY_PATH, bpmnSource(), "application/xml");
        synchronizationProcessor.forceProcessSynchronizers();

        String processInstanceId = startProcess();
        assertHistoricVariable(processInstanceId, "javaTaskRan", "yes");
        assertHistoricVariable(processInstanceId, "pureTaskRan", "yes");
    }

    /**
     * A {@code flowable:class} delegate must pick up a recompiled version without a server restart.
     * Deploys a v1 delegate, runs the process, then overwrites the same {@code .java} with v2 (the
     * {@code .bpmn} is untouched) and runs again — the second run must observe v2. Without the
     * per-rebuild classloader refresh, the JVM's initiating-loader cache keeps returning v1.
     */
    @Test
    void pure_class_delegate_reflects_a_recompiled_version_without_restart() {
        write(JAVA_TASK_REGISTRY_PATH, javaTaskSource(), "text/x-java");
        write(PURE_TASK_REGISTRY_PATH, versionedPureTaskSource("v1"), "text/x-java");
        write(BPMN_REGISTRY_PATH, bpmnSource(), "application/xml");
        synchronizationProcessor.forceProcessSynchronizers();

        String firstInstanceId = startProcess();
        assertHistoricVariable(firstInstanceId, "pureVersion", "v1");

        write(PURE_TASK_REGISTRY_PATH, versionedPureTaskSource("v2"), "text/x-java");
        synchronizationProcessor.forceProcessSynchronizers();

        String secondInstanceId = startProcess();
        assertHistoricVariable(secondInstanceId, "pureVersion", "v2");
    }

    @AfterEach
    void cleanup() {
        boolean removed = false;
        for (String path : new String[] {BPMN_REGISTRY_PATH, JAVA_TASK_REGISTRY_PATH, PURE_TASK_REGISTRY_PATH}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                removed = true;
            }
        }
        if (removed) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void write(String path, String content, String contentType) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, contentType, true);
    }

    private String startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"java-bpmn-it\",\"parameters\":\"{}\"}";
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString());
    }

    private void assertHistoricVariable(String processInstanceId, String name, String expectedValue) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/historic-instances/" + processInstanceId + "/variables")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("variableName", hasItem(name))
                                                 .body("find { it.variableName == '" + name + "' }.value", equalTo(expectedValue)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private static String javaTaskSource() {
        return """
                package com.acme;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;
                public class MyJavaTask implements JavaDelegate {
                    @Override
                    public void execute(DelegateExecution execution) {
                        execution.setVariable("javaTaskRan", "yes");
                    }
                }
                """;
    }

    private static String pureTaskSource() {
        return """
                package com.acme;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;
                public class PureJavaTask implements JavaDelegate {
                    @Override
                    public void execute(DelegateExecution execution) {
                        execution.setVariable("pureTaskRan", "yes");
                    }
                }
                """;
    }

    private static String versionedPureTaskSource(String version) {
        return """
                package com.acme;
                import org.flowable.engine.delegate.DelegateExecution;
                import org.flowable.engine.delegate.JavaDelegate;
                public class PureJavaTask implements JavaDelegate {
                    @Override
                    public void execute(DelegateExecution execution) {
                        execution.setVariable("pureVersion", "%s");
                    }
                }
                """.formatted(version);
    }

    private static String bpmnSource() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Java BPMN IT" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="java-task"/>
                    <serviceTask id="java-task" name="JavaTask delegate" flowable:delegateExpression="${JavaTask}">
                      <extensionElements>
                        <flowable:field name="handler">
                          <flowable:string><![CDATA[%s]]></flowable:string>
                        </flowable:field>
                      </extensionElements>
                    </serviceTask>
                    <sequenceFlow id="f2" sourceRef="java-task" targetRef="pure-task"/>
                    <serviceTask id="pure-task" name="Pure class delegate" flowable:class="%s"/>
                    <sequenceFlow id="f3" sourceRef="pure-task" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY, JAVA_TASK_FQN, PURE_TASK_FQN);
    }

}
