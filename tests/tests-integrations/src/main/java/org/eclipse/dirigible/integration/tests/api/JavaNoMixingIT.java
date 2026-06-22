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
 * Verifies the engine rejects a client class that mixes the two handler styles. A WebSocket handler
 * that both implements {@code WebsocketHandler} (interface style) and carries {@code @Websocket} +
 * {@code @OnMessage} (annotation style) must NOT be wired, while a clean interface-style handler in
 * the same project IS — proving the rejection is selective, not a whole-project failure. The
 * WebSocket registry is the observable, deterministic signal (it is consulted synchronously after
 * the sync cycle). The same no-mixing guard applies to jobs and listeners.
 */
class JavaNoMixingIT extends IntegrationTest {

    private static final String PROJECT = "java-no-mixing-it";
    private static final String BASE = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/";
    private static final String STATUS = "/services/java/" + PROJECT + "/demo/StatusController";
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void mixed_style_handler_is_rejected_while_clean_one_is_registered() {
        writeAllAndSync();

        // Clean interface-style handler registers (positive control — proves wiring works at all).
        assertRegistered("good-no-mixing", true);
        // Handler that mixes WebsocketHandler + @Websocket/@OnMessage is rejected: not registered.
        assertRegistered("mixed-no-mixing", false);
    }

    private void writeAllAndSync() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("GoodSocket.java", """
                package demo;
                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.net.WebsocketHandler;
                @Component
                public class GoodSocket implements WebsocketHandler {
                    public String endpoint() { return "good-no-mixing"; }
                    public void onMessage(String message, String from) { }
                }
                """);
        sources.put("MixedSocket.java", """
                package demo;
                import org.eclipse.dirigible.sdk.net.OnMessage;
                import org.eclipse.dirigible.sdk.net.Websocket;
                import org.eclipse.dirigible.sdk.net.WebsocketHandler;
                @Websocket(name = "Mixed", endpoint = "mixed-no-mixing")
                public class MixedSocket implements WebsocketHandler {
                    public String endpoint() { return "mixed-no-mixing"; }
                    @OnMessage public void onMessage(String message, String from) { }
                }
                """);
        sources.put("StatusController.java", """
                package demo;
                import java.util.Map;
                import org.eclipse.dirigible.components.base.spring.BeanProvider;
                import org.eclipse.dirigible.engine.java.websocket.JavaWebsocketRegistry;
                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.http.PathParam;
                @Controller
                public class StatusController {
                    @Get("/{endpoint}")
                    public Map<String, Object> status(@PathParam("endpoint") String endpoint) {
                        JavaWebsocketRegistry registry = BeanProvider.getBean(JavaWebsocketRegistry.class);
                        return Map.of("registered", registry.contains(endpoint));
                    }
                }
                """);
        sources.forEach((name, source) -> repository.createResource(BASE + name, source.getBytes(StandardCharsets.UTF_8), false,
                "text/x-java", true));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void assertRegistered(String endpoint, boolean expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(STATUS + "/" + endpoint)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"registered\":" + expected)),
                TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        String project = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT;
        if (repository.hasCollection(project)) {
            repository.removeCollection(project);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }
}
