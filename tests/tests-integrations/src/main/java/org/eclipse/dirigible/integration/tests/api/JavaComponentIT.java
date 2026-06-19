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
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end test for the client-Java bean container: a {@code @Controller} that receives a
 * {@code @Component} service and a {@code List<Greeter>} (collection injection) through its
 * constructor, served over {@code /services/java/...} without the IDE.
 */
class JavaComponentIT extends IntegrationTest {

    private static final String PROJECT = "java-component-it";
    private static final String BASE = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/demo/DemoController";
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void constructor_and_collection_injection_served_over_http() {
        writeAllAndSync();

        // GreetingService injected by constructor; greet("World") -> "Hello, World".
        assertReturns("/greet", "Hello, World");
        // Two @Component Greeter implementations collected into the injected List<Greeter>.
        assertReturns("/count", "2");
    }

    private void writeAllAndSync() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("Greeter.java", """
                package demo;
                public interface Greeter {
                    String name();
                }
                """);
        sources.put("EnglishGreeter.java", """
                package demo;
                import org.eclipse.dirigible.sdk.component.Component;
                @Component
                public class EnglishGreeter implements Greeter {
                    public String name() { return "en"; }
                }
                """);
        sources.put("GermanGreeter.java", """
                package demo;
                import org.eclipse.dirigible.sdk.component.Component;
                @Component
                public class GermanGreeter implements Greeter {
                    public String name() { return "de"; }
                }
                """);
        sources.put("GreetingService.java", """
                package demo;
                import org.eclipse.dirigible.sdk.component.Component;
                @Component
                public class GreetingService {
                    public String greet(String who) { return "Hello, " + who; }
                }
                """);
        sources.put("DemoController.java", """
                package demo;
                import java.util.List;
                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                @Controller
                public class DemoController {
                    private final GreetingService greetings;
                    private final List<Greeter> greeters;
                    public DemoController(GreetingService greetings, List<Greeter> greeters) {
                        this.greetings = greetings;
                        this.greeters = greeters;
                    }
                    @Get("/greet")
                    public String greet() { return greetings.greet("World"); }
                    @Get("/count")
                    public int count() { return greeters.size(); }
                }
                """);
        sources.forEach((name, source) -> repository.createResource(BASE + name, source.getBytes(StandardCharsets.UTF_8), false,
                "text/x-java", true));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void assertReturns(String path, String expectedFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedFragment)),
                TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT)) {
            repository.removeCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }
}
