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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An artefact the synchronizer cannot reconcile is surfaced in the IDE Problems view, not only
 * logged on the server (issue #8255).
 *
 * <p>
 * A {@code .bpmn} the embedded engine refuses at deploy - here a service task with no
 * implementation - is registered {@code FAILED} and used to leave no trace in front of the
 * developer. It now becomes a Problems-view entry under the {@code Synchronization} category, keyed
 * to the source file, and the entry clears when the file is fixed on a later pass.
 */
@Tag("slow")
class SynchronizerFailedProblemIT extends IntegrationTest {

    private static final String PROJECT = "synchronizer-failed-problem-it";
    private static final String LOCATION = "/" + PROJECT + "/process.bpmn";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + LOCATION;
    private static final String PROBLEMS = "/services/ide/problems";
    private static final String OUR_PROBLEM = "findAll { it.category == 'Synchronization' && it.location == '" + LOCATION + "' }";
    private static final long TIMEOUT_SECONDS = 60;

    /** A service task with no implementation - the engine refuses to deploy it. */
    private static final String INVALID_BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         targetNamespace="http://www.flowable.org/processdef">
              <process id="synchronizer-failed-problem-it-process" name="Invalid" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="broken"/>
                <serviceTask id="broken" name="Broken"/>
                <sequenceFlow id="f2" sourceRef="broken" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;

    /** The same process with a user task instead - valid, so it deploys and the problem clears. */
    private static final String VALID_BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://www.flowable.org/processdef">
              <process id="synchronizer-failed-problem-it-process" name="Valid" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="review"/>
                <userTask id="review" name="Review" flowable:candidateGroups="ADMINISTRATOR"/>
                <sequenceFlow id="f2" sourceRef="review" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void cleanup() {
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void a_failed_deploy_is_surfaced_in_the_problems_view_and_clears_when_fixed() {
        publish(INVALID_BPMN);

        // The failure is now a Problems-view entry naming the file, with the deploy error as its cause.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OUR_PROBLEM, hasSize(1))
                                                 // The cause is the real deploy failure with the engine's
                                                 // reason appended (#8269), not the nested "Undepleted..." wrapper.
                                                 .body(OUR_PROBLEM + ".cause[0]", containsString("deploy"))
                                                 .body(OUR_PROBLEM + ".cause[0]", containsString("mandatory"))
                                                 .body(OUR_PROBLEM + ".cause[0]", not(containsString("Undepleted"))),
                TIMEOUT_SECONDS);

        // Fixing the file heals the artefact on the next pass, and its problem is cleared.
        publish(VALID_BPMN);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OUR_PROBLEM, hasSize(0)),
                TIMEOUT_SECONDS);
    }

    private void publish(String bpmn) {
        repository.createResource(BPMN_REGISTRY_PATH, bpmn.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
