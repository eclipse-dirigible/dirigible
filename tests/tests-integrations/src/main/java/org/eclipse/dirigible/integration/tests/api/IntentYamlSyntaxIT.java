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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.equalTo;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A YAML syntax error in an intent answers {@code 422} with an issue naming the line and column on
 * every endpoint that parses the document - never a {@code 500} carrying the parser's raw message
 * (dirigible #8185). HTTP-only.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentYamlSyntaxIT extends IntegrationTest {

    /** The flow mapping on line 4 is never closed. */
    private static final String MALFORMED_YAML = """
            name: yamlsyntax
            entities:
              - name: A
                fields: [ {bad
            """;

    /** The stream ends on line 5 while the flow mapping opened on line 4 is still open. */
    private static final String SYNTAX_ISSUE = "YAML syntax error at line 5, column 1: expected ',' or '}', but got <stream end>"
            + " (while parsing a flow mapping that started at line 4, column 15)";
    private static final String PROJECT = "intent-yaml-syntax";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void parseAnswersUnprocessableWithThePosition() {
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(MALFORMED_YAML)
                                                 .when()
                                                 .post("/services/ide/intent/parse")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasSize(1))
                                                 .body("issues[0]", equalTo(SYNTAX_ISSUE)));
    }

    @Test
    void validateAnswersUnprocessableWithThePosition() {
        restAssuredExecutor.execute(() -> given().contentType("text/plain")
                                                 .body(MALFORMED_YAML)
                                                 .when()
                                                 .post("/services/ide/intent/validate")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasSize(1))
                                                 .body("issues[0]", equalTo(SYNTAX_ISSUE)));
    }

    @Test
    void generateAnswersUnprocessableWithThePosition() {
        repository.createResource(PROJECT_PATH + "/app.intent", MALFORMED_YAML.getBytes(StandardCharsets.UTF_8));

        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/intent/generate?workspace=workspace&project=" + PROJECT
                                                         + "&path=app.intent")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("issues", hasSize(1))
                                                 .body("issues[0]", equalTo(SYNTAX_ISSUE)));
    }

    @AfterEach
    void removeProject() {
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
