/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(Lifecycle.PER_CLASS)
public class PlatformSuiteTest {

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext wac;

    @Test
    public void executePlatformTest() throws Exception {
        javascriptService.handleRequest("platform-tests", "engines-get-types.js", null, null, false);
        javascriptService.handleRequest("platform-tests", "lifecycle-publish-project.js", null, null, false);
        javascriptService.handleRequest("platform-tests", "repository-create-file.js", null, null, false);
        javascriptService.handleRequest("platform-tests", "workspace-create-workspace.js", null, null, false);
        javascriptService.handleRequest("platform-tests", "command-execute.js", null, null, false);
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}
