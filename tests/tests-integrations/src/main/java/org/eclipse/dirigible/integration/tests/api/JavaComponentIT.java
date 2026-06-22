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

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the client-Java bean container: a {@code @Controller} that receives a
 * {@code @Component} service and a {@code List<Greeter>} (collection injection) through its
 * constructor, served over {@code /services/java/...} without the IDE. The fixture project lives
 * under {@code src/main/resources/JavaComponentIT}.
 */
class JavaComponentIT extends IntegrationTest {

    private static final String PROJECT = "JavaComponentIT";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/demo/DemoController";
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void constructor_and_collection_injection_served_over_http() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // GreetingService injected by constructor; greet("World") -> "Hello, World".
        assertReturns("/greet", "Hello, World");
        // Two @Component Greeter implementations collected into the injected List<Greeter>.
        assertReturns("/count", "2");
        // A @Component JavaHandler is dispatched as the injected container bean (constructor injection).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/java/" + PROJECT + "/demo/HelloHandler")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Hello, Handler")),
                TIMEOUT_SECONDS);
    }

    private void assertReturns(String path, String expectedFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedFragment)),
                TIMEOUT_SECONDS);
    }
}
