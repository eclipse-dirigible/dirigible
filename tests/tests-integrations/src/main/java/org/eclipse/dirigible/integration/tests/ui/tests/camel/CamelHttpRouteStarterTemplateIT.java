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

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.WelcomeView;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.SynchronizationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

public class CamelHttpRouteStarterTemplateIT extends UserInterfaceIntegrationTest {

    private static final String TEMPLATE_TITLE = "HTTP Route Project Starter";
    private static final String TEST_PROJECT = CamelHttpRouteStarterTemplateIT.class.getSimpleName();

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void testCreateProjectFromTemplate() {
        Workbench workbench = ide.openWorkbench();

        WelcomeView welcomeView = workbench.openWelcomeView();
        welcomeView.searchForTemplate(TEMPLATE_TITLE);
        welcomeView.selectTemplate(TEMPLATE_TITLE);

        welcomeView.typeProjectName(TEST_PROJECT);
        welcomeView.typeFileName(TEST_PROJECT);
        welcomeView.confirmTemplate();

        workbench.publishAll(true);
        SynchronizationUtil.waitForSynchronizationExecution();

        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/services/integrations/http-route")
                             .then()
                             .statusCode(200)
                             .body(containsString("Set by handler.ts")),
                15);
    }

}

