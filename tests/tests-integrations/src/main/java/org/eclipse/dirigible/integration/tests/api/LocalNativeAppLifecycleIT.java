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
import org.eclipse.dirigible.components.engine.nativeapps.process.RuntimeState;
import org.eclipse.dirigible.components.engine.nativeapps.service.NativeAppService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
     * {@code .nativeapp} fixture; the lifecycle command runs it via {@code java
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

    private void removeProjectFiles(String name) {
        for (String relative : new String[] {"/" + name + "/" + name + ".nativeapp", "/" + name + "/Server.java"}) {
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
     * Locks in the contract that {@code command.dir}, when set, is resolved as a project-root-relative
     * subpath — so applications whose source lives in a subdirectory (e.g. {@code apps/server/}) are
     * supported without authors having to embed their own project folder name into the artefact. The
     * fixture publishes {@code Server.java} into {@code <project>/apps/server/} and sets
     * {@code command.dir: "apps/server"}; the proxy can only return 200 if the spawn happened in that
     * subdirectory and the Java single-source compiler picked the file up there.
     */
    @Test
    void start_command_runs_in_command_dir_subpath() {
        writeFixtureWithServerInSubdir("local-subdir", "apps/server", subdirLazyFixture("local-subdir", "apps/server"));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-subdir/")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        if (!processManager.isAlive(service.findByName("local-subdir"))) {
            throw new AssertionError("Native app with command.dir subpath should be alive after first proxy request.");
        }
    }

    /**
     * Regression for the "unpublish kills the JVM" bug: the stop subprocess must receive both
     * {@code DIRIGIBLE_NATIVE_APP_PORT} and {@code DIRIGIBLE_NATIVE_APP_PID} on its environment.
     * Without the port, an author's stop script that reads it with a POSIX fallback (e.g.
     * {@code ${PORT:-8080}}) silently resolves to the platform's own port and signals the Dirigible
     * JVM. Without the PID, a stop script has no safe target — killing "every PID on the port" via
     * {@code lsof | xargs kill} also catches the JVM's outbound HttpClient keep-alive entry and brings
     * the platform down.
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
        RuntimeState beforeStop = processManager.getState(app)
                                                .orElseThrow(() -> new AssertionError("RuntimeState missing for [local-stop-port]."));
        int resolvedPort = beforeStop.getPort();
        long resolvedPid = beforeStop.getProcess()
                                     .pid();

        // Synchronizer DELETE → processManager.stop(app) → runs lifecycle.stop with the resolved port
        // and held PID exported on the subprocess env. The stop script captures both into the marker
        // file, one per line.
        removeProjectFiles("local-stop-port");
        synchronizationProcessor.forceProcessSynchronizers();

        String captured = Files.readString(marker)
                               .trim();
        String expected = resolvedPort + "\n" + resolvedPid;
        if (!expected.equals(captured)) {
            throw new AssertionError("Expected stop subprocess to see DIRIGIBLE_NATIVE_APP_PORT=[" + resolvedPort
                    + "] and DIRIGIBLE_NATIVE_APP_PID=[" + resolvedPid + "] (marker contents '" + expected.replace("\n", "\\n")
                    + "') but marker file contained [" + captured.replace("\n", "\\n") + "].");
        }
    }

    /**
     * Regression for the orphan-grandchild case: when the held root is a shell that spawns the real
     * listener as a non-{@code exec} child, {@link Process#destroy()} kills only the shell and the
     * child is reparented to init/launchd. {@code stop()} must walk {@link ProcessHandle#descendants()}
     * and SIGKILL anything still alive afterwards.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void stop_kills_descendant_processes() throws Exception {
        writeFixture("local-orphan", shellWrappedFixture("local-orphan"));

        // Lazy mode — first proxy request spawns the sh → java tree.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-orphan/")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        NativeApp app = service.findByName("local-orphan");
        RuntimeState state = processManager.getState(app)
                                           .orElseThrow(() -> new AssertionError("RuntimeState missing for [local-orphan]."));
        Process held = state.getProcess();

        // Snapshot the descendant tree while still alive — sh wrapper must have at least one
        // java descendant. If we don't see one, the fixture's shell exec'd away (defeating the
        // test premise) or java hasn't been spawned yet.
        List<ProcessHandle> descendants = held.toHandle()
                                              .descendants()
                                              .toList();
        if (descendants.isEmpty()) {
            throw new AssertionError("Expected at least one descendant under the held shell PID [" + held.pid() + "].");
        }

        processManager.stop(app);

        // Tiny grace for SIGKILL delivery, then assert every previously-snapshotted descendant is
        // gone. Without the descendants() walk in stop(), the java grandchild would survive sh's
        // SIGTERM and still be alive here.
        Thread.sleep(500);
        for (ProcessHandle d : descendants) {
            if (d.isAlive()) {
                throw new AssertionError("Descendant PID [" + d.pid() + "] (command [" + d.info()
                                                                                          .command()
                                                                                          .orElse("<unknown>")
                        + "]) should have been killed by stop() — orphan-grandchild leak.");
            }
        }
    }

    /**
     * Regression for the case where the author's {@code lifecycle.stop} script kills
     * {@code $DIRIGIBLE_NATIVE_APP_PID} directly (the safe pattern this platform encourages). With the
     * held root SIGTERMed by the script, the OS reparents any surviving children to init/launchd
     * <em>before</em> the platform's own {@code Process.destroy()} runs — at which point
     * {@code descendants()} on the dead held PID returns empty, and a naive "snapshot after
     * runStopCommandsBestEffort" would miss the leftover walk and orphan the listener.
     *
     * <p>
     * Live-confirmed footgun: the sample-library-local-native-app sample's
     * {@code npm stop → kill "$DIRIGIBLE_NATIVE_APP_PID"} would leave the actual Node listener running
     * and bound to its port. {@code NativeAppProcessManager.stop} therefore snapshots descendants
     * BEFORE invoking the author script.
     */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void stop_cleans_descendants_when_author_stop_kills_held_pid() throws Exception {
        writeFixture("local-orphan-pidstop", shellWrappedFixtureWithPidStop("local-orphan-pidstop"));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/native-apps-proxy/v1/local-orphan-pidstop/")
                                                 .then()
                                                 .statusCode(200),
                ASSERT_TIMEOUT_SECONDS);

        NativeApp app = service.findByName("local-orphan-pidstop");
        RuntimeState state = processManager.getState(app)
                                           .orElseThrow(() -> new AssertionError("RuntimeState missing for [local-orphan-pidstop]."));
        Process held = state.getProcess();
        List<ProcessHandle> descendants = held.toHandle()
                                              .descendants()
                                              .toList();
        if (descendants.isEmpty()) {
            throw new AssertionError("Expected at least one descendant under the held shell PID [" + held.pid() + "].");
        }

        processManager.stop(app);

        Thread.sleep(500);
        for (ProcessHandle d : descendants) {
            if (d.isAlive()) {
                throw new AssertionError("Descendant PID [" + d.pid() + "] (command [" + d.info()
                                                                                          .command()
                                                                                          .orElse("<unknown>")
                        + "]) survived stop() — author's stop killed the held root and the descendants walk missed the leftovers.");
            }
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
        String nativeAppPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + name + "/" + name + ".nativeapp";
        repository.createResource(nativeAppPath, nativeAppJson.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /**
     * Variant of {@link #writeFixture} that places {@code Server.java} into a project subdirectory
     * (e.g. {@code apps/server/}) so the matching {@code command.dir} value can exercise the
     * non-default branch of {@code NativeAppProcessManager.resolveWorkingDir}. The {@code .nativeapp}
     * itself stays at the project root so the synchronizer's location handling is identical to every
     * other fixture.
     */
    private void writeFixtureWithServerInSubdir(String name, String dirSubpath, String nativeAppJson) {
        String javaPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + name + "/" + dirSubpath + "/Server.java";
        repository.createResource(javaPath, JAVA_SERVER_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        String nativeAppPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + name + "/" + name + ".nativeapp";
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
     * Lazy-mode fixture that sets {@code command.dir} on every OS entry to a project-relative subpath
     * (typically the directory where {@code Server.java} lives). Exercises the platform's
     * project-root-relative resolution: the resolver joins {@code dir} under the artefact's project
     * root rather than the registry root.
     */
    private String subdirLazyFixture(String name, String dirSubpath) {
        return """
                {
                  "name": "%s",
                  "description": "local java http server in a project subdirectory",
                  "basePath": "%s",
                  "type": "local",
                  "config": {
                    "lifecycle": {
                      "start": {
                        "mode": "lazy",
                        "commands": [
                          { "os": "mac",     "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "linux",   "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "windows", "executable": "java", "dir": "%s", "arguments": [ { "name": "Server.java", "value": "" } ] }
                        ]
                      },
                      "stop": { "commands": [] }
                    },
                    "security": null
                  }
                }
                """.formatted(name, name, dirSubpath, dirSubpath, dirSubpath);
    }

    /**
     * Fixture whose start command spawns {@code java Server.java} as a NON-{@code exec} child of
     * {@code sh}. The {@code ; true} tail prevents bash's tail-call optimization (otherwise sh would
     * {@code exec} into java and the held PID would be java itself with no descendants). This produces
     * the shell → java orphan-prone chain we want to exercise. mac/linux only.
     */
    private String shellWrappedFixture(String name) {
        return """
                {
                  "name": "%s",
                  "description": "shell-wrapped java server (orphan-grandchild regression)",
                  "basePath": "%s",
                  "type": "local",
                  "config": {
                    "lifecycle": {
                      "start": {
                        "mode": "lazy",
                        "commands": [
                          { "os": "mac",   "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "java Server.java; true" } ] },
                          { "os": "linux", "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "java Server.java; true" } ] }
                        ]
                      },
                      "stop": { "commands": [] }
                    },
                    "security": null
                  }
                }
                """.formatted(name, name);
    }

    /**
     * Like {@link #shellWrappedFixture} but declares a {@code lifecycle.stop} that targets
     * {@code $DIRIGIBLE_NATIVE_APP_PID} — the safe pattern. Used by
     * {@code stop_cleans_descendants_when_author_stop_kills_held_pid} to confirm the engine snapshots
     * descendants BEFORE invoking the author script.
     */
    private String shellWrappedFixtureWithPidStop(String name) {
        return """
                {
                  "name": "%s",
                  "description": "shell-wrapped java server with PID-targeted stop (regression)",
                  "basePath": "%s",
                  "type": "local",
                  "config": {
                    "lifecycle": {
                      "start": {
                        "mode": "lazy",
                        "commands": [
                          { "os": "mac",   "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "java Server.java; true" } ] },
                          { "os": "linux", "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "java Server.java; true" } ] }
                        ]
                      },
                      "stop": {
                        "commands": [
                          { "os": "mac",   "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "kill \\"$DIRIGIBLE_NATIVE_APP_PID\\" 2>/dev/null || true" } ] },
                          { "os": "linux", "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "kill \\"$DIRIGIBLE_NATIVE_APP_PID\\" 2>/dev/null || true" } ] }
                        ]
                      }
                    },
                    "security": null
                  }
                }
                """.formatted(name, name);
    }

    /**
     * Builds a fixture whose {@code lifecycle.stop} subprocess captures both
     * {@code $DIRIGIBLE_NATIVE_APP_PORT} and {@code $DIRIGIBLE_NATIVE_APP_PID} into the given marker
     * file (one per line, in order). Used to assert end-to-end that the resolved port AND held PID
     * reach the stop subprocess via the contract env vars — both are needed so authors can write safe
     * stop scripts (target the held PID, not "every PID on this port", which would also kill the
     * Dirigible JVM via its outbound keep-alive connection). mac/linux only — the resolver falls back
     * to {@code Process.destroy()} on Windows since no {@code os: windows} stop entry is provided.
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
                          { "os": "mac",   "executable": "java", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "linux", "executable": "java", "arguments": [ { "name": "Server.java", "value": "" } ] }
                        ]
                      },
                      "stop": {
                        "commands": [
                          { "os": "mac",   "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "printf '%%s\\\\n%%s' \\"$DIRIGIBLE_NATIVE_APP_PORT\\" \\"$DIRIGIBLE_NATIVE_APP_PID\\" > %s" } ] },
                          { "os": "linux", "executable": "sh",
                            "arguments": [ { "name": "-c", "value": "printf '%%s\\\\n%%s' \\"$DIRIGIBLE_NATIVE_APP_PORT\\" \\"$DIRIGIBLE_NATIVE_APP_PID\\" > %s" } ] }
                        ]
                      }
                    },
                    "security": null
                  }
                }
                """.formatted(
                name, name, markerAbsolutePath, markerAbsolutePath);
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
                          { "os": "mac",     "executable": "java", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "linux",   "executable": "java", "arguments": [ { "name": "Server.java", "value": "" } ] },
                          { "os": "windows", "executable": "java", "arguments": [ { "name": "Server.java", "value": "" } ] }
                        ]
                      },
                      "stop": { "commands": [] }
                    },
                    "security": %s
                  }
                }
                """.formatted(name, name, mode, security);
    }
}
