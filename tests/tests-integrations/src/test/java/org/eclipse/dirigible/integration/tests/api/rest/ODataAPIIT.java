/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.rest;

import io.restassured.http.ContentType;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.integration.tests.IntegrationTest;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;

class ODataAPIIT extends IntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void testODataMetadata() throws Exception {
        String user = DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();
        String pass = DirigibleConfig.BASIC_ADMIN_PASS.getFromBase64Value();
        restAssuredExecutor.execute( //
                () -> given().when()
                             .get("/odata/v2/$metadata")
                             .then()
                             .statusCode(200)
                             .contentType(ContentType.XML),
                user, pass);
    }
}
