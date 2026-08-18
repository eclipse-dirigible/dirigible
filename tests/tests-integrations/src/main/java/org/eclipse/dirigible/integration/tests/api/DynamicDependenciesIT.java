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
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;

/**
 * The restartless dependency lifecycle, end to end over one running platform: an AOT module jar
 * arrives (via the declaration watcher - zero runtime javac for its classes), a third-party library
 * becomes importable from registry {@code .java} sources, the module upgrades, a native-library jar
 * and an unreadable jar are rejected without a partial swap, and the removed module leaves cleanly.
 * All repositories are file: fixtures built by the test - no network.
 */
// One Dirigible boot for the whole journey; the steps build on each other in @Order sequence.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynamicDependenciesIT extends IntegrationTest {

    /** The registry project declaring the maven dependencies. */
    private static final String PROJECT = "dynamic-deps-it";
    private static final String PROJECT_JSON_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/project.json";
    private static final String CLIENT_SOURCE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/client/LibUser.java";

    /** The AOT module's controller - served straight from the module jar, never compiled at runtime. */
    private static final String MODULE_ENDPOINT = "/services/java/hello-module/aotdemo/HelloController/hello";

    /** The registry handler importing the third-party fixture library. */
    private static final String CLIENT_ENDPOINT = "/services/java/" + PROJECT + "/client/LibUser";

    private static final long AWAIT_SECONDS = 60;

    @TempDir
    static Path tempDir;

    private static Path fixtureRepo;
    private static Path localRepository;

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
        // the fixture repository REPLACES Maven Central, so no request can leave the machine
        Configuration.set("DIRIGIBLE_MAVEN_REPOSITORIES", "central=" + fixtureRepo.toUri());
        Configuration.set("DIRIGIBLE_MAVEN_LOCAL_REPO", localRepository.toString());
        Configuration.set("DIRIGIBLE_DEPENDENCIES_DIR", tempDir.resolve("resolved-modules")
                                                               .toString());

        Path work = Files.createDirectories(tempDir.resolve("work"));
        MavenFixtures.deploy(fixtureRepo, "com.example", "hello-module", "1.0.0",
                MavenFixtures.buildModuleJar(work, "hello-module-1.0.0.jar", "hello-module",
                        Map.of("aotdemo.HelloController", helloControllerSource("1.0.0")), Map.of("greeting.txt", "v1.0.0")));
        MavenFixtures.deploy(fixtureRepo, "com.example", "hello-module", "1.1.0",
                MavenFixtures.buildModuleJar(work, "hello-module-1.1.0.jar", "hello-module",
                        Map.of("aotdemo.HelloController", helloControllerSource("1.1.0")), Map.of("greeting.txt", "v1.1.0")));
        MavenFixtures.deploy(fixtureRepo, "com.example", "textlib", "1.0.0",
                MavenFixtures.buildPlainJar(work, "textlib-1.0.0.jar", Map.of("com.example.textlib.TextLib", """
                        package com.example.textlib;
                        public class TextLib {
                            public static String shout(String input) {
                                return input.toUpperCase() + "!";
                            }
                        }
                        """)));
        MavenFixtures.deploy(fixtureRepo, "com.example", "native-lib", "1.0.0",
                jarWithRawEntries(work.resolve("native-lib-1.0.0.jar"), Map.of("lib/dummy.so", "not really native")));
        Path brokenJar = work.resolve("broken-1.0.0.jar");
        Files.writeString(brokenJar, "this is not a jar");
        MavenFixtures.deploy(fixtureRepo, "com.example", "broken", "1.0.0", brokenJar);
    }

    @Test
    @Order(1)
    void an_aot_module_jar_activates_without_restart_via_the_declaration_watcher() {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:hello-module:1.0.0" }""");

        // no resolve call - the watcher notices the changed declaration and runs the swap pipeline;
        // the response proves zero runtime javac: the class is served by the modules classloader
        awaitEndpoint(MODULE_ENDPOINT, "hello from 1.0.0 via dirigible-modules");

        assertThat(registryContent("/hello-module/greeting.txt")).isEqualTo("v1.0.0");
    }

    @Test
    @Order(2)
    void a_registry_java_source_compiles_against_a_declared_third_party_library() {
        repository.createResource(CLIENT_SOURCE_PATH, """
                package client;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                import com.example.textlib.TextLib;
                public class LibUser implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.getWriter().write(TextLib.shout("dirigible"));
                    }
                }
                """.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        // the source registers (and fails to compile - the library is not declared yet)
        synchronizationProcessor.forceProcessSynchronizers();

        writeProjectJson("""
                { "type": "maven", "id": "com.example:hello-module:1.0.0" },
                { "type": "maven", "id": "com.example:textlib:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", anEmptyMap()));

        awaitEndpoint(CLIENT_ENDPOINT, "DIRIGIBLE!");
    }

    @Test
    @Order(3)
    void an_upgraded_module_swaps_its_classes_and_registry_payload() {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:hello-module:1.1.0" },
                { "type": "maven", "id": "com.example:textlib:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", anEmptyMap()));

        awaitEndpoint(MODULE_ENDPOINT, "hello from 1.1.0 via dirigible-modules");
        assertThat(registryContent("/hello-module/greeting.txt")).isEqualTo("v1.1.0");
        // the old version's jar stays untouched at its immutable versioned path
        assertThat(localRepository.resolve("com/example/hello-module/1.0.0/hello-module-1.0.0.jar")).exists();
    }

    @Test
    @Order(4)
    void a_native_library_jar_is_rejected_and_the_installed_generation_keeps_serving() {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:hello-module:1.1.0" },
                { "type": "maven", "id": "com.example:textlib:1.0.0" },
                { "type": "maven", "id": "com.example:native-lib:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", hasKey("modules-swap"))
                                             .body("failures.'modules-swap'", containsString("lib/dummy.so"))
                                             .body("failures.'modules-swap'", containsString("platform")));

        // no partial swap - the previous generation answers as before
        awaitEndpoint(MODULE_ENDPOINT, "hello from 1.1.0 via dirigible-modules");
        awaitEndpoint(CLIENT_ENDPOINT, "DIRIGIBLE!");
    }

    @Test
    @Order(5)
    void an_unreadable_jar_aborts_the_swap_and_the_installed_generation_keeps_serving() {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:hello-module:1.1.0" },
                { "type": "maven", "id": "com.example:textlib:1.0.0" },
                { "type": "maven", "id": "com.example:broken:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", hasKey("modules-swap"))
                                             .body("failures.'modules-swap'", containsString("not a readable archive")));

        awaitEndpoint(MODULE_ENDPOINT, "hello from 1.1.0 via dirigible-modules");
    }

    @Test
    @Order(6)
    void a_removed_module_unregisters_its_classes_and_registry_payload() {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:textlib:1.0.0" }""");
        resolveExpecting(response -> response.body("failures", anEmptyMap()));

        awaitEndpointStatus(MODULE_ENDPOINT, 404);
        assertThat(repository.hasResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/hello-module/greeting.txt")).isFalse();
        // the surviving dependency keeps serving
        awaitEndpoint(CLIENT_ENDPOINT, "DIRIGIBLE!");
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

    private void awaitEndpointStatus(String endpoint, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(endpoint)
                                                 .then()
                                                 .statusCode(expectedStatus),
                AWAIT_SECONDS);
    }

    private String registryContent(String registryRelativePath) {
        return new String(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + registryRelativePath)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private static String helloControllerSource(String version) {
        return """
                package aotdemo;
                import org.eclipse.dirigible.sdk.http.Controller;
                import org.eclipse.dirigible.sdk.http.Get;
                @Controller
                public class HelloController {
                    @Get("/hello")
                    public String hello() {
                        return "hello from %s via " + getClass().getClassLoader().getName();
                    }
                }
                """.formatted(version);
    }

    private static Path jarWithRawEntries(Path jarPath, Map<String, String> entries) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue()
                               .getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jarPath;
    }

}
