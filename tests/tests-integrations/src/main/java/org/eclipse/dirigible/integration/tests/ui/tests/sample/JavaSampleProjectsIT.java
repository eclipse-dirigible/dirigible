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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import io.restassured.http.ContentType;

/**
 * The client-Java samples, published together into one instance and verified one sample per test.
 *
 * <p>
 * Publishing them together also covers something the per-sample runs could not: the five projects
 * are compiled in ONE {@code javac} batch into ONE {@code ClientClassLoader}, so they exercise the
 * bean container with the whole family's {@code @Component}s registered side by side.
 *
 * <p>
 * Counterpart of {@link TypeScriptSampleProjectsIT} - the two families cannot share an instance
 * because their entity samples both own the {@code SAMPLE_COUNTRY} table (see
 * {@link SampleProjectsIT}).
 */
class JavaSampleProjectsIT extends SampleProjectsIT {

    private static final String ENTITY_PROJECT = "sample-java-entity-decorators";
    private static final String COUNTRY_CONTROLLER_BASE = "/services/java/" + ENTITY_PROJECT + "/demo/CountryController";
    private static final String GREETING_BASE = "/services/java/" + ENTITY_PROJECT + "/demo/GreetingController";

    private static final String EXTENSION_PROJECT = "sample-java-extension-decorator";
    private static final String EXTENSION_CONSUMER_BASE = "/services/java/" + EXTENSION_PROJECT + "/demo/extension/ExtensionConsumer";
    private static final String INJECTING_CONSUMER_BASE = "/services/java/" + EXTENSION_PROJECT + "/demo/extension/InjectingConsumer";

    private static final String LISTENER_TRIGGER = "/services/js/sample-java-listener-decorator/demo/listener/trigger.mjs";

    private static final String WEBSOCKET_STATUS_BASE = "/services/java/sample-java-websocket-decorator/demo/websocket/WebsocketStatus";

    @Override
    protected List<String> getRepositoryUrls() {
        return List.of( //
                "https://github.com/dirigiblelabs/sample-java-entity-decorators.git", //
                "https://github.com/dirigiblelabs/sample-java-extension-decorator.git", //
                "https://github.com/dirigiblelabs/sample-java-job-decorator.git", //
                "https://github.com/dirigiblelabs/sample-java-listener-decorator.git", //
                "https://github.com/dirigiblelabs/sample-java-websocket-decorator.git");
    }

    /**
     * The {@code @Entity} / {@code @Repository} / {@code @Controller} annotation stack with
     * CSVIM-seeded country CRUD and OpenAPI registration, plus the Spring-style DI showcase
     * (constructor injection and the {@code Beans} facade).
     */
    @Test
    void entityDecorators() {
        restAssuredExecutor.execute(() -> {
            given().when()
                   .get(COUNTRY_CONTROLLER_BASE)
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"))
                   .body(containsString("Albania"))
                   .body(containsString("Algeria"));

            given().when()
                   .get(COUNTRY_CONTROLLER_BASE + "/1")
                   .then()
                   .statusCode(200)
                   .body(containsString("Afghanistan"));

            given().when()
                   .get("/services/openapi")
                   .then()
                   .statusCode(200)
                   .body(containsString(COUNTRY_CONTROLLER_BASE))
                   .body(containsString(COUNTRY_CONTROLLER_BASE + "/{id}"));

            // DI showcase — constructor injection of the @Component GreetingService.
            given().when()
                   .get(GREETING_BASE + "/greet/World")
                   .then()
                   .statusCode(200)
                   .body(containsString("Hello, World!"));

            // DI showcase — the Beans facade for programmatic lookup.
            given().when()
                   .get(GREETING_BASE + "/greet-via-beans/World")
                   .then()
                   .statusCode(200)
                   .body(containsString("Hello, World!"));
        });
    }

