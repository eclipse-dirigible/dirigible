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

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.MavenFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * The dependency trust chain, end to end over one running platform: a clean resolution writes the
 * lockfile, a tampered local-repository jar fails its checksum verification while everything else
 * keeps serving, frozen mode rejects a coordinate the lock does not carry without consulting any
 * repository, and a platform-provided coordinate declared at a different version is reported as
 * shadowed - with the platform's own class provably serving. All repositories are file: fixtures
 * built by the test - no network.
 */
// One Dirigible boot for the whole journey; the steps build on each other in @Order sequence.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DependencyLockfileIT extends IntegrationTest {

    /** The registry project declaring the maven dependencies. */
    private static final String PROJECT = "lockfile-it";
    private static final String PROJECT_JSON_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/project.json";
    private static final String PROBE_SOURCE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/client/GsonProbe.java";

    /** The AOT survivor module's controller - proves the rest keeps serving after a failure. */
    private static final String SURVIVOR_ENDPOINT = "/services/java/survivor-module/survivor/SurvivorController/ping";

    /** The registry handler proving the platform's gson serves, not a declared one. */
    private static final String PROBE_ENDPOINT = "/services/java/" + PROJECT + "/client/GsonProbe";

    private static final String ALPHA = "com.example:alpha:1.0.0";

    private static final long AWAIT_SECONDS = 60;

    @TempDir
    static Path tempDir;

    private static Path fixtureRepo;
    private static Path localRepository;
    private static Path lockfilePath;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeAll
    static void prepareFixtures() throws IOException {
        fixtureRepo = Files.createDirectories(tempDir.resolve("fixture-repo"));
        localRepository = tempDir.resolve("local-repo");
        Path resolvedModules = tempDir.resolve("resolved-modules");
        lockfilePath = resolvedModules.resolve("project-lock.json");
        // the fixture repository REPLACES Maven Central, so no request can leave the machine
        Configuration.set("DIRIGIBLE_MAVEN_REPOSITORIES", "central=" + fixtureRepo.toUri());
        Configuration.set("DIRIGIBLE_MAVEN_LOCAL_REPO", localRepository.toString());
        Configuration.set("DIRIGIBLE_DEPENDENCIES_DIR", resolvedModules.toString());
        Configuration.set("DIRIGIBLE_DEPENDENCIES_FROZEN", "false");

        Path work = Files.createDirectories(tempDir.resolve("work"));
        MavenFixtures.deploy(fixtureRepo, "com.example", "alpha", "1.0.0",
                MavenFixtures.buildPlainJar(work, "alpha-1.0.0.jar", Map.of("com.example.alpha.Alpha", """
                        package com.example.alpha;
                        public class Alpha {
                        }
                        """)));
        MavenFixtures.deploy(fixtureRepo, "com.example", "survivor-module", "1.0.0",
                MavenFixtures.buildModuleJar(work, "survivor-module-1.0.0.jar", "survivor-module", Map.of("survivor.SurvivorController", """
                        package survivor;
                        import org.eclipse.dirigible.sdk.http.Controller;
                        import org.eclipse.dirigible.sdk.http.Get;
                        @Controller
                        public class SurvivorController {
                            @Get("/ping")
                            public String ping() {
                                return "survivor alive";
                            }
                        }
                        """), Map.of()));
        MavenFixtures.deploy(fixtureRepo, "com.example", "gamma", "1.0.0",
                MavenFixtures.buildPlainJar(work, "gamma-1.0.0.jar", Map.of("com.example.gamma.Gamma", """
                        package com.example.gamma;
                        public class Gamma {
                        }
                        """)));
        // a fake gson the platform-provided one must shadow - the marker resource distinguishes it
        MavenFixtures.deploy(fixtureRepo, "com.google.code.gson", "gson", "1.0.0-fixture",
                MavenFixtures.buildPlainJar(work, "gson-1.0.0-fixture.jar", Map.of("com.google.gson.FixtureMarker", """
                        package com.google.gson;
                        public class FixtureMarker {
                        }
                        """), Map.of("gson-fixture.marker", "the fixture gson, not the platform's")));
    }

    @Test
    @Order(1)
    void a_clean_resolution_writes_the_lockfile() throws IOException {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:alpha:1.0.0" },
                { "type": "maven", "id": "com.example:survivor-module:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", anEmptyMap())
                                             .body("frozen", is(false))
                                             .body("lockfile", containsString("project-lock.json"))
                                             .body("declaredBy.'com.example:alpha:1.0.0'", hasItem(PROJECT))
                                             .body("report.findAll { it.coordinate == '" + ALPHA + "' }.status", hasItem("active")));

        awaitEndpoint(SURVIVOR_ENDPOINT, "survivor alive");

        assertThat(lockfilePath).exists();
        String lock = Files.readString(lockfilePath, StandardCharsets.UTF_8);
        assertThat(lock).contains("\"id\": \"com.example:alpha:1.0.0\"")
                        .contains("\"id\": \"com.example:survivor-module:1.0.0\"")
                        .contains("\"" + PROJECT + "\"")
                        .contains(sha256(localRepository.resolve("com/example/alpha/1.0.0/alpha-1.0.0.jar")));

        // the second pass verifies every artifact against the just-written lock and stays clean
        resolveExpecting(response -> response.body("failures", anEmptyMap())
                                             .body("report.findAll { it.coordinate == '" + ALPHA + "' }.status", hasItem("active")));
    }

    @Test
    @Order(2)
    void a_tampered_artifact_fails_verification_and_the_rest_keeps_serving() throws IOException {
        Path alphaJar = localRepository.resolve("com/example/alpha/1.0.0/alpha-1.0.0.jar");
        String lockedSha = sha256(alphaJar);
        byte[] original = Files.readAllBytes(alphaJar);
        Files.write(alphaJar, "tampered".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

        try {
            resolveExpecting(response -> response.body("failures", hasKey(ALPHA))
                                                 .body("failures.'" + ALPHA + "'", containsString("Checksum mismatch"))
                                                 .body("report.findAll { it.coordinate == '" + ALPHA + "' }.status", hasItem("failed"))
                                                 .body("report.findAll { it.coordinate == 'com.example:survivor-module:1.0.0' }.status",
                                                         hasItem("active"))
                                                 .body("artifacts", not(hasItem(alphaJar.toString()))));

            // the verified rest keeps serving - an integrity failure is per-artifact, never total
            awaitEndpoint(SURVIVOR_ENDPOINT, "survivor alive");

            // the failed pass must never launder the tampered artifact into the trusted set
            assertThat(Files.readString(lockfilePath, StandardCharsets.UTF_8)).contains(lockedSha);
        } finally {
            Files.write(alphaJar, original);
        }

        // the restored artifact matches the lock again and re-activates
        resolveExpecting(response -> response.body("failures", anEmptyMap())
                                             .body("report.findAll { it.coordinate == '" + ALPHA + "' }.status", hasItem("active")));
    }

    @Test
    @Order(3)
    void frozen_mode_rejects_a_coordinate_the_lock_does_not_carry() {
        Configuration.set("DIRIGIBLE_DEPENDENCIES_FROZEN", "true");
        try {
            writeProjectJson("""
                    { "type": "maven", "id": "com.example:alpha:1.0.0" },
                    { "type": "maven", "id": "com.example:survivor-module:1.0.0" },
                    { "type": "maven", "id": "com.example:gamma:1.0.0" }""");

            resolveExpecting(response -> response.body("frozen", is(true))
                                                 .body("failures", hasKey("com.example:gamma:1.0.0"))
                                                 .body("failures.'com.example:gamma:1.0.0'", containsString("frozen mode"))
                                                 .body("report.findAll { it.coordinate == 'com.example:gamma:1.0.0' }.status",
                                                         hasItem("frozen-mismatch"))
                                                 .body("report.findAll { it.coordinate == '" + ALPHA + "' }.status", hasItem("active")));

            // frozen mode never consulted any repository - the rejected coordinate was not downloaded
            assertThat(localRepository.resolve("com/example/gamma/1.0.0/gamma-1.0.0.jar")).doesNotExist();
            awaitEndpoint(SURVIVOR_ENDPOINT, "survivor alive");
        } finally {
            Configuration.set("DIRIGIBLE_DEPENDENCIES_FROZEN", "false");
        }

        writeProjectJson("""
                { "type": "maven", "id": "com.example:alpha:1.0.0" },
                { "type": "maven", "id": "com.example:survivor-module:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", anEmptyMap()));
    }

    @Test
    @Order(4)
    void a_platform_provided_coordinate_is_shadowed_and_the_platform_class_serves() {
        repository.createResource(PROBE_SOURCE_PATH, """
                package client;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class GsonProbe implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        String marker = getClass().getClassLoader().getResource("gson-fixture.marker") == null ? "no-marker" : "marker";
                        response.getWriter().write(marker + ":" + new com.google.gson.Gson().getClass().getName());
                    }
                }
                """.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();

        writeProjectJson("""
                { "type": "maven", "id": "com.example:alpha:1.0.0" },
                { "type": "maven", "id": "com.example:survivor-module:1.0.0" },
                { "type": "maven", "id": "com.google.code.gson:gson:1.0.0-fixture" }""");

        // shadowing is a report, not a failure - the resolution itself stays clean
        resolveExpecting(response -> response.body("failures", anEmptyMap())
                                             .body("report.findAll { it.coordinate == 'com.google.code.gson:gson:1.0.0-fixture' }.status",
                                                     hasItem("shadowed"))
                                             .body("report.findAll { it.coordinate == 'com.google.code.gson:gson:1.0.0-fixture' }.message[0]",
                                                     containsString("requested: 1.0.0-fixture")));

        // never downloaded - parent-first delegation would never serve it anyway
        assertThat(localRepository.resolve("com/google/code/gson/gson/1.0.0-fixture/gson-1.0.0-fixture.jar")).doesNotExist();

        // the class actually loaded is the platform's: the fixture's marker resource is absent and
        // the platform's Gson answers
        awaitEndpoint(PROBE_ENDPOINT, "no-marker:com.google.gson.Gson");
    }

    private void writeProjectJson(String dependencyEntries) {
        String content = """
                {
                    "guid": "%s",
                    "dependencies": [
                %s
                    ]
                }
                """.formatted(PROJECT, dependencyEntries.indent(8));
        repository.createResource(PROJECT_JSON_PATH, content.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
    }

    private void resolveExpecting(Consumer<ValidatableResponse> assertions) {
        restAssuredExecutor.execute(() -> {
            ValidatableResponse response = given().when()
                                                  .post("/services/core/dependencies/resolve")
                                                  .then()
                                                  .statusCode(200)
                                                  .contentType(ContentType.JSON);
            assertions.accept(response);
        });
    }

    private void awaitEndpoint(String endpoint, String expectedBodyFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(endpoint)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedBodyFragment)),
                AWAIT_SECONDS);
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is mandated by every JVM", e);
        }
        return HexFormat.of()
                        .formatHex(digest.digest(Files.readAllBytes(file)));
    }

}
