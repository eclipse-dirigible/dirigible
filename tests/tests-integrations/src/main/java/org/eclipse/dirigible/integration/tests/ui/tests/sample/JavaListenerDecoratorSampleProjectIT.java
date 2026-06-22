/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;

import ch.qos.logback.classic.Level;

public class JavaListenerDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-listener-decorator";
    private static final String LISTENER_TRIGGER = "/services/js/" + PROJECT + "/demo/listener/trigger.mjs";

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-listener-decorator.git";
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LISTENER_TRIGGER)
                                                 .then()
                                                 .statusCode(200));

        // Self-describing interface style — OrderListener implements MessageHandler.
        await().atMost(15, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("OrderListener received:", Level.INFO));

        // Method-level annotation style — InvoiceListener's @Listener method records via the injected
        // Auditor.
        await().atMost(15, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("Auditor: invoice received:", Level.INFO));
    }

}
