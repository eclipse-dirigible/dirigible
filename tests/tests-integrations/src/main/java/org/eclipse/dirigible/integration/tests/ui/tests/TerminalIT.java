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

import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.Terminal;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Import(TerminalTestRestConfig.class)
public class TerminalIT extends UserInterfaceIntegrationTest {

    @LocalServerPort
    private int localServerPort;

    @Test
    void testTerminalWorks() {
        Workbench workbench = ide.openWorkbench();

        Terminal terminal = workbench.openTerminal();

        String testRest = "http://localhost:" + localServerPort + TerminalTestRestConfig.TerminalTestRest.TEST_PATH;
        terminal.enterCommand("wget -qO- " + testRest);

        await().pollInterval(1, TimeUnit.SECONDS)
               .atMost(30, TimeUnit.SECONDS)
               .until(() -> TerminalTestRestConfig.TerminalTestRest.isCalled());
    }

}
