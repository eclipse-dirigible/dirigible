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

import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.process.NativeAppProcessManager;
import org.eclipse.dirigible.components.engine.nativeapps.service.NativeAppService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Exercises the full LOCAL native-app code path end-to-end: process spawn, port resolution, lazy
 * start, ALWAYS-mode boot, basic-auth injection, and clean teardown.
 *
 * <p>
 * The spawned process is a tiny Java HTTP server (single-file source executed via {@code java
 * Server.java}, available since Java 11). Java is the platform's hard runtime requirement, so this
 * needs no extra prerequisite on the CI runner.
 */
class LocalNativeAppLifecycleIT extends IntegrationTest {

    private static final long ASSERT_TIMEOUT_SECONDS = 30;

    /**
     * Self-contained HTTP server: reads {@code DIRIGIBLE_NATIVE_APP_PORT} from env, replies with a JSON
     * body that echoes the request {@code path} and the inbound {@code Authorization} header so tests
     * can assert basic-auth injection. Written as a Java source file the test publishes alongside the
     * {@code .native-app} fixture; the lifecycle command runs it via {@code java
     * Server.java}.
     */
    private static final String JAVA_SERVER_SOURCE = """
            import com.sun.net.httpserver.HttpServer;
            import java.net.InetAddress;
            import java.net.InetSocketAddress;
            public class Server {
                public static void main(String[] args) throws Exception {
                    int port = Integer.parseInt(System.getenv().getOrDefault("DIRIGIBLE_NATIVE_APP_PORT", "0"));
                    HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
                    s.createContext("/", ex -> {
                        String auth = ex.getRequestHeaders().getFirst("Authorization");
                        if (auth == null) auth = "";
                        String body = "{\\"path\\":\\"" + ex.getRequestURI().getPath()
                                + "\\",\\"auth\\":\\"" + auth + "\\"}";
                        byte[] b = body.getBytes("UTF-8");
                        ex.getResponseHeaders().set("Content-Type", "application/json");
                        ex.sendResponseHeaders(200, b.length);
                        ex.getResponseBody().write(b);
                        ex.close();
                    });
                    System.err.println("READY");
                    s.start();
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private NativeAppProcessManager processManager;

    @Autowired
    private NativeAppService service;

    @AfterEach
    void cleanupArtefact() {
        processManager.stopAll();
        for (String name : new String[] {"local-lazy", "local-always", "local-auth", "local-stop-port"}) {
            removeProjectFiles(name);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void removeProjectFiles(String name) {
        for (String relative : new String[] {"/" + name + "/" + name + ".native-app", "/" + name + "/Server.java"}) {
            String path = IRepositoryStructure.PATH_REGISTRY_PUBLIC + relative;
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
    }

    @Test
    void lazy_start_starts_process_on_first_request() {
        writeFixture("local-lazy", lazyFixture("local-lazy"));

        NativeApp app = service.findByName("local-lazy");
        if (processManager.isAlive(app)) {
            throw new AssertionError("Lazy-start native app should not be alive before first request.");
        }

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-lazy/")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        if (!processManager.isAlive(service.findByName("local-lazy"))) {
            throw new AssertionError("Lazy-start native app should be alive after the first proxy request.");
        }
    }

    @Test
    void always_mode_starts_at_publish_time() {
        writeFixture("local-always", alwaysFixture("local-always"));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-always/anything")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        if (!processManager.isAlive(service.findByName("local-always"))) {
            throw new AssertionError("ALWAYS-mode native app should be alive.");
        }
    }

    /**
     * Regression for the "unpublish kills the JVM" bug: the stop subprocess must receive
     * {@code DIRIGIBLE_NATIVE_APP_PORT} on its environment, otherwise an author's stop script that
     * reads the env var with a fallback (e.g. {@code ${PORT:-8080}}) silently resolves to the
     * platform's own port and signals the Dirigible JVM.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void stop_command_subprocess_receives_DIRIGIBLE_NATIVE_APP_PORT() throws Exception {
        Path marker = Files.createTempFile("native-stop-port-", ".txt");
        marker.toFile()
              .deleteOnExit();

        writeFixture("local-stop-port", stopMarkerFixture("local-stop-port", marker.toAbsolutePath()
                                                                                   .toString()));

        // Lazy mode — first proxy request spawns the process.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-stop-port/")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        NativeApp app = service.findByName("local-stop-port");
        int resolvedPort = processManager.getState(app)
                                         .orElseThrow(() -> new AssertionError("RuntimeState missing for [local-stop-port]."))
                                         .getPort();

        // Synchronizer DELETE → processManager.stop(app) → runs lifecycle.stop with the resolved port
        // exported on the subprocess env. The stop script captures the env var to the marker file.
        removeProjectFiles("local-stop-port");
        synchronizationProcessor.forceProcessSynchronizers();

        String captured = Files.readString(marker)
                               .trim();
        if (!Integer.toString(resolvedPort)
                    .equals(captured)) {
            throw new AssertionError("Expected stop subprocess to see DIRIGIBLE_NATIVE_APP_PORT=[" + resolvedPort
                    + "] but marker file contained [" + captured + "].");
        }
    }

    @Test
    void basic_auth_header_is_injected_for_local_app() {
        writeFixture("local-auth", localAuthFixture());

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-auth/")
                                                 .then()
                                                 .statusCode(200)
                                                 // body echoes the Authorization header observed by the upstream
                                                 .body(containsString("Basic YWxpY2U6d29uZGVybGFuZA==")),
                ASSERT_TIMEOUT_SECONDS);
    }

    // ----- Fixtures ---------------------------------------------------------

    private void writeFixture(String name, String nativeAppJson) {
        String javaPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + name + "/Server.java";
        repository.createResource(javaPath, JAVA_SERVER_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        String nativeAppPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + name + "/" + name + ".native-app";
        repository.createResource(nativeAppPath, nativeAppJson.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private String lazyFixture(String name) {
        return baseLocalFixture(name, "lazy", null, null);
    }

    private String alwaysFixture(String name) {
        return baseLocalFixture(name, "always", null, null);
    }

    private String localAuthFixture() {
        return baseLocalFixture("local-auth", "lazy", "alice", "wonderland");
    }

    /**
     * Builds a fixture whose {@code lifecycle.stop} subprocess captures
     * {@code $DIRIGIBLE_NATIVE_APP_PORT} into the given marker file. Used to assert end-to-end that the
     * resolved port reaches the stop subprocess via the env var. mac/linux only — the resolver falls
     * back to {@code Process.destroy()} on Windows since no {@code os: windows} stop entry is provided.
     */
    private String stopMarkerFixture(String name, String markerAbsolutePath) {
        // %%s survives String.formatted() and reaches sh as the literal printf format string %s.
        return """
                {
                  "name": "%s",
                  "description": "stop-command env-var marker",
                  "basePath": "%s",
                  "type": "local",
                  "config": {
                    "lifecycle": {
                      "start": {
                        "mode": "lazy",
                        "commands": [
                          { "os": "mac",   "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "linux", "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] }
                        ]
                      },
                      "stop": {
                        "commands": [
                          { "os": "mac",   "executable": "sh", "dir": "%s",
                            "arguments": [ { "name": "-c", "value": "printf %%s \\"$DIRIGIBLE_NATIVE_APP_PORT\\" > %s" } ] },
                          { "os": "linux", "executable": "sh", "dir": "%s",
                            "arguments": [ { "name": "-c", "value": "printf %%s \\"$DIRIGIBLE_NATIVE_APP_PORT\\" > %s" } ] }
                        ]
                      }
                    },
                    "security": null
                  }
                }
                """.formatted(name, name, name, name, name, markerAbsolutePath, name, markerAbsolutePath);
    }

    private String baseLocalFixture(String name, String mode, String user, String pass) {
        String security;
        if (user != null && pass != null) {
            security = """
                    {
                      "authentication": {
                        "type": "basic",
                        "credentials": {
                          "user": "%s",
                          "password": "%s"
                        }
                      },
                      "exposedPaths": null
                    }
                    """.formatted(user, pass);
        } else {
            security = "null";
        }
        return """
                {
                  "name": "%s",
                  "description": "local java http server",
                  "basePath": "%s",
                  "type": "local",
                  "config": {
                    "lifecycle": {
                      "start": {
                        "mode": "%s",
                        "commands": [
                          { "os": "mac",     "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "linux",   "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "windows", "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] }
                        ]
                      },
                      "stop": { "commands": [] }
                    },
                    "security": %s
                  }
                }
                """.formatted(name, name, mode, name, name, name, security);
    }
}
