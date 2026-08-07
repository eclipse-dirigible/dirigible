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
 * End-to-end test for the Java Camel handler integration: drops a {@code .java} class implementing
 * {@link org.apache.camel.Processor} and a {@code .camel} route into the registry, lets the
 * synchronizers compile + deploy, then invokes the exposed HTTP route and asserts the client Java
 * handler ran (it sets the response body).
 *
 * <p>
 * The route step {@code dirigible-java:<fqn>} is served by {@code DirigibleJavaComponent}, which
 * resolves the class through {@code ClientClassLoaderHolder} on every exchange — so the second test
 * proves a recompiled handler is picked up without a server restart (the {@code .camel} route is
 * untouched between runs). Counterpart of {@code JavaBpmnIT} for the Camel engine.
 */
class JavaCamelIT extends IntegrationTest {

    private static final String PROJECT = "java-camel-it";
    private static final String HTTP_PATH = "java-camel-it";
    private static final String HANDLER_FQN = "com.acme.camel.MyCamelHandler";

    private static final String HANDLER_SOURCE_LOCATION = "/" + PROJECT + "/" + HANDLER_FQN.replace('.', '/') + ".java";
    private static final String ROUTE_LOCATION = "/" + PROJECT + "/route.camel";

    private static final String HANDLER_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + HANDLER_SOURCE_LOCATION;
    private static final String ROUTE_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + ROUTE_LOCATION;

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void http_route_invokes_client_java_processor() {
        write(HANDLER_REGISTRY_PATH, handlerSource("Set by MyCamelHandler"), "text/x-java");
        write(ROUTE_REGISTRY_PATH, routeSource(), "text/plain");
        synchronizationProcessor.forceProcessSynchronizers();

        assertRouteBody("Set by MyCamelHandler");
    }

    /**
     * The {@code dirigible-java} component resolves the handler on every exchange, so a recompiled
     * class must be observed without a restart. Deploys v1, invokes the route, overwrites the same
     * {@code .java} with v2 (the {@code .camel} route is untouched) and invokes again — the second
     * response must reflect v2.
     */
    @Test
    void recompiled_processor_is_reflected_without_restart() {
        write(HANDLER_REGISTRY_PATH, handlerSource("Set by MyCamelHandler v1"), "text/x-java");
        write(ROUTE_REGISTRY_PATH, routeSource(), "text/plain");
        synchronizationProcessor.forceProcessSynchronizers();

        assertRouteBody("Set by MyCamelHandler v1");

        write(HANDLER_REGISTRY_PATH, handlerSource("Set by MyCamelHandler v2"), "text/x-java");
        synchronizationProcessor.forceProcessSynchronizers();

        assertRouteBody("Set by MyCamelHandler v2");
    }

    @AfterEach
    void cleanup() {
        boolean removed = false;
        for (String path : new String[] {ROUTE_REGISTRY_PATH, HANDLER_REGISTRY_PATH}) {
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

    private void assertRouteBody(String expectedBody) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/integrations/" + HTTP_PATH)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedBody)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private static String handlerSource(String body) {
        return """
                package com.acme.camel;
                import org.apache.camel.Exchange;
                import org.apache.camel.Processor;
                public class MyCamelHandler implements Processor {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getMessage().setBody("%s");
                    }
                }
                """.formatted(body);
    }

    private static String routeSource() {
        return """
                - route:
                    id: java-camel-it-route
                    from:
                      id: from-java-camel-it
                      description: Expose path /services/integrations/java-camel-it
                      uri: platform-http
                      parameters:
                        path: java-camel-it
                        httpMethodRestrict: GET
                      steps:
                        - to:
                            id: to-java-camel-it
                            description: Java handler
                            uri: dirigible-java
                            parameters:
                              className: %s
                """.formatted(HANDLER_FQN);
    }

}
