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

import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;

import java.util.List;
import java.util.function.Consumer;

public abstract class BaseMultitenantTestProject extends BaseTestProject implements TestProject, MultitenantTestProject {

    protected BaseMultitenantTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super(projectResourcesFolder, ide, projectUtil, edmView);
    }

    @Override
    public final void test(List<DirigibleTestTenant> tenants) {
        configure();

        tenants.stream()
               .forEach(this::verify);

    }

    @Override
    public final void verify() {
        verify(DirigibleTestTenant.createDefaultTenant());
    }

    protected void wrapVerification(Consumer<DirigibleTestTenant> verification, DirigibleTestTenant tenant, String failedVerification) {
        try {
            verification.accept(tenant);
        } catch (RuntimeException | Error ex) {
            throw new AssertionError("Failed to verify test project [" + getProjectResourcesFolder() + "] for tenant [" + tenant
                    + "]. Failed verification: " + failedVerification, ex);
        }
    }

}
