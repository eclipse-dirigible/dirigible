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
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.dirigible.engine.java.domain.JavaFile;
import org.eclipse.dirigible.engine.java.service.JavaFileService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;

/**
 * The reference project of the in-process unit-test slice (dirigible #7643), published to the
 * platform: its unit tests under {@code custom/test/} are compiled by the module's Maven build
 * (where {@code dirigible-sdk-test} runs them), never by the platform - which has no JUnit to
 * compile them against - while the application they test runs exactly as the slice runs it.
 */
class IntentSliceSampleIT extends IntegrationTest {

    private static final String PROJECT = "sample-intent-money";
    private static final String LINE_API = "/services/java/" + PROJECT + "/gen/money/api/invoiceline/InvoiceLineController";

    @Autowired
    private ProjectDeployer projectDeployer;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private JavaFileService javaFileService;

    @Test
    void publishesTheApplicationWithoutItsUnitTests() {
        projectDeployer.deploy(PROJECT);

        List<String> locations = javaFileService.findByProject(PROJECT)
                                                .stream()
                                                .map(JavaFile::getLocation)
                                                .sorted()
                                                .toList();
        assertEquals(List.of("/" + PROJECT + "/custom/LineVatAction.java", //
                "/" + PROJECT + "/gen/money/api/invoiceline/InvoiceLineController.java", //
                "/" + PROJECT + "/gen/money/data/invoiceline/InvoiceLineEntity.java", //
                "/" + PROJECT + "/gen/money/data/invoiceline/InvoiceLineRepository.java"), locations,
                "the platform compiles the application and leaves custom/test/ to the module's build");
        assertTrue(javaFileService.findByProject(PROJECT)
                                  .stream()
                                  .allMatch(file -> file.getError() == null),
                "every application class compiled and wired: " + javaFileService.findByProject(PROJECT));

        // The money path the slice's LineVatActionTest pins: 1 x 2.90 at 5% is 0.145, half-up 0.15.
        RestAssuredConfig exactDecimals = RestAssuredConfig.config()
                                                           .jsonConfig(JsonConfig.jsonConfig()
                                                                                 .numberReturnType(
                                                                                         JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        restAssuredExecutor.execute(() -> given().config(exactDecimals)
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"Description\":\"Paper\",\"Quantity\":1,\"UnitPrice\":2.90,\"VatRate\":5}")
                                                 .when()
                                                 .post(LINE_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("VatAmount", comparesEqualTo(new BigDecimal("0.15"))),
                25);
    }
}
