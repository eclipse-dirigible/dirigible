/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.camel;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectDeployer;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * An HTTP-triggered route reaches its JavaScript component and answers with the body that component
 * set.
 */
public class CamelDirigibleJavaScriptComponentHttpRouteIT extends IntegrationTest {

    private static final String PROJECT = "CamelDirigibleJavaScriptComponentHttpRouteIT";

    @Autowired
    private ProjectDeployer projectDeployer;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void theRouteAnswersWithTheBodyItsComponentSet() {
        projectDeployer.deploy(PROJECT);

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/http-route")
                             .then()
                             .statusCode(200)
                             .body(containsString("Body set by the handler")),
                25);
    }
}
