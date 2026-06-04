/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;

import ch.qos.logback.classic.Level;

/**
 * Clones {@code dirigiblelabs/sample-java-entity-decorators}, publishes it through the IDE, and
 * verifies the full {@code engine-java} annotation surface end-to-end:
 *
 * <ul>
 * <li>{@code @Entity} / {@code @Repository} / {@code @Controller} — CSVIM-seeded country CRUD</li>
 * <li>{@code @Extension} — contribution registered in the Dirigible extension registry</li>
 * <li>{@code @Scheduled} — Quartz job fires within 10 s and logs a confirmation line</li>
 * <li>{@code @Listener} — ActiveMQ queue listener receives a message sent by a JS trigger</li>
 * <li>{@code @Websocket} — handler registered in {@code JavaWebsocketRegistry}</li>
 * </ul>
 */
public class JavaEntityDecoratorsSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-entity-decorators";

    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/demo/CountryController";
    private static final String EXTENSION_CONSUMER_BASE = "/services/java/" + PROJECT + "/demo/extension/ExtensionConsumer";
    private static final String WEBSOCKET_STATUS_BASE = "/services/java/" + PROJECT + "/demo/websocket/WebsocketStatus";
    private static final String LISTENER_TRIGGER = "/services/js/" + PROJECT + "/demo/listener/trigger.mjs";

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUpLogAsserter() {
        consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-entity-decorators.git";
    }

    @Override
    protected void verifyProject() {
        verifyEntityDecoratorStack();
        verifyExtensionAnnotation();
        verifyScheduledAnnotation();
        verifyListenerAnnotation();
        verifyWebsocketAnnotation();
    }

    private void verifyEntityDecoratorStack() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(CONTROLLER_BASE)
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"))
                   .body(containsString("Albania"))
                   .body(containsString("Algeria"));

            given().when()
                   .get(CONTROLLER_BASE + "/1")
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"));

            given().when()
                   .get("/services/openapi")
                   .then()
                   .statusCode(200)
                   .body(containsString(CONTROLLER_BASE))
                   .body(containsString(CONTROLLER_BASE + "/{id}"));
        });
    }

    private void verifyExtensionAnnotation() {
        // ExtensionClassConsumer must have stored a record with module =
        // "demo.extension.SampleContribution".
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EXTENSION_CONSUMER_BASE + "/contributions")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("demo.extension.SampleContribution")));
    }

    private void verifyScheduledAnnotation() {
        // CleanupJob fires every second; wait up to 10 s for the first execution log line.
        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("CleanupJob executed!", Level.INFO));
    }

    private void verifyListenerAnnotation() {
        // Trigger sends a message to "java-order-queue"; OrderListener logs the receipt.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LISTENER_TRIGGER)
                                                 .then()
                                                 .statusCode(200));

        await().atMost(10, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("OrderListener received:", Level.INFO));
    }

    private void verifyWebsocketAnnotation() {
        // WebsocketStatus controller queries JavaWebsocketRegistry for the "java-chat" endpoint.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WEBSOCKET_STATUS_BASE + "/status")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("true")));
    }

}
