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

import java.util.Arrays;

import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Without the tenant provisioning API there is no users callback either. The users registry itself
 * (entities, repository, service) always exists - the Owner-facing endpoints share it - so what is
 * pinned is the endpoint, by name.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApplicationUsersProvisioningApiDisabledIT extends IntegrationTest {

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private ApplicationContext applicationContext;

    /** An administrator, so a 404 says "no such endpoint" rather than "not for you". */
    @Test
    void theUsersCallbackDoesNotAnswer() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/tenant-provisioning/tenants/anything/users")
                                                 .then()
                                                 .statusCode(404));
    }

    @Test
    void theUsersCallbackEndpointIsNotABean() {
        boolean present = Arrays.stream(applicationContext.getBeanDefinitionNames())
                                .map(applicationContext::getType)
                                .anyMatch(type -> type != null && type.getName()
                                                                      .equals("org.eclipse.dirigible.components.tenants.users.ApplicationUserProvisioningEndpoint"));
        assertTrue(!present, "the users callback must not exist without the tenant provisioning API");
    }
}
