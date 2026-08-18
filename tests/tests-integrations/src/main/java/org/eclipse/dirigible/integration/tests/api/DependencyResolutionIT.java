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
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * End-to-end test for the maven dependency type in project.json: a fixture coordinate served from a
 * file: repository is declared, resolved through the REST surface into the local repository and
 * linked into the resolved-modules directory, and the state endpoint reports it. No test touches
 * the network - the fixture repository replaces Maven Central.
 */
// One Dirigible boot for the whole class: each method cleans up the declaring project after itself.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DependencyResolutionIT extends IntegrationTest {

    /** The project declaring the maven dependencies. */
    private static final String PROJECT = "dependency-resolution-it";

    /** The project.json path under the registry root. */
    private static final String PROJECT_JSON_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/project.json";

    /** The resolver's logger - package-private class, so the name is spelled out. */
    private static final String RESOLVER_LOGGER = "org.eclipse.dirigible.components.dependencies.MavenDependencyResolver";

    @TempDir
    static Path tempDir;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private Path localRepository;
    private Path resolvedModulesDir;
    private LogsAsserter resolverLogs;

    @BeforeEach
    void setUp() throws IOException {
        Path fixtureRepo = Files.createDirectories(tempDir.resolve("fixture-repo"));
        deploy(fixtureRepo, "leaf", "1.0.0", "");
        deploy(fixtureRepo, "mid", "1.0.0", dependencyOn("leaf", "1.0.0"));
        localRepository = tempDir.resolve("local-repo");
        resolvedModulesDir = tempDir.resolve("resolved-modules");
        // the fixture repository REPLACES Maven Central, so no request can leave the machine
        Configuration.set("DIRIGIBLE_MAVEN_REPOSITORIES", "central=" + fixtureRepo.toUri());
        Configuration.set("DIRIGIBLE_MAVEN_LOCAL_REPO", localRepository.toString());
        Configuration.set("DIRIGIBLE_DEPENDENCIES_DIR", resolvedModulesDir.toString());
        resolverLogs = new LogsAsserter(RESOLVER_LOGGER, Level.ERROR);
    }

    @AfterEach
    void removeProjectFromRegistry() {
        if (repository.hasResource(PROJECT_JSON_PATH)) {
            repository.removeResource(PROJECT_JSON_PATH);
        }
    }

    @Test
    void a_declared_maven_dependency_is_resolved_and_activated() {
        writeProjectJson("""
                {
                    "guid": "%s",
                    "dependencies": [
                        { "type": "maven", "id": "com.example:mid:1.0.0" }
                    ]
                }
                """.formatted(PROJECT));

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/core/dependencies/resolve")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("failures", anEmptyMap())
                                                 .body("declared", hasItem("com.example:mid:1.0.0"))
                                                 .body("artifacts", hasSize(2)));

        // the artifacts landed in the local repository at their immutable versioned paths
        assertThat(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar")).exists();
        assertThat(localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar")).exists();

        // and are linked into the resolved-modules directory the next launch's classpath includes
        assertThat(resolvedModulesDir.resolve("com.example-mid-1.0.0.jar")).exists();
        assertThat(resolvedModulesDir.resolve("com.example-leaf-1.0.0.jar")).exists();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/dependencies")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("enabled", is(true))
                                                 .body("declared", hasItem("com.example:mid:1.0.0"))
                                                 .body("artifacts", hasSize(2))
                                                 .body("failures", anEmptyMap()));
    }

    @Test
    void an_unresolvable_coordinate_is_reported_without_failing_the_platform() {
        writeProjectJson("""
                {
                    "guid": "%s",
                    "dependencies": [
                        { "type": "maven", "id": "com.example:mid:1.0.0" },
                        { "type": "maven", "id": "com.example:missing:9.9.9" }
                    ]
                }
                """.formatted(PROJECT));

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/core/dependencies/resolve")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("failures", hasKey("com.example:missing:9.9.9"))
                                                 // the resolvable declaration still made it
                                                 .body("artifacts", hasSize(2)));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/core/dependencies")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("failures", hasKey("com.example:missing:9.9.9")));

        // the error log names the coordinate and the repositories tried
        assertThat(resolverLogs.containsMessage("Could not resolve maven dependency [com.example:missing:9.9.9] from repositories [central",
                Level.ERROR)).isTrue();
    }

    private void writeProjectJson(String content) {
        repository.createResource(PROJECT_JSON_PATH, content.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
    }

    private static void deploy(Path repositoryDir, String artifactId, String version, String dependenciesXml) throws IOException {
        Path directory = repositoryDir.resolve("com/example")
                                      .resolve(artifactId)
                                      .resolve(version);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(artifactId, version, dependenciesXml));
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(directory.resolve(artifactId + "-" + version + ".jar")))) {
            out.putNextEntry(new JarEntry("fixture.txt"));
            out.write("fixture".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private static String dependencyOn(String artifactId, String version) {
        return """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>%s</artifactId>
                        <version>%s</version>
                    </dependency>
                </dependencies>
                """.formatted(artifactId, version);
    }

}
