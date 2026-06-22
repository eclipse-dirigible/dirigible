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
import static org.hamcrest.Matchers.containsString;

public class JavaWebsocketDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String PROJECT = "sample-java-websocket-decorator";
    private static final String WEBSOCKET_STATUS_BASE = "/services/java/" + PROJECT + "/demo/websocket/WebsocketStatus";

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-java-websocket-decorator.git";
    }

    @Override
    protected void verifyProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(WEBSOCKET_STATUS_BASE + "/status")
                                                 .then()
                                                 .statusCode(200)
                                                 // Self-describing interface style — ChatHandler implements WebsocketHandler.
                                                 .body(containsString("\"chat\":true"))
                                                 // Method-level annotation style — TickerHandler is @Websocket + @OnX.
                                                 .body(containsString("\"ticker\":true")));
    }

}
