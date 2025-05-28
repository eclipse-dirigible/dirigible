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
import org.eclipse.dirigible.tests.framework.ide.FormView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.Workbench;

public abstract class BaseTestProject implements TestProject {

    private final String projectResourcesFolder;

    private final IDE ide;
    private final ProjectUtil projectUtil;
    private final EdmView edmView;

    protected BaseTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        this.projectResourcesFolder = projectResourcesFolder;

        this.ide = ide;
        this.projectUtil = projectUtil;
        this.edmView = edmView;
    }

    @Override
    public final void test() throws Exception {
        configure();
        verify();
    }

    @Override
    public void configure() {
        copyToWorkspace();
        publish();
        ide.close();
    }

    protected final void publish() {
        publish(true);
    }

    protected final void publish(boolean waitForSynchronizationExecution) {
        Workbench workbench = ide.openWorkbench();
        workbench.publishAll(waitForSynchronizationExecution);
    }

    protected final void copyToWorkspace() {
        projectUtil.copyResourceProjectToDefaultUserWorkspace(projectResourcesFolder);
    }

    protected void generateEDM(String edmFileName) {
        ide.openWorkbench();
        edmView.regenerate(projectResourcesFolder, edmFileName);
    }

    protected IDE getIde() {
        return ide;
    }

    protected EdmView getEdmView() {
        return edmView;
    }

    protected void generateForms(String... formFileNames) {
        Workbench workbench = ide.openWorkbench();
        workbench.expandProject(getProjectResourcesFolder());

        for (String formFileName : formFileNames) {
            workbench.openFile(formFileName);

            FormView formView = workbench.getFormView();
            formView.regenerateForm();
            ide.assertStatusBarMessage("Generated from model '" + formFileName + "'");

        }
    }

    public String getProjectResourcesFolder() {
        return projectResourcesFolder;
    }

    @Override
    public void cleanup() {
        // nothing to cleanup by default
    }
}
