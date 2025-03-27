/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.tenants.domain.User;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.tests.DirigibleTestTenant;
import org.eclipse.dirigible.tests.UserInterfaceIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Import(RestTransactionsITConfig.class)
public class RestTransactionsIT extends UserInterfaceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void testTranactionalAnnotationWorksForRESTs() {
        given().get(RestTransactionsITConfig.TestRest.TEST_PATH)
               .then()
               .statusCode(500);

        Optional<User> createdUser = userService.findUserByUsernameAndTenantId(RestTransactionsITConfig.TestRest.TEST_USERNAME,
                DirigibleTestTenant.createDefaultTenant()
                                   .getId());
        assertThat(createdUser).isEmpty();
    }

}
