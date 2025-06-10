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

import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BpmnMultitenancyIT extends MultitenancyUserInterfaceIntegrationTest {

    @Autowired
    private BpmnMultitenancyTestProject testProject;

    @Test
    void testEmployeeRegistrationBpmProcessExecutesInTenantIsolation() {
        List<DirigibleTestTenant> tenants = provisionTenants();

        testProject.test(tenants);
    }

}
