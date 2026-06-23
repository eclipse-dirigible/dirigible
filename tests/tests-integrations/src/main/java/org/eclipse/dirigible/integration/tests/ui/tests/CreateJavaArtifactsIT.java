/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Drives the Workbench "New -> Java" submenu and asserts that each artefact lands in the right
 * place with the right content: a fully-qualified name creates the package folders and the matching
 * {@code package} declaration, a simple name lands in the project root with no package, and each
 * skeleton follows the strong-interface style. Content is read back over the workspace REST API
 * rather than the Monaco DOM, which keeps the assertions deterministic.
 */
class CreateJavaArtifactsIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "CreateJavaArtifactsIT";
    private static final String WORKSPACE_FILES = "/services/ide/workspaces/workspace/" + PROJECT;
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    protected RestAssuredExecutor restAssuredExecutor;

    @Test
    void createsJavaArtifactsFromTheNewMenu() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT);

        // Fully-qualified name -> package folders are created and the package declaration matches.
        workbench.createJavaArtifact(PROJECT, "Class", "com.test.MyClass");
        assertFileContains("/com/test/MyClass.java", "package com.test;", "public class MyClass");

        // Simple name -> created in the project root, no package declaration.
        workbench.createJavaArtifact(PROJECT, "Interface", "MyService");
        assertFileContains("/MyService.java", "public interface MyService");
        assertFileDoesNotContain("/MyService.java", "package ");

        workbench.createJavaArtifact(PROJECT, "Enum", "Color");
        assertFileContains("/Color.java", "public enum Color");

        workbench.createJavaArtifact(PROJECT, "Annotation", "MyMarker");
        assertFileContains("/MyMarker.java", "public @interface MyMarker");

        workbench.createJavaArtifact(PROJECT, "Record", "Point");
        assertFileContains("/Point.java", "public record Point(");

        workbench.createJavaArtifact(PROJECT, "Exception", "DomainException");
        assertFileContains("/DomainException.java", "extends Exception");

        // Strong-interface skeletons.
        workbench.createJavaArtifact(PROJECT, "Controller", "GreetController");
        assertFileContains("/GreetController.java", "@Controller", "org.eclipse.dirigible.sdk.http.Get");

        workbench.createJavaArtifact(PROJECT, "Job", "CleanupJob");
        assertFileContains("/CleanupJob.java", "implements JobHandler", "public String cron()");

        workbench.createJavaArtifact(PROJECT, "Listener", "OrderListener");
        assertFileContains("/OrderListener.java", "implements MessageHandler", "ListenerKind");

        // Repository additionally prompts for the entity type it manages.
        workbench.createJavaArtifact(PROJECT, "Repository", "CountryRepository", "Country");
        assertFileContains("/CountryRepository.java", "@Repository", "extends JavaRepository<Country>", "super(Country.class)");

        // Package -> the full nested folder chain is created from a dotted name.
        workbench.createJavaArtifact(PROJECT, "Package", "com.test.iliyan");
        assertFolderExists("/com/test/iliyan");
    }

    private void assertFileContains(String path, String... fragments) {
        restAssuredExecutor.execute(() -> {
            var response = given().when()
                                  .get(WORKSPACE_FILES + path)
                                  .then()
                                  .statusCode(200);
            for (String fragment : fragments) {
                response.body(containsString(fragment));
            }
        }, TIMEOUT_SECONDS);
    }

    private void assertFileDoesNotContain(String path, String fragment) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WORKSPACE_FILES + path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(not(containsString(fragment))),
                TIMEOUT_SECONDS);
    }

    private void assertFolderExists(String path) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WORKSPACE_FILES + path)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

}
