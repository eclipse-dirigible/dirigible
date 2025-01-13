/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.DirigibleApplication;
import org.eclipse.dirigible.tests.DirigibleCleaner;
import org.eclipse.dirigible.tests.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = DirigibleApplication.class)
public abstract class IntegrationTest {

    @Autowired
    private TenantCreator tenantCreator;

    @Autowired
    private DirigibleCleaner dirigibleCleaner;

    @AfterEach
    final void cleanUp() {
        dirigibleCleaner.clean();
    }

    protected void createTenants(DirigibleTestTenant... tenants) {
        createTenants(Arrays.asList(tenants));
    }

    protected void createTenants(List<DirigibleTestTenant> tenants) {
        tenants.forEach(tenantCreator::createTenant);
    }

    protected void waitForTenantsProvisioning(List<DirigibleTestTenant> tenants) {
        tenants.forEach(this::waitForTenantProvisioning);
    }

    protected void waitForTenantProvisioning(DirigibleTestTenant tenant) {
        Awaitility.await()
                  .atMost(35, TimeUnit.SECONDS)
                  .until(() -> tenantCreator.isTenantProvisioned(tenant));
    }

}
