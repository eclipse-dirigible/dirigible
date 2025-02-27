/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.projects;

import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.Workbench;
import org.eclipse.dirigible.tests.util.ProjectUtil;

public abstract class BaseTestProject implements TestProject {

    private final String projectResourcesFolder;
    private final IDE ide;
    private final ProjectUtil projectUtil;

    protected BaseTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil) {
        this.projectResourcesFolder = projectResourcesFolder;

        this.ide = ide;
        this.projectUtil = projectUtil;
    }

    protected String getProjectResourcesFolder() {
        return projectResourcesFolder;
    }

    @Override
    public final void publish() {
        publish(true);
    }

    @Override
    public final void publish(boolean waitForSynchronizationExecution) {
        Workbench workbench = ide.openWorkbench();
        workbench.publishAll(waitForSynchronizationExecution);
    }

    @Override
    public final void copyToWorkspace() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(projectResourcesFolder);
    }

}
