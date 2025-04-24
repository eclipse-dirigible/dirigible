/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.projects.BaseTestProject;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Lazy
@Component
class CamelDirigibleTwoStepsJSInvokerHttpRouteTestProject extends BaseTestProject {

    private final RestAssuredExecutor restAssuredExecutor;

    CamelDirigibleTwoStepsJSInvokerHttpRouteTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView,
            RestAssuredExecutor restAssuredExecutor) {
        super("CamelDirigibleTwoStepsJSInvokerHttpRouteIT", ide, projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
    }

    @Override
    public void verify() throws SQLException {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/http-route")
                             .then()
                             .statusCode(200)
                             .body(containsString("This is a body set by the handler")),
                25);
    }

}
