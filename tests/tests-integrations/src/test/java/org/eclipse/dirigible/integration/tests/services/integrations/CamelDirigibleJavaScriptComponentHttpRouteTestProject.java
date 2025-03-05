/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.services.integrations;

import org.eclipse.dirigible.integration.tests.ui.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Lazy
@Component
class CamelDirigibleJavaScriptComponentHttpRouteTestProject extends BaseTestProject {

    private final RestAssuredExecutor restAssuredExecutor;

    CamelDirigibleJavaScriptComponentHttpRouteTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView,
            RestAssuredExecutor restAssuredExecutor) {
        super("CamelDirigibleJavaScriptComponentHttpRouteIT", ide, projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
    }

    @Override
    public void verify() throws SQLException {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/http-route")
                             .then()
                             .statusCode(200)
                             .body(containsString("Body set by the handler")),
                25);
    }

}
