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
import io.restassured.parsing.Parser;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class RolesDecoratorSampleProjectIT extends SampleProjectRepositoryIT {

    private static final String ADMIN_USERNAME = "adm1";
    private static final String ADMIN_PASS = "adm1-pass";

    private static final String UNAUTHORIZED_USER_USERNAME = "unathorized-usr";
    private static final String UNAUTHORIZED_USER_PASS = "unathorized-usr-pass";

    @Autowired
    private SecurityUtil securityUtil;

    private LogsAsserter consoleErrorLogAsserter;

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/dirigiblelabs/sample-roles-decorator.git";
    }

    @BeforeEach
    void setUp() {
        this.consoleErrorLogAsserter = new LogsAsserter("app.err", Level.INFO);

    }

    @Override
    protected void verifyProject() {
        securityUtil.createUserInDefaultTenant(ADMIN_USERNAME, ADMIN_PASS, Roles.ADMINISTRATOR.getRoleName());
        restAssuredExecutor.execute(this::verifyAuthorizedUserAccess, ADMIN_USERNAME, ADMIN_PASS);

        securityUtil.createUserInDefaultTenant(UNAUTHORIZED_USER_USERNAME, UNAUTHORIZED_USER_PASS);
        restAssuredExecutor.execute(this::verifyUnauthorizedUserAccess, UNAUTHORIZED_USER_USERNAME, UNAUTHORIZED_USER_PASS);
    }

    private void verifyAuthorizedUserAccess() {
        // set default parser to enforce restassured json body validations
        // since the response doesn't specify the content type
        given().when()
               .get("/services/ts/sample-roles-decorator/RolesCheck.ts")
               .then()
               .statusCode(200)
               .using()
               .defaultParser(Parser.JSON)
               .body("message", equalTo("Roles Check"))
               .body("user", equalTo(ADMIN_USERNAME));
    }

    private void verifyUnauthorizedUserAccess() {
        given().when()
               .get("/services/ts/sample-roles-decorator/RolesCheck.ts")
               .then()
               .statusCode(500);

        consoleErrorLogAsserter.assertLoggedMessage("Current user [" + UNAUTHORIZED_USER_USERNAME
                + "] is not allowed to call module [RolesCheck]. Required some of roles [ADMINISTRATOR]", Level.ERROR);
    }

}
