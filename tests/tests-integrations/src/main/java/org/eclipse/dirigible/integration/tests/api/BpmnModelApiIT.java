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

import io.restassured.http.ContentType;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end test for the BPMN editor model REST API.
 *
 * <p>
 * Verifies the three endpoints the browser BPMN modeler depends on:
 * <ul>
 * <li>{@code GET /services/bpm/models/{workspace}/{project}/{path}} — returns process metadata and
 * the Oryx JSON model for a {@code .bpmn} file stored in the user workspace.</li>
 * <li>{@code GET /services/bpm/stencil-sets} — returns the BPMN stencil set definition used to
 * populate the modeler palette.</li>
 * </ul>
 *
 * <p>
 * Files are written directly into the repository at the workspace path the endpoint resolves to for
 * the {@code admin} user — no browser or synchronizer required.
 */
class BpmnModelApiIT extends IntegrationTest {

    private static final String USERNAME = "admin";
    private static final String WORKSPACE = "bpmn-model-api-it";
    private static final String PROJECT = "bpmn-model-api-it-project";
    private static final String BPMN_FILE = "modeler-test-process.bpmn";

    private static final String WORKSPACE_FILE_PATH = "/users/" + USERNAME + "/" + WORKSPACE + "/" + PROJECT + "/" + BPMN_FILE;

    private static final String MODEL_ENDPOINT = "/services/bpm/models/" + WORKSPACE + "/" + PROJECT + "/" + BPMN_FILE;

    private static final String STENCIL_SETS_ENDPOINT = "/services/bpm/stencil-sets";

    private static final String PROCESS_ID = "modeler-test-process";
    private static final String PROCESS_NAME = "Modeler Test Process";

    /**
     * Minimal valid Flowable BPMN process: Start → Service Task (JSTask handler) → User Task →
     * Exclusive Gateway → End. Mirrors the real patterns from Codbex sample projects (leave-request,
     * customer-onboarding).
     */
    private static final String SIMPLE_BPMN = """
            <?xml version='1.0' encoding='UTF-8'?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xmlns:flowable="http://flowable.org/bpmn"
                         xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                         xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
                         xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
                         targetNamespace="http://www.flowable.org/processdef"
                         exporter="Flowable Open Source Modeler" exporterVersion="6.8.1">
              <process id="modeler-test-process" name="Modeler Test Process" isExecutable="true">
                <startEvent id="start-event"/>
                <serviceTask id="notify-task" name="Notify" flowable:async="true" flowable:delegateExpression="${JSTask}">
                  <extensionElements>
                    <flowable:field name="handler">
                      <flowable:string><![CDATA[bpmn-model-api-it-project/tasks/notify.ts]]></flowable:string>
                    </flowable:field>
                  </extensionElements>
                </serviceTask>
                <userTask id="review-task" name="Review Request" flowable:candidateGroups="ADMINISTRATOR"/>
                <exclusiveGateway id="decision" default="declined-flow"/>
                <endEvent id="end-event"/>
                <sequenceFlow id="flow1" sourceRef="start-event" targetRef="notify-task"/>
                <sequenceFlow id="flow2" sourceRef="notify-task" targetRef="review-task"/>
                <sequenceFlow id="flow3" sourceRef="review-task" targetRef="decision"/>
                <sequenceFlow id="approved-flow" sourceRef="decision" targetRef="end-event">
                  <conditionExpression xsi:type="tFormalExpression"><![CDATA[${approved}]]></conditionExpression>
                </sequenceFlow>
                <sequenceFlow id="declined-flow" sourceRef="decision" targetRef="end-event"/>
              </process>
              <bpmndi:BPMNDiagram id="BPMNDiagram_modeler-test">
                <bpmndi:BPMNPlane bpmnElement="modeler-test-process" id="BPMNPlane_modeler-test">
                  <bpmndi:BPMNShape bpmnElement="start-event" id="BPMNShape_start-event">
                    <omgdc:Bounds height="30.0" width="30.0" x="100.0" y="85.0"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape bpmnElement="notify-task" id="BPMNShape_notify-task">
                    <omgdc:Bounds height="80.0" width="100.0" x="180.0" y="60.0"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape bpmnElement="review-task" id="BPMNShape_review-task">
                    <omgdc:Bounds height="80.0" width="100.0" x="330.0" y="60.0"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape bpmnElement="decision" id="BPMNShape_decision">
                    <omgdc:Bounds height="40.0" width="40.0" x="480.0" y="80.0"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNShape bpmnElement="end-event" id="BPMNShape_end-event">
                    <omgdc:Bounds height="28.0" width="28.0" x="570.0" y="86.0"/>
                  </bpmndi:BPMNShape>
                  <bpmndi:BPMNEdge bpmnElement="flow1" id="BPMNEdge_flow1">
                    <omgdi:waypoint x="130.0" y="100.0"/>
                    <omgdi:waypoint x="180.0" y="100.0"/>
                  </bpmndi:BPMNEdge>
                  <bpmndi:BPMNEdge bpmnElement="flow2" id="BPMNEdge_flow2">
                    <omgdi:waypoint x="280.0" y="100.0"/>
                    <omgdi:waypoint x="330.0" y="100.0"/>
                  </bpmndi:BPMNEdge>
                  <bpmndi:BPMNEdge bpmnElement="flow3" id="BPMNEdge_flow3">
                    <omgdi:waypoint x="430.0" y="100.0"/>
                    <omgdi:waypoint x="480.0" y="100.0"/>
                  </bpmndi:BPMNEdge>
                  <bpmndi:BPMNEdge bpmnElement="approved-flow" id="BPMNEdge_approved-flow">
                    <omgdi:waypoint x="520.0" y="100.0"/>
                    <omgdi:waypoint x="570.0" y="100.0"/>
                  </bpmndi:BPMNEdge>
                  <bpmndi:BPMNEdge bpmnElement="declined-flow" id="BPMNEdge_declined-flow">
                    <omgdi:waypoint x="500.0" y="120.0"/>
                    <omgdi:waypoint x="584.0" y="114.0"/>
                  </bpmndi:BPMNEdge>
                </bpmndi:BPMNPlane>
              </bpmndi:BPMNDiagram>
            </definitions>
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void cleanup() {
        if (repository.hasResource(WORKSPACE_FILE_PATH)) {
            repository.removeResource(WORKSPACE_FILE_PATH);
        }
    }

    @Test
    void getModel_returns_process_metadata_for_bpmn_file() {
        repository.createResource(WORKSPACE_FILE_PATH, SIMPLE_BPMN.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(MODEL_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .contentType(ContentType.JSON)
                                                 .body("key", equalTo(PROCESS_ID))
                                                 .body("name", equalTo(PROCESS_NAME))
                                                 .body("model", notNullValue()),
                30);
    }

    @Test
    void stencilSets_returns_bpmn_stencil_definition() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(STENCIL_SETS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .contentType(ContentType.JSON)
                                                 .body("namespace", containsString("bpmn2.0"))
                                                 .body("title", equalTo("Process editor")));
    }
}
