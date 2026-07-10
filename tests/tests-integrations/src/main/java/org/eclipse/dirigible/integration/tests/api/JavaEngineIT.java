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

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the engine-java module: drops {@code .java} sources directly under
 * {@code /registry/public/...}, triggers a forced synchronization, and asserts the
 * {@code /services/java/...} endpoint reflects the latest state.
 *
 * <p>
 * The test exercises the full reconciliation loop without the IDE: create → hot-reload (modify) →
 * delete → compile-error. Each case verifies the synchronizer's CREATE/UPDATE/DELETE/FAILED
 * handling end-to-end through the HTTP boundary.
 */
class JavaEngineIT extends IntegrationTest {

    /** Project segment used for all sources in this IT. */
    private static final String PROJECT = "java-engine-it";

    /** Registry-relative source path used throughout. */
    private static final String SOURCE_LOCATION = "/" + PROJECT + "/demo/Hello.java";

    /** Fully-qualified path under the registry root. */
    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + SOURCE_LOCATION;

    /** Endpoint where a successfully-loaded {@code demo.Hello} handler answers. */
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/demo/Hello";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void create_then_serve() {
        writeAndSync(handlerSource("v1"));
        assertEndpointReturns(200, "hello from v1");
    }

    @Test
    void hot_reload_replaces_handler_response() {
        writeAndSync(handlerSource("v1"));
        assertEndpointReturns(200, "hello from v1");

        writeAndSync(handlerSource("v2"));
        // Same URL, but a fresh ClassLoader has replaced the prior one; v1's body must be gone.
        assertEndpointReturns(200, "hello from v2");
    }

    @Test
    void delete_unregisters_handler() {
        writeAndSync(handlerSource("v1"));
        assertEndpointReturns(200, "hello from v1");

        repository.removeResource(REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        // Cleanup phase ran: the handler is no longer in the registry. We don't assert on the
        // response body — Spring Boot 4's default error handling does not include the
        // ResponseStatusException reason in the JSON body without per-request opt-in, so the
        // status code is the contract we exercise here.
        assertEndpointStatus(404);
    }

    @Test
    void compile_error_keeps_endpoint_unregistered() {
        // Body references a non-existent symbol — javac diagnostic surfaces, handler stays absent.
        String broken = """
                package demo;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class Hello implements JavaHandler {
                    public void handle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                        thisMethodDoesNotExist(req);
                    }
                }
                """;
        writeAndSync(broken);
        assertEndpointStatus(404);
    }

    @Test
    void broken_definition_self_heals_without_a_byte_change() {
        // A duplicate FQN breaks the SECOND source's parse (definition state BROKEN) - one
        // deterministic instance of the "transient parse failure" class. The regression this guards:
        // a BROKEN definition used to be skipped until its CONTENT changed, so removing the cause
        // (the first duplicate) never healed the second source, and - because one unparsed .java is
        // invisible to the registry-wide javac batch - the whole client codebase stayed broken until
        // someone edited the file's bytes.
        writeAndSync(handlerSource("original"));
        assertEndpointReturns(200, "hello from original");

        String duplicatePath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/other/Hello.java";
        repository.createResource(duplicatePath, handlerSource("duplicate").getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
        // The duplicate is rejected; the original keeps serving.
        assertEndpointReturns(200, "hello from original");

        // Remove the ORIGINAL - the duplicate's bytes stay untouched. The heal needs two passes
        // (one whose cleanup drops the stale FQN claim, one whose retry re-parses), and a concurrent
        // scheduled run can consume a force after its parse phase already ran - so force a few.
        repository.removeResource(REGISTRY_PATH);
        for (int i = 0; i < 3; i++) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
        assertEndpointReturns(200, "hello from duplicate");

        repository.removeResource(duplicatePath);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @AfterEach
    void removeArtefactFromRegistry() {
        // Best-effort cleanup so the next test starts from a clean registry. The
        // @DirtiesContext on IntegrationTest resets the Spring context separately.
        if (repository.hasResource(REGISTRY_PATH)) {
            repository.removeResource(REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void writeAndSync(String source) {
        repository.createResource(REGISTRY_PATH, source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /**
     * Polls the endpoint until the assertion holds, capped at {@value #ASSERTION_TIMEOUT_SECONDS}
     * seconds. The cap is required because {@code forceProcessSynchronizers()} returns synchronously
     * but the in-process compile + class-define cycle (including the one-time fat-jar extraction on the
     * first invocation per Spring context) can still finish a few hundred milliseconds after the call
     * returns, depending on scheduler scheduling. The framework's retry helper handles exactly this
     * scenario.
     */
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    private void assertEndpointReturns(int expectedStatus, String expectedBodyFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(expectedStatus)
                                                 .body(containsString(expectedBodyFragment)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void assertEndpointStatus(int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENDPOINT)
                                                 .then()
                                                 .statusCode(expectedStatus),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private static String handlerSource(String tag) {
        return """
                package demo;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class Hello implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.setContentType("application/json");
                        response.getWriter().write("{\\"message\\": \\"hello from %s\\"}");
                    }
                }
                """.formatted(tag);
    }

}
