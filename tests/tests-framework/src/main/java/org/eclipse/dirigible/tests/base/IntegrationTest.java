/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.util.PortUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

// enforce spring application cleanup between test method executions
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    // set config to false if you want to disable the headless mode
    // private static final boolean headlessExecution = false;
    private static final boolean headlessExecution = Boolean.parseBoolean(System.getProperty("selenide.headless", Boolean.TRUE.toString()));

    @Autowired
    private TenantCreator tenantCreator;

    public static boolean isHeadlessExecution() {
        return headlessExecution;
    }

    @BeforeAll
    static void useRandomPortForSftp() {
        Configuration.set("DIRIGIBLE_SFTP_PORT", Integer.toString(PortUtil.getFreeRandomPort()));
    }

    @BeforeAll
    static void cleanBeforeTestClassExecution() {
        DirigibleCleaner.deleteDirigibleFolder();
    }

    @AfterAll
    public static void reloadConfigurations() {
        Configuration.reloadConfigurations();
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
                  .pollInterval(3, TimeUnit.SECONDS)
                  .atMost(35, TimeUnit.SECONDS)
                  .until(() -> tenantCreator.isTenantProvisioned(tenant));
    }

}
