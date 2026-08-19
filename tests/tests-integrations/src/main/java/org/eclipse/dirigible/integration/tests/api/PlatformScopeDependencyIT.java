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

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.launcher.agent.InstrumentationHolder;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
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
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * The {@code scope: "platform"} lifecycle, end to end over one running platform with a real
 * launcher-agent delivery ({@code -javaagent} on the failsafe fork): a platform-scoped library
 * becomes visible to platform code and registry {@code .java} sources without a restart, a JDBC
 * driver registers with {@code DriverManager}, and a version bump honors the append-only contract
 * as pending-restart. All repositories are file: fixtures - no network.
 */
// One Dirigible boot for the whole journey; the steps build on each other in @Order sequence.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlatformScopeDependencyIT extends IntegrationTest {

    /** The registry project declaring the maven dependencies. */
    private static final String PROJECT = "platform-scope-it";
    private static final String PROJECT_JSON_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/project.json";
    private static final String CLIENT_SOURCE_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/client/PlatformLibUser.java";

    /** The registry handler importing the platform-scoped fixture library. */
    private static final String CLIENT_ENDPOINT = "/services/java/" + PROJECT + "/client/PlatformLibUser";

    private static final String LIB_CLASS = "com.example.platformlib.PlatformLib";
    private static final String DRIVER_CLASS = "com.example.fixturedriver.FixtureDriver";
    private static final String DRIVER_URL = "jdbc:fixture:mem";

    private static final long AWAIT_SECONDS = 60;

    @TempDir
    static Path tempDir;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeAll
    static void prepareFixtures() throws IOException {
        Path fixtureRepo = Files.createDirectories(tempDir.resolve("fixture-repo"));
        // the fixture repository REPLACES Maven Central, so no request can leave the machine
        Configuration.set("DIRIGIBLE_MAVEN_REPOSITORIES", "central=" + fixtureRepo.toUri());
        Configuration.set("DIRIGIBLE_MAVEN_LOCAL_REPO", tempDir.resolve("local-repo")
                                                               .toString());
        Configuration.set("DIRIGIBLE_DEPENDENCIES_DIR", tempDir.resolve("resolved-modules")
                                                               .toString());

        Path work = Files.createDirectories(tempDir.resolve("work"));
        MavenFixtures.deploy(fixtureRepo, "com.example", "platformlib", "1.0.0",
                MavenFixtures.buildPlainJar(work, "platformlib-1.0.0.jar", Map.of(LIB_CLASS, platformLibSource("v1"))));
        MavenFixtures.deploy(fixtureRepo, "com.example", "platformlib", "2.0.0",
                MavenFixtures.buildPlainJar(work, "platformlib-2.0.0.jar", Map.of(LIB_CLASS, platformLibSource("v2"))));
        MavenFixtures.deploy(fixtureRepo, "com.example", "fixture-driver", "1.0.0",
                MavenFixtures.buildPlainJar(work, "fixture-driver-1.0.0.jar", Map.of(DRIVER_CLASS, fixtureDriverSource()),
                        Map.of("META-INF/services/java.sql.Driver", DRIVER_CLASS)));
    }

    @Test
    @Order(1)
    void the_launcher_agent_delivered_instrumentation_to_this_jvm() {
        // pins the -javaagent delivery of the failsafe fork - the production executable jar pins
        // its own delivery in LauncherAgentDeliveryIT
        assertThat(InstrumentationHolder.get()).as("the -javaagent delivery must capture Instrumentation before main")
                                               .isNotNull();
    }

    @Test
    @Order(2)
    void a_platform_scoped_library_activates_without_restart() throws Exception {
        repository.createResource(CLIENT_SOURCE_PATH, """
                package client;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                import com.example.platformlib.PlatformLib;
                public class PlatformLibUser implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.getWriter().write(PlatformLib.marker() + ":" + PlatformLib.shout("platform"));
                    }
                }
                """.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        writeProjectJson("""
                { "type": "maven", "id": "com.example:platformlib:1.0.0", "scope": "platform" }""");

        resolveExpectingPlatformStatus("com.example:platformlib:1.0.0", "active");

        // platform-side visibility: this test class is platform code - the appended jar resolves
        // through the system classloader parent chain without any restart
        Class<?> platformLib = Class.forName(LIB_CLASS);
        assertThat(platformLib.getMethod("marker")
                              .invoke(null)).isEqualTo("v1");

        // and a registry .java source compiles against it and serves
        awaitEndpoint(CLIENT_ENDPOINT, "v1:PLATFORM!");
    }

    @Test
    @Order(3)
    void an_appended_jdbc_driver_serves_driver_manager_from_platform_code() throws Exception {
        writeProjectJson("""
                { "type": "maven", "id": "com.example:platformlib:1.0.0", "scope": "platform" },
                { "type": "maven", "id": "com.example:fixture-driver:1.0.0", "scope": "platform" }""");

        resolveExpectingPlatformStatus("com.example:fixture-driver:1.0.0", "active");

        // DriverManager applies a caller-classloader visibility check - this call succeeding from
        // platform code is exactly what the platform tier exists for
        Driver driver = DriverManager.getDriver(DRIVER_URL);
        assertThat(driver.getClass()
                         .getName()).isEqualTo(DRIVER_CLASS);
    }

    @Test
    @Order(4)
    void a_version_bump_is_pending_restart_per_the_append_only_contract() throws Exception {
        LogsAsserter installerLogs = new LogsAsserter("org.eclipse.dirigible.components.dependencies.PlatformScopeInstaller", Level.WARN);
        writeProjectJson("""
                { "type": "maven", "id": "com.example:platformlib:2.0.0", "scope": "platform" },
                { "type": "maven", "id": "com.example:fixture-driver:1.0.0", "scope": "platform" }""");

        resolveExpectingPlatformStatus("com.example:platformlib:2.0.0", "pending-restart");

        // nothing new was appended: the process keeps serving 1.0.0
        Class<?> platformLib = Class.forName(LIB_CLASS);
        assertThat(platformLib.getMethod("marker")
                              .invoke(null)).isEqualTo("v1");
        // and the WARN names both versions
        assertThat(installerLogs.containsMessage("1.0.0", Level.WARN)).isTrue();
        assertThat(installerLogs.containsMessage("2.0.0", Level.WARN)).isTrue();
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

    private void resolveExpectingPlatformStatus(String coordinate, String status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/core/dependencies/resolve")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("platform.findAll { it.coordinate == '" + coordinate + "' }.status",
                                                         hasItem(status)));
    }

    private void awaitEndpoint(String endpoint, String expectedBodyFragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(endpoint)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(expectedBodyFragment)),
                AWAIT_SECONDS);
    }

    private static String platformLibSource(String marker) {
        return """
                package com.example.platformlib;
                public class PlatformLib {
                    public static String marker() {
                        return "%s";
                    }
                    public static String shout(String input) {
                        return input.toUpperCase() + "!";
                    }
                }
                """.formatted(marker);
    }

    private static String fixtureDriverSource() {
        return """
                package com.example.fixturedriver;
                import java.sql.Connection;
                import java.sql.Driver;
                import java.sql.DriverManager;
                import java.sql.DriverPropertyInfo;
                import java.sql.SQLException;
                import java.sql.SQLFeatureNotSupportedException;
                import java.util.Properties;
                import java.util.logging.Logger;
                public class FixtureDriver implements Driver {
                    static {
                        try {
                            DriverManager.registerDriver(new FixtureDriver());
                        } catch (SQLException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    @Override
                    public boolean acceptsURL(String url) {
                        return url != null && url.startsWith("jdbc:fixture:");
                    }
                    @Override
                    public Connection connect(String url, Properties info) {
                        return null;
                    }
                    @Override
                    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
                        return new DriverPropertyInfo[0];
                    }
                    @Override
                    public int getMajorVersion() {
                        return 1;
                    }
                    @Override
                    public int getMinorVersion() {
                        return 0;
                    }
                    @Override
                    public boolean jdbcCompliant() {
                        return false;
                    }
                    @Override
                    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                        throw new SQLFeatureNotSupportedException();
                    }
                }
                """;
    }

}
