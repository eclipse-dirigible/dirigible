/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;

/**
 * A deployment that did not ask for the tenant provisioning API does not have it.
 *
 * <p>
 * The distinction being pinned is between absent and merely refusing. The API accepts database
 * credentials over HTTP and can move a tenant into service, so the default has to be that none of
 * it is there - no endpoint answering, no bean holding the logic. Nothing here switches anything
 * on; the default is the whole point.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantProvisioningApiDisabledIT extends IntegrationTest {

    private static final String[] PATHS = { //
            "/services/tenant-provisioning/tenants/anything", //
            "/services/tenant-provisioning/tenants/anything/activation"};

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    /** An administrator, so a 404 says "no such endpoint" rather than "not for you". */
    @Test
    void noEndpointAnswersUnderTheProvisioningPrefix() {
        restAssuredExecutor.execute(() -> {
            for (String path : PATHS) {
                given().when()
                       .get(path)
                       .then()
                       .statusCode(404);
            }
            given().contentType(ContentType.JSON)
                   .body(Map.of("name", "Nothing"))
                   .when()
                   .put(PATHS[0])
                   .then()
                   .statusCode(404);
        });
    }

    @Test
    void noneOfTheProvisioningBeansExist() {
        String[] beans = applicationContext.getBeanDefinitionNames();
        for (String bean : beans) {
            Class<?> type = applicationContext.getType(bean);
            assertTrue(type == null || !type.getPackageName()
                                            .equals("org.eclipse.dirigible.components.tenants.provisioning.external"),
                    "the disabled tenant provisioning API must contribute no beans, found: " + bean);
        }
    }
}
