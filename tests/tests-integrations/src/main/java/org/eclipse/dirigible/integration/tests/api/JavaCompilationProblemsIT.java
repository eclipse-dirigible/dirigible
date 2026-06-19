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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that a client-Java compile failure surfaces as a Problems-view entry (category
 * {@code Compilation}, with the failing line) and that fixing or removing the source clears it.
 * HTTP-only against the Problems endpoint - no Selenide.
 */
class JavaCompilationProblemsIT extends IntegrationTest {

    private static final String PROJECT = "java-problems-it";
    private static final String SOURCE_LOCATION = "/" + PROJECT + "/demo/Broken.java";
    private static final String REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + SOURCE_LOCATION;
    private static final String PROBLEMS_ENDPOINT = "/services/ide/problems";
    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    /** A GPath filter selecting our file's compilation problems out of the global list. */
    private static final String OURS = "findAll { it.category == 'Compilation' && it.location == '" + SOURCE_LOCATION + "' }";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void compile_failure_creates_a_problem_and_a_fix_clears_it() {
        writeAndSync(brokenSource());

        // A compilation problem exists for this file, at the offending line.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OURS + ".size()", greaterThan(0))
                                                 .body(OURS + "[0].line", not(emptyOrNullString())),
                ASSERTION_TIMEOUT_SECONDS);

        // Fixing the source clears the problem on the next synchronization.
        writeAndSync(validSource());
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OURS + ".size()", equalTo(0)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void deleting_a_broken_source_clears_its_problem() {
        writeAndSync(brokenSource());
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OURS + ".size()", greaterThan(0)),
                ASSERTION_TIMEOUT_SECONDS);

        repository.removeResource(REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROBLEMS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OURS + ".size()", equalTo(0)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void removeArtefactFromRegistry() {
        if (repository.hasResource(REGISTRY_PATH)) {
            repository.removeResource(REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private void writeAndSync(String source) {
        repository.createResource(REGISTRY_PATH, source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String brokenSource() {
        // Line 8 references a non-existent symbol - javac reports an error there.
        return """
                package demo;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class Broken implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        thisMethodDoesNotExist(request);
                    }
                }
                """;
    }

    private static String validSource() {
        return """
                package demo;
                import jakarta.servlet.http.HttpServletRequest;
                import jakarta.servlet.http.HttpServletResponse;
                import org.eclipse.dirigible.engine.java.handler.JavaHandler;
                public class Broken implements JavaHandler {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
                        response.getWriter().write("ok");
                    }
                }
                """;
    }
}
