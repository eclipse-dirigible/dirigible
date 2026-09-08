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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * End-to-end coverage for dependency injection in client {@code JavaDelegate}s (dirigible #7058): a
 * delegate is created by Flowable, so it is never a container-owned bean, and until now
 * {@code @Inject} had no effect on it - the collaborator read {@code null} at runtime while the
 * container looked correctly wired.
 *
 * <p>
 * The fixture project under {@code src/main/resources/JavaDelegateInjectionIT} exercises both
 * delegate paths in one process - {@code flowable:class} and
 * {@code flowable:delegateExpression="${JavaTask}"} - with constructor, field and collection
 * injection, a {@code @PostConstruct}-only delegate, and the two delegates that declare no
 * injection point at all (which must keep being built exactly as before). Two further processes pin
 * the behaviour that is easiest to regress: an unsatisfiable dependency must fail the STEP and not
 * the deployment, and a recompiled collaborator must reach the delegate Flowable caches on the
 * parsed activity.
 */
// One Dirigible boot for the whole class: the fixture is deployed once and every method starts its
// own process instance, so the per-method context reset inherited from IntegrationTest would only
// add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JavaDelegateInjectionIT extends IntegrationTest {

    private static final String PROJECT = "JavaDelegateInjectionIT";
    private static final String INJECTION_PROCESS = "java-delegate-injection";
    private static final String UNSATISFIED_PROCESS = "java-delegate-unsatisfied";
    private static final String VERSION_PROCESS = "java-delegate-version";

    private static final String VERSION_PROVIDER_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/delegateinjection/VersionProvider.java";

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    private static boolean deployed;

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void deployFixtureOnce() {
        if (deployed) {
            return;
        }
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);
        deployed = true;
    }

    @Test
    void both_delegate_paths_wire_their_collaborators() {
        String instanceId = startProcess(INJECTION_PROCESS);

        // flowable:class, single constructor taking the @Component collaborator.
        assertHistoricVariable(instanceId, "ctorRate", "42");
        // ${JavaTask} + handler, @Inject field.
        assertHistoricVariable(instanceId, "fieldRate", "42");
        // Collection injection over a plain-interface extension point.
        assertHistoricVariable(instanceId, "rules", "A,B");
        // Declares only a lifecycle callback, and it still runs.
        assertHistoricVariable(instanceId, "postConstructRan", "yes");
        // Declares no injection point: built by the caller's own reflection, exactly as before.
        assertHistoricVariable(instanceId, "plainClassRan", "yes");
        assertHistoricVariable(instanceId, "plainTaskRan", "yes");
    }

    @Test
    void an_unsatisfiable_dependency_fails_the_step_not_the_deployment() {
        String instanceId = startProcess(UNSATISFIED_PROCESS);

        // The step's own failure path: the retry cycle is exhausted and the job dead-letters, which is
        // where an intent step's retry: / onError: picks it up. Turning this into a publish-time
        // wiring error would be the regression.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instance/" + instanceId + "/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", greaterThan(0)),
                ASSERTION_TIMEOUT_SECONDS);
        assertNoRuntimeVariable(instanceId, "unsatisfiedRan");

        // The deployment is intact: the healthy process in the same project still runs end to end.
        assertHistoricVariable(startProcess(INJECTION_PROCESS), "ctorRate", "42");
    }

    @Test
    void a_recompiled_collaborator_reaches_the_cached_delegate() {
        assertHistoricVariable(startProcess(VERSION_PROCESS), "version", "v1");

        write(VERSION_PROVIDER_PATH, versionProviderSource("v2"));
        synchronizationProcessor.forceProcessSynchronizers();

        // Flowable caches the delegate on the parsed activity; the client rebuild evicts the
        // process-definition cache, so the next start re-creates it and re-wires it against the new
        // generation's singletons.
        assertHistoricVariable(startProcess(VERSION_PROCESS), "version", "v2");
    }

    private String startProcess(String processDefinitionKey) {
        String body = "{\"processDefinitionKey\":\"" + processDefinitionKey + "\",\"businessKey\":\"" + processDefinitionKey
                + "\",\"parameters\":\"{}\"}";
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

    private void assertNoRuntimeVariable(String processInstanceId, String name) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instance/" + processInstanceId + "/variables")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("variableName", not(hasItem(name))));
    }

    private void write(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
    }

    private static String versionProviderSource(String version) {
        return """
                package delegateinjection;

                import org.eclipse.dirigible.sdk.component.Component;

                @Component
                public class VersionProvider {

                    public String version() {
                        return "%s";
                    }
                }
                """.formatted(version);
    }

}
