/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;

public class ListenerDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private LogsAsserter consoleLogAsserter;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute( //
                () -> given().get("/services/js/sample-listener-decorator/OrderListenerTrigger.js")
                             .then()
                             .statusCode(200));

        consoleLogAsserter.containsMessage("Hello from the OrderListener Trigger! Message: [ I am a message created at:", Level.INFO);
        consoleLogAsserter.containsMessage("Processing message event: [ I am a message created at:", Level.INFO);
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-listener-decorator.git";
    }

}
