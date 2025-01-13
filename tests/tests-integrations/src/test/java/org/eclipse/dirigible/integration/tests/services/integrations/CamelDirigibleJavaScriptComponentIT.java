/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.services.integrations;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.integration.tests.ui.tests.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

class CamelDirigibleJavaScriptComponentIT extends UserInterfaceIntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    private LogsAsserter logsAsserter;

    @BeforeEach
    void setUp() {
        this.logsAsserter = new LogsAsserter("CustomComponentLogger", Level.INFO);
    }

    @Test
    void testInvokeJSWithCronRoute() {
        ide.createAndPublishProjectFromResources("CamelDirigibleJavaScriptComponentIT/cron-route/call-dirigible-js-custom-component");

        assertBodyIsPassedAndHandledProperlyByJSHandler();
    }

    private void assertBodyIsPassedAndHandledProperlyByJSHandler() {
        // this log message is expected to be logged by the final camel log step
        await().atMost(20, TimeUnit.SECONDS)
               .until(() -> logsAsserter.containsMessage("Completed execution. Body: [MY TEST BODY]", Level.INFO));
    }

    @Test
    void testInvokeJSWithEndpointRoute() {
        ide.createAndPublishProjectFromResources(
                "CamelDirigibleJavaScriptComponentIT/http-route/http-route-call-dirigible-js-custom-component");

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/http-route")
                             .then()
                             .statusCode(200)
                             .body(containsString("Body set by the handler")),
                25);

    }

}
