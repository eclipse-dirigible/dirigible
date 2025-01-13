/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.componenets.api.io;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.Test;
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
public class IOSuiteTest {

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext wac;

    @Test
    public void executeFilesTest() throws Exception {
        javascriptService.handleRequest("io-tests", "files-create-temp-file.js", null, null, false);
        javascriptService.handleRequest("io-tests", "files-file-streams.js", null, null, false);
    }

    // @Test
    // public void executeFTPTest() throws Exception {
    // javascriptService.handleRequest("io-tests", "ftp-get-file.js", null, null, false);
    // }

    @Test
    public void executeStreamsTest() throws Exception {
        javascriptService.handleRequest("io-tests", "streams-copy.js", null, null, false);
        javascriptService.handleRequest("io-tests", "streams-text.js", null, null, false);
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}
