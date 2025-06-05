/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.camel;

import org.eclipse.dirigible.tests.base.BaseTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Lazy
@Component
class CamelAsyncStepTestProject extends BaseTestProject {

    private final RestAssuredExecutor restAssuredExecutor;

    public CamelAsyncStepTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, RestAssuredExecutor restAssuredExecutor) {
        super("CamelAsyncStepIT", ide, projectUtil, edmView);
        this.restAssuredExecutor = restAssuredExecutor;
    }

    @Override
    public void verify() {
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/CamelAsyncStepITRoute")
                             .then()
                             .statusCode(200)
                             .body(containsString("My name is: John Doe")),
                25);
    }
}
