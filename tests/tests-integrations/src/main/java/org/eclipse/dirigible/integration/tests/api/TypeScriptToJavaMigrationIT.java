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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * Moving a project generated with the removed TypeScript templates to Java (#8051): its inventory
 * lists what is left to port, its Regenerate is answered with the Java template that replaces the
 * one it records, regenerating with that one replaces the generated TypeScript with a Java
 * application that runs, and the assistant takes its hand-written TypeScript as a file to port.
 */
class TypeScriptToJavaMigrationIT extends IntegrationTest {

    private static final String PROJECT = "ts-books";

    private static final String RETIRED_TEMPLATE = "template-application-angular/template/template.js";

    private static final String SUCCESSOR = "template-application-ui-harmonia-java/template/template.js";

    private static final String INVENTORY = "/services/ide/javascript-deprecation/projects/" + PROJECT;

    private static final String GENERATE = "/services/ide/generate/model/workspace/" + PROJECT + "?path=" + PROJECT + ".model";

    private static final String WORKSPACE = "/services/ide/workspaces/workspace/" + PROJECT + "/";

    private static final String BOOKS = "/services/java/" + PROJECT + "/gen/ts_books/api/books/BookController";

    private static final String ASSIST = "/services/ide/intent/assist";

    private static final long GENERATION_TIMEOUT_SECONDS = 120;

    @Autowired
    private ProjectDeployer projectDeployer;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void aTypeScriptProjectIsRegeneratedAsJavaAndItsCustomScriptsAreLeftToPort() {
        projectDeployer.deploy("TypeScriptToJavaMigrationIT/" + PROJECT);

        // What is left to port: the generated TypeScript, marked with the template it came from, the
        // custom service, the extension module, and the browser code calling their URLs.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVENTORY)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("project", equalTo(PROJECT))
                                                 .body("scripts.find { it.path == 'gen/ts-books/dao/Books/BookRepository.ts' }.generated",
                                                         equalTo(true))
                                                 .body("scripts.find { it.path == 'gen/ts-books/dao/Books/BookRepository.ts' }.template",
                                                         equalTo(RETIRED_TEMPLATE))
                                                 .body("scripts.find { it.path == 'api/BooksReport.ts' }.generated", equalTo(false))
                                                 .body("scripts.find { it.path == 'api/BooksReport.ts' }.imports", hasItem("sdk/http"))
                                                 .body("scripts.find { it.path == 'api/BooksReport.ts' }.decorators",
                                                         hasItems("Controller", "Get"))
                                                 .body("scripts.find { it.kind == 'EXTENSION_MODULE' }.runs",
                                                         hasItem("ts-books/navigations/books/service.js"))
                                                 .body("references.path", hasItem("widgets/books-count/controller.js")));

        // The recorded template was removed: the generation is answered with the one that replaces it.
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(generation(RETIRED_TEMPLATE))
                                                 .when()
                                                 .post(GENERATE)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("template", equalTo(RETIRED_TEMPLATE))
                                                 .body("successor", equalTo(SUCCESSOR))
                                                 .body("message", containsString("removed in 14.16.0")));

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(generation(SUCCESSOR))
                                                 .when()
                                                 .post(GENERATE)
                                                 .then()
                                                 .statusCode(201));

        // The generated TypeScript is gone, the Java is there, and the descriptor records the successor.
        assertWorkspaceFile("gen/ts-books/dao/Books/BookRepository.ts", 404);
        assertWorkspaceFile("gen/ts-books/api/Books/BookService.ts", 404);
        assertWorkspaceFile("gen/ts_books/data/books/BookRepository.java", 200);
        assertWorkspaceFile(PROJECT + ".gen", 404);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WORKSPACE + PROJECT + ".model.gen")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(SUCCESSOR)));

        // The regenerated application runs.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BOOKS)
                                                 .then()
                                                 .statusCode(200),
                GENERATION_TIMEOUT_SECONDS);

        // Left to port: only the hand-written code.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVENTORY)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("scripts.generated", not(hasItem(true)))
                                                 .body("scripts.path",
                                                         hasItems("api/BooksReport.ts", "navigations/books/navigation.extension"))
                                                 .body("references.path", hasItem("widgets/books-count/controller.js")));
    }

    @Test
    void theAssistantTakesATypeScriptFileAsAFileToPortButNotAGeneratedOne() {
        projectDeployer.deploy("TypeScriptToJavaMigrationIT/" + PROJECT);

        // The port is accepted, and reaches the model call: no key is configured here.
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(assist("api/BooksReport.ts"))
                                                 .when()
                                                 .post(ASSIST)
                                                 .then()
                                                 .statusCode(412));
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(assist("gen/ts-books/api/Books/BookService.ts"))
                                                 .when()
                                                 .post(ASSIST)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("regenerate the project with the Java templates")));
    }

    @Test
    void anInventoryOfAProjectThatIsNotPublishedIsNotFound() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/ide/javascript-deprecation/projects/not-published")
                                                 .then()
                                                 .statusCode(404));
    }

    private void assertWorkspaceFile(String path, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WORKSPACE + path)
                                                 .then()
                                                 .statusCode(status));
    }

    /** The request the IDE's Regenerate sends: the descriptor's parameters, with the template named. */
    private static Map<String, Object> generation(String template) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tablePrefix", "TSB_");
        parameters.put("brand", "Eclipse Dirigible");
        parameters.put("brandUrl", "https://www.dirigible.io/");
        parameters.put("title", "TypeScript Books");
        parameters.put("description", "A project generated with the TypeScript application template");
        parameters.put("dataSource", "DefaultDB");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("template", template);
        request.put("parameters", parameters);
        return request;
    }

    private static Map<String, Object> assist(String path) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspace", "workspace");
        request.put("project", PROJECT);
        request.put("path", path);
        request.put("message", "Port this file to Java");
        return request;
    }
}
