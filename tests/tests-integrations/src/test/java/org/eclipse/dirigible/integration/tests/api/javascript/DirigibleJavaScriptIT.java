/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.javascript;

import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;
import org.eclipse.dirigible.tests.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

class DirigibleJavaScriptIT extends IntegrationTest {

    @Autowired
    private DirigibleJavaScriptTestsFactory jsTestFactory;

    @MockBean
    private ProjectStatusProvider projectStatusProvider;

    @TestFactory
    List<DynamicContainer> jsTests() {
        // register all JS tests defined in [src/test/resources/META-INF/dirigible/modules-tests]
        return jsTestFactory.createTestContainers();
    }

    @AfterEach
    final void tearDown() {
        jsTestFactory.close();
    }

}
