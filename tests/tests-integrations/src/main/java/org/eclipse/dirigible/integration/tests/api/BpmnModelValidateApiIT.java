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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * The BPMN modeler's Validate button endpoint (issue #8075).
 *
 * <p>
 * {@code POST /services/bpm/models/validate} runs Flowable's process validator over the posted
 * modeler JSON and returns the problems the editor's validation popup renders. The modeler loads a
 * model with {@code GET /models/{workspace}/{project}/{path}} and posts its {@code model} node to
 * validate, so this drives the same two calls: it loads an invalid model (a service task with no
 * implementation, which is what an unresolved {@code flowable:class} looks like) and asserts the
 * problem comes back, and loads a valid model and asserts it does not.
 */
// One Dirigible boot for the whole class (as BpmnModelApiIT): each method cleans up after itself.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BpmnModelValidateApiIT extends IntegrationTest {

    private static final String USERNAME = "admin";
    private static final String WORKSPACE = "bpmn-validate-it";
    private static final String PROJECT = "bpmn-validate-it-project";

    private static final String VALIDATE_ENDPOINT = "/services/bpm/models/validate";
    private static final String MISSING_IMPLEMENTATION = "flowable-servicetask-missing-implementation";
    private static final String BROKEN_SERVICE_TASK_ID = "broken-task";

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private String writtenFilePath;

    @AfterEach
    void cleanup() {
        if (writtenFilePath != null && repository.hasResource(writtenFilePath)) {
            repository.removeResource(writtenFilePath);
            writtenFilePath = null;
        }
    }

    @Test
    void invalid_model_reports_the_missing_service_task_implementation() {
        Map<String, Object> model = loadModelAsModeler("invalid-process.bpmn", INVALID_BPMN);

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(model)
                                                 .when()
                                                 .post(VALIDATE_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.problem == '" + MISSING_IMPLEMENTATION + "' }",
                                                         hasSize(greaterThanOrEqualTo(1)))
                                                 .body("find { it.problem == '" + MISSING_IMPLEMENTATION + "' }.activityId",
                                                         equalTo(BROKEN_SERVICE_TASK_ID)));
    }

    @Test
    void valid_model_reports_no_missing_implementation() {
        Map<String, Object> model = loadModelAsModeler("valid-process.bpmn", VALID_BPMN);

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(model)
                                                 .when()
                                                 .post(VALIDATE_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.problem == '" + MISSING_IMPLEMENTATION + "' }", hasSize(0)));
    }

    /**
     * Writes a {@code .bpmn} into the workspace and reads it back as the modeler does, returning the
     * {@code model} node (the stencil JSON) the Validate popup posts.
     */
    private Map<String, Object> loadModelAsModeler(String fileName, String bpmn) {
        writtenFilePath = "/users/" + USERNAME + "/" + WORKSPACE + "/" + PROJECT + "/" + fileName;
        repository.createResource(writtenFilePath, bpmn.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);

        String modelEndpoint = "/services/bpm/models/" + WORKSPACE + "/" + PROJECT + "/" + fileName;
        @SuppressWarnings("unchecked")
        Map<String, Object>[] holder = new Map[1];
        restAssuredExecutor.execute(() -> holder[0] = given().when()
                                                             .get(modelEndpoint)
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("model"),
                30);
        return holder[0];
    }

    private static final String DIAGRAM =
            """
                      <bpmndi:BPMNDiagram id="d">
                        <bpmndi:BPMNPlane bpmnElement="%s" id="p">
                          <bpmndi:BPMNShape bpmnElement="start" id="s_start"><omgdc:Bounds height="30.0" width="30.0" x="100.0" y="85.0"/></bpmndi:BPMNShape>
                          <bpmndi:BPMNShape bpmnElement="%s" id="s_mid"><omgdc:Bounds height="80.0" width="100.0" x="180.0" y="60.0"/></bpmndi:BPMNShape>
                          <bpmndi:BPMNShape bpmnElement="end" id="s_end"><omgdc:Bounds height="28.0" width="28.0" x="330.0" y="86.0"/></bpmndi:BPMNShape>
                          <bpmndi:BPMNEdge bpmnElement="f1" id="e_f1"><omgdi:waypoint x="130.0" y="100.0"/><omgdi:waypoint x="180.0" y="100.0"/></bpmndi:BPMNEdge>
                          <bpmndi:BPMNEdge bpmnElement="f2" id="e_f2"><omgdi:waypoint x="280.0" y="100.0"/><omgdi:waypoint x="330.0" y="100.0"/></bpmndi:BPMNEdge>
                        </bpmndi:BPMNPlane>
                      </bpmndi:BPMNDiagram>
                    """;

    private static String process(String processId, String middle) {
        return """
                <?xml version='1.0' encoding='UTF-8'?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
                             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Validate IT" isExecutable="true">
                    <startEvent id="start"/>
                    %s
                    <endEvent id="end"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="%s"/>
                    <sequenceFlow id="f2" sourceRef="%s" targetRef="end"/>
                  </process>
                %s
                </definitions>
                """.formatted(processId, middle, middleId(middle), middleId(middle), DIAGRAM.formatted(processId, middleId(middle)));
    }

    private static String middleId(String middle) {
        return middle.contains(BROKEN_SERVICE_TASK_ID) ? BROKEN_SERVICE_TASK_ID : "review";
    }

    /** A service task with no class/expression/delegateExpression/type - the missing implementation. */
    private static final String INVALID_BPMN =
            process("bpmn-validate-it-invalid", "<serviceTask id=\"" + BROKEN_SERVICE_TASK_ID + "\" name=\"Broken\"/>");

    /** A user task needs no implementation, so the model is valid. */
    private static final String VALID_BPMN =
            process("bpmn-validate-it-valid", "<userTask id=\"review\" name=\"Review\" flowable:candidateGroups=\"ADMINISTRATOR\"/>");
}
