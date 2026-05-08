/*
 * Copyright (c) 2022 codbex or an codbex affiliate company and contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 codbex or an codbex affiliate company and contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests.sample;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.GitPerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

abstract class SampleProjectRepositoryIT extends UserInterfaceIntegrationTest {

    @Autowired
    protected RestAssuredExecutor restAssuredExecutor;

    @Autowired
    protected SynchronizationProcessor synchronizationProcessor;

    @Test
    final void testSampleProject() {
        cloneProject();

        Workbench workbench = ide.openWorkbench();
        workbench.publishAll(true);

        synchronizationProcessor.forceProcessSynchronizers();

        verifyProject();
    }

    protected abstract void verifyProject();

    private void cloneProject() {
        ide.openHomePage();

        GitPerspective gitPerspective = ide.openGitPerspective();
        String repositoryUrl = getRepositoryURL();
        gitPerspective.cloneRepository(repositoryUrl);
    }

    protected abstract String getRepositoryURL();

}
