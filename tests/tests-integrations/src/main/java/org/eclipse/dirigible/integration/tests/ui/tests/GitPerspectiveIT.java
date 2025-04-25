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

import ch.qos.logback.classic.Level;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.GitPerspective;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class GitPerspectiveIT extends UserInterfaceIntegrationTest {
    private LogsAsserter consoleLogAsserter;
    private GitPerspective gitPerspective;
    private Workbench workbench;

    @BeforeEach
    void setUp() {
        this.consoleLogAsserter = new LogsAsserter("app.out", Level.INFO);
    }

    @Test
    void testGitFunctionality() {
        this.gitPerspective = ide.openGitPerspective();

        gitPerspective.cloneRepository("https://github.com/dirigiblelabs/test_sample_git");

        this.workbench = ide.openWorkbench();
        workbench.publishAll(true);

        assertWorkingProject();
    }

    void assertWorkingProject() {
        await().atMost(5, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> consoleLogAsserter.containsMessage("GIT-PERSPECTIVE-VALIDATION-OK", Level.INFO));
    }
}
