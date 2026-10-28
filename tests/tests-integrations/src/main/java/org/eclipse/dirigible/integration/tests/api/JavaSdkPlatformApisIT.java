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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The platform APIs that used to be reachable only from JavaScript - scheduler management, granted
 * tenants, the OAuth client and asynchronous HTTP - called from client Java, compiled and run by
 * the platform like any application code.
 *
 * <p>
 * The scheduler is exercised on a client-Java job of the same project, end to end: disable and
 * enable read back through the job's definition, a trigger that must have run the job body by the
 * time it returns, a parameter the job does not declare refused, and a message that must land in
 * the job's execution log. The outbound calls go to a server this test runs in the same JVM.
 */
// One Dirigible boot for the whole class: each method cleans up after itself, so the per-method
// context reset inherited from IntegrationTest would only add ~10s of boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JavaSdkPlatformApisIT extends IntegrationTest {

    private static final String PROJECT = "java-sdk-platform-apis-it";
    private static final String JOB_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/platformapis/SdkManagedJob.java";
    private static final String PARAMETERISED_JOB_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/platformapis/SdkParameterisedJob.job";
    private static final String CONTROLLER_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/platformapis/PlatformApisController.java";
    private static final String ENDPOINT = "/services/java/" + PROJECT + "/platformapis/PlatformApisController";

    /** A client-Java JobHandler's job is named after its class. */
    private static final String JOB = "platformapis.SdkManagedJob";
    /** A .job file's job is named after the file. */
    private static final String PARAMETERISED_JOB = "SdkParameterisedJob";
    private static final String LOGGED_MESSAGE = "logged through the Java SDK";
    private static final String TOKEN_RESPONSE = "{\"access_token\":\"it-token\",\"token_type\":\"Bearer\",\"expires_in\":60}";

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private JobLogService jobLogService;

    private HttpServer server;
    private volatile String tokenRequestBody;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/token", exchange -> {
            tokenRequestBody = new String(exchange.getRequestBody()
                                                  .readAllBytes(),
                    StandardCharsets.UTF_8);
            answer(exchange, "application/json", TOKEN_RESPONSE);
        });
        server.createContext("/ping", exchange -> answer(exchange, "text/plain", "pong"));
        server.start();
    }

    @AfterEach
    void cleanup() {
        server.stop(0);
        boolean cleaned = false;
        for (String path : new String[] {CONTROLLER_PATH, JOB_PATH, PARAMETERISED_JOB_PATH}) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
                cleaned = true;
            }
        }
        if (cleaned) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void theSchedulerManagesAClientJavaJob() {
        publish();

        // One pass: a compile-readiness retry re-runs the whole sequence, and every step is relative to
        // the state the pass found, so a re-run holds as well.
        restAssuredExecutor.execute(() -> assertEquals("disabled,enabled,ran,refused,logged,listed,described", call("/scheduler")),
                ASSERTION_TIMEOUT_SECONDS);

        assertTrue(jobLogService.findByJob(JOB)
                                .stream()
                                .anyMatch(log -> LOGGED_MESSAGE.equals(log.getMessage())),
                "the message must land in the job's execution log");
    }

    @Test
    void tenantsAreReportedUnavailableAndRefusedOutsideTheTokenGroupsResolution() {
        publish();

        restAssuredExecutor.execute(() -> assertEquals("unavailable,refused", call("/tenants")), ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void theOAuthClientPostsTheClientCredentialsAndReturnsTheTokenResponse() {
        publish();

        restAssuredExecutor.execute(() -> assertEquals(TOKEN_RESPONSE, call("/oauth?port=" + port())), ASSERTION_TIMEOUT_SECONDS);

        assertTrue(tokenRequestBody.contains("grant_type=client_credentials"), tokenRequestBody);
        assertTrue(tokenRequestBody.contains("client_id=it+client"), tokenRequestBody);
    }

    @Test
    void theAsyncHttpClientCompletesWithTheResponse() {
        publish();

        restAssuredExecutor.execute(() -> assertEquals("200:pong", call("/async?port=" + port())), ASSERTION_TIMEOUT_SECONDS);
    }

    private void publish() {
        repository.createResource(JOB_PATH, jobSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        repository.createResource(PARAMETERISED_JOB_PATH, parameterisedJobSource().getBytes(StandardCharsets.UTF_8), false,
                "application/json", true);
        repository.createResource(CONTROLLER_PATH, controllerSource().getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private int port() {
        return server.getAddress()
                     .getPort();
    }

    private static String call(String path) {
        return given().when()
                      .get(ENDPOINT + path)
                      .then()
                      .statusCode(200)
                      .extract()
                      .asString();
    }

    private static void answer(HttpExchange exchange, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders()
                .set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String jobSource() {
        return """
                package platformapis;

                import java.util.concurrent.atomic.AtomicInteger;

                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.job.JobHandler;

                @Component
                public class SdkManagedJob implements JobHandler {

                    public static final AtomicInteger RUNS = new AtomicInteger();

                    @Override
                    public String cron() {
                        // Never: the test runs the job itself, and nothing else should.
                        return "0 0 0 1 1 ? 2099";
                    }

                    @Override
                    public void run() {
                        RUNS.incrementAndGet();
                    }
                }
                """;
    }

    /**
     * A job declaring a parameter: its parameter refers back to it, which the scheduler's JSON must not
     * follow (#8145). Never scheduled to fire, so its handler is never looked up.
     */
    private static String parameterisedJobSource() {
        return """
                {
                    "expression": "0 0 0 1 1 ? 2099",
                    "group": "defined",
                    "handler": "%s/platformapis/never-run.ts",
                    "description": "A job declaring a parameter",
                    "parameters": [{"name": "region", "type": "string", "defaultValue": "EU"}]
                }
                """.formatted(PROJECT);
    }

    private static String controllerSource() {
        return """
                package platformapis;

                import java.util.ArrayList;
                import java.util.List;
                import java.util.Map;
                import java.util.concurrent.CompletableFuture;

                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                import org.eclipse.dirigible.sdk.http.HttpAsyncClient;
                import org.eclipse.dirigible.sdk.http.QueryParam;
                import org.eclipse.dirigible.sdk.job.Scheduler;
                import org.eclipse.dirigible.sdk.security.OAuthClient;
                import org.eclipse.dirigible.sdk.security.Tenants;
                import org.eclipse.dirigible.sdk.utils.Converter;

                @Controller
                public class PlatformApisController {

                    private static final String JOB = "%s";
                    private static final String PARAMETERISED_JOB = "%s";

                    @Get("/scheduler")
                    public String scheduler() throws Exception {
                        List<String> steps = new ArrayList<>();

                        Scheduler.disable(JOB);
                        steps.add(enabled() ? "enabled" : "disabled");
                        Scheduler.enable(JOB);
                        steps.add(enabled() ? "enabled" : "disabled");

                        int runs = SdkManagedJob.RUNS.get();
                        Scheduler.trigger(JOB);
                        steps.add(SdkManagedJob.RUNS.get() == runs + 1 ? "ran" : "not-run");

                        try {
                            Scheduler.trigger(JOB, Map.of("undeclared", "value"));
                            steps.add("accepted");
                        } catch (IllegalArgumentException refused) {
                            steps.add("refused");
                        }

                        Scheduler.info(JOB, "%s");
                        steps.add("logged");

                        String jobs = Scheduler.list();
                        steps.add(jobs.contains(JOB) && jobs.contains(PARAMETERISED_JOB) ? "listed" : "unlisted");
                        steps.add(Scheduler.getJob(PARAMETERISED_JOB).contains("region") ? "described" : "undescribed");
                        return String.join(",", steps);
                    }

                    @Get("/tenants")
                    public String tenants() {
                        if (Tenants.isAvailable()) {
                            return "available";
                        }
                        try {
                            Tenants.getGrantedTenants();
                            return "unavailable,listed";
                        } catch (IllegalStateException refused) {
                            return "unavailable,refused";
                        }
                    }

                    @Get("/oauth")
                    public String oauth(@QueryParam("port") String port) throws Exception {
                        return new OAuthClient("http://127.0.0.1:" + port + "/token", "it client", "s3cr+t/=&").getToken();
                    }

                    @Get("/async")
                    public String async(@QueryParam("port") String port) {
                        CompletableFuture<String> response = HttpAsyncClient.get("http://127.0.0.1:" + port + "/ping");
                        Map<?, ?> completed = Converter.fromJson(response.join(), Map.class);
                        return completed.get("statusCode") + ":" + completed.get("text");
                    }

                    private static boolean enabled() {
                        return Boolean.TRUE.equals(Converter.fromJson(Scheduler.getJob(JOB), Map.class).get("enabled"));
                    }
                }
                """.formatted(JOB, PARAMETERISED_JOB, LOGGED_MESSAGE);
    }
}
