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
 * Verifies the engine rejects a client class that mixes the two handler styles. A WebSocket handler
 * that both implements {@code WebsocketHandler} (interface style) and carries {@code @Websocket} +
 * {@code @OnMessage} (annotation style) must NOT be wired, while a clean interface-style handler in
 * the same project IS — proving the rejection is selective, not a whole-project failure. The
 * WebSocket registry is the observable, deterministic signal (it is consulted synchronously after
 * the sync cycle). The same no-mixing guard applies to jobs and listeners. The fixture project
 * lives under {@code src/main/resources/JavaNoMixingIT}.
 */
class JavaNoMixingIT extends IntegrationTest {

    private static final String PROJECT = "JavaNoMixingIT";
    private static final String STATUS = "/services/java/" + PROJECT + "/demo/StatusController";
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
    void mixed_style_handler_is_rejected_while_clean_one_is_registered() {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // Clean interface-style handler registers (positive control — proves wiring works at all).
        assertRegistered("good-no-mixing", true);
        // Handler that mixes WebsocketHandler + @Websocket/@OnMessage is rejected: not registered.
        assertRegistered("mixed-no-mixing", false);
    }

    private void assertRegistered(String endpoint, boolean expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(STATUS + "/" + endpoint)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("\"registered\":" + expected)),
                TIMEOUT_SECONDS);
    }
}
