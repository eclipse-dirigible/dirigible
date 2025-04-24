/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.projects;

import org.eclipse.dirigible.tests.framework.ide.DirigibleTestTenant;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.util.ProjectUtil;

import java.util.List;

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

}
