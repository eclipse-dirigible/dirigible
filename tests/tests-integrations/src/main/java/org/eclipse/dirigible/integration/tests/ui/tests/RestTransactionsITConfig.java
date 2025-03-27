/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestConfiguration
class RestTransactionsITConfig {

    @RestController
    static class TestRest {
        static final String TEST_PATH = "/services/core/version/rest/api/transactions";

        static final String TEST_USERNAME = "test-user";
        static final String TEST_PASSWORD = "test-password";

        private final UserService userService;
        private final TenantContext tenantContext;

        TestRest(UserService userService, TenantContext tenantContext) {
            this.userService = userService;
            this.tenantContext = tenantContext;
        }

        @Transactional
        @GetMapping(TEST_PATH)
        String testTransactions() {
            userService.createNewUser(TEST_USERNAME, TEST_PASSWORD, tenantContext.getCurrentTenant()
                                                                                 .getId());
            throw new IllegalStateException("Intentionally throw an exception to test REST transactional behaviour.");
        }
    }
}
