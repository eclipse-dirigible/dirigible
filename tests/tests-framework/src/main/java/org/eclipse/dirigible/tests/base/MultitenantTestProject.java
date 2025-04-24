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

import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;

import java.util.List;

public interface MultitenantTestProject {

    /**
     * Execute all the needed steps to configure and verify the project to assert that it works properly
     * for the provided tenants.
     *
     * @param tenants
     */
    void test(List<DirigibleTestTenant> tenants);

    /**
     * Verify test project is working for the provided tenant
     *
     * @param tenant
     */
    void verify(DirigibleTestTenant tenant);

}
