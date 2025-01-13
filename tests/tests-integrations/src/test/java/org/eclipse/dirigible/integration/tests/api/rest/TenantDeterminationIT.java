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

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.eclipse.dirigible.integration.tests.IntegrationTest;
import org.eclipse.dirigible.integration.tests.TenantCreator;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.equalTo;

abstract class TenantDeterminationIT extends IntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private TenantCreator tenantCreator;

    protected void testHealthIsAccessible(String host, String xForwardedHost) {
        restAssuredExecutor.execute(() -> testHealthIsAccessible(xForwardedHost), host);
    }

    protected void testHealthIsAccessible(String xForwardedHost) {
        RequestSpecification requestSpec = RestAssured.given();

        if (null != xForwardedHost) {
            requestSpec = requestSpec.header("x-forwarded-host", xForwardedHost);
        }
        requestSpec.when()
                   .get("/actuator/health")
                   .then()
                   .statusCode(200)
                   .body("status", equalTo("UP"));
    }

    protected void testHealthIsNotAccessible(String host, String xForwardedHost) {
        restAssuredExecutor.execute(() -> testHealthIsNotAccessible(xForwardedHost), host);
    }

    protected void testHealthIsNotAccessible(String xForwardedHost) {
        RequestSpecification requestSpec = RestAssured.given();

        if (null != xForwardedHost) {
            requestSpec = requestSpec.header("x-forwarded-host", xForwardedHost);
        }
        requestSpec.when()
                   .get("/actuator/health")
                   .then()
                   .statusCode(404)
                   .body("status", Matchers.equalTo(404))
                   .body("message", Matchers.equalTo("There is no registered tenant for the current host"));
    }
}
