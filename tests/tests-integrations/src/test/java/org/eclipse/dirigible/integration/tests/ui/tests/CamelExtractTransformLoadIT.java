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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CamelExtractTransformLoadIT extends UserInterfaceIntegrationTest {

    @Autowired
    private CamelJDBCTestProject jdbcTestProject;

    @Autowired
    private CamelTypescriptTestProject typescriptTestProject;


    @Test
    void testJDBCScenario() {
        jdbcTestProject.copyToWorkspace();
        jdbcTestProject.publish();

        jdbcTestProject.verify();
    }

    @Test
    void testTypeScriptScenario() {
        typescriptTestProject.copyToWorkspace();
        typescriptTestProject.publish();

        typescriptTestProject.verify();
    }
}
