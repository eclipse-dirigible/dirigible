/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.TenantCreator;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.equalTo;

abstract class TenantDeterminationIT extends IntegrationTest {

    private static final String CURRENT_TENANT_PATH = "/services/security/tenant-selection/current";

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

    /**
     * The tenant a request runs in, as the shells' header chip reads it: under {@code SUBDOMAIN} it is
     * the host's, the instance says whether it is multitenant at all, and there is never a switch to
     * offer - the groups say nothing about tenants in this mode.
     *
     * @param tenant the tenant whose host and user the request carries
     * @param multitenant whether the instance runs in multitenant mode
     */
    protected void assertCurrentTenant(DirigibleTestTenant tenant, boolean multitenant) {
        restAssuredExecutor.execute(tenant, () -> RestAssured.given()
                                                             .when()
                                                             .get(CURRENT_TENANT_PATH)
                                                             .then()
                                                             .statusCode(200)
                                                             .body("tenant.id", equalTo(tenant.getId()))
                                                             .body("tenant.name", equalTo(tenant.getName()))
                                                             .body("tenant.defaultTenant", equalTo(tenant.isDefaultTenant()))
                                                             .body("multitenant", equalTo(multitenant))
                                                             .body("resolutionStrategy", equalTo("SUBDOMAIN"))
                                                             .body("switchable", equalTo(false))
                                                             .body("tenants", Matchers.empty()));
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