    @Test
    void extensionDecorator() {
        restAssuredExecutor.execute(() -> {
            // Style 1 — Extensions.find(SampleExtensionPoint.class) returns the SampleContribution
            // instances; the consumer maps each via describe(), so the body carries the contribution's
            // own string. Asserting on it also verifies the cast — only a real implementor reaches it.
            given().when()
                   .get(EXTENSION_CONSUMER_BASE + "/contributions")
                   .then()
                   .statusCode(200)
                   .body(containsString("Hello from SampleContribution!"));

            // Style 2 — collection injection: the container populates the controller's
            // List<SampleExtensionPoint> with every @Component contribution, no explicit lookup.
            given().when()
                   .get(INJECTING_CONSUMER_BASE + "/injected-contributions")
                   .then()
                   .statusCode(200)
                   .body(containsString("Hello from SampleContribution!"));
        });
    }

    @Test
    void jobDecorator() {
        LogsAsserter consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);

        // Both jobs fire on a sub-5s cron, so they log again regardless of how long ago the sample
        // was published - the asserter only sees messages logged after it attached.

        // Self-describing interface style — CleanupJob implements JobHandler (schedule from cron()).
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("CleanupJob executed!", Level.INFO));

        // Method-level annotation style — Maintenance's @Scheduled method.
        await().atMost(20, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Maintenance.purgeTempFiles executed", Level.INFO));

        // Client-Java jobs are now real Job definitions on the shared scheduler, so they are VISIBLE
        // and MONITORED in the Jobs perspective (which reads /services/jobs) with engine "java" -
        // not hidden on a private in-JVM scheduler as before.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.engine == 'java' }.size()", greaterThanOrEqualTo(2)));

        // And they SURVIVE a registry synchronization pass - the synchronizer must not reap the
        // runtime-registered rows (they are not backed by a registry artefact).
        synchronizationProcessor.forceProcessSynchronizers();
        SynchronizationUtil.waitForStableSynchronization();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/jobs")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.engine == 'java' }.size()", greaterThanOrEqualTo(2)));

        verifyTriggerNowRunsTheClientJavaJob();
    }

    /**
     * Trigger-now (the Jobs perspective's play button) must run a client-Java job on the Java engine.
     * The manual path used to be JavaScript-only, so it tried to run the job's CLASS NAME as a
     * repository path to a JS module and answered 500 (dirigible #6305).
     *
     * <p>
     * The status is the whole assertion: the Java dispatch either resolves the client bean and invokes
     * it, or throws (an unknown bean, a job body that fails) - and either way the endpoint surfaces
     * that as a 500. It cannot answer 200 without having run the job.
     */
    private void verifyTriggerNowRunsTheClientJavaJob() {
        String name = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                         .get("/services/jobs")
                                                                         .then()
                                                                         .statusCode(200)
                                                                         .extract()
                                                                         .path("find { it.engine == 'java' }.name"));

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("[]")
                                                 .when()
                                                 .post("/services/jobs/trigger/" + name)
                                                 .then()
                                                 .statusCode(200));
    }

    @Test
    void listenerDecorator() {
        LogsAsserter consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LISTENER_TRIGGER)
                                                 .then()
                                                 .statusCode(200));

        // Self-describing interface style — OrderListener implements MessageHandler.
        await().atMost(15, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("OrderListener received:", Level.INFO));

        // Method-level annotation style — InvoiceListener's @Listener method records via the injected
        // Auditor.
        await().atMost(15, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Auditor: invoice received:", Level.INFO));
    }

    @Test
    void websocketDecorator() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WEBSOCKET_STATUS_BASE + "/status")
                                                 .then()
                                                 .statusCode(200)
                                                 // Self-describing interface style — ChatHandler implements WebsocketHandler.
                                                 .body(containsString("\"chat\":true"))
                                                 // Method-level annotation style — TickerHandler is @Websocket + @OnX.
                                                 .body(containsString("\"ticker\":true")));
    }

}
