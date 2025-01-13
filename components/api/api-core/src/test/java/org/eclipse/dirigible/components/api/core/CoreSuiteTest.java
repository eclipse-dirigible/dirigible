/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.Disabled;
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
public class CoreSuiteTest {

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext wac;

    @Test
    public void executeConfigurationGetTest() throws Exception {
        javascriptService.handleRequest("core-tests", "configurations-get-test.js", null, null, false);
    }

    @Test
    public void executeContextGetTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/context-get-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeEnvGetTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/env-get-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeEnvListTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/env-list-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeGlobalsGetTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/globals-get-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeGlobalsListTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/globals-list-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Disabled
    @Test
    public void executeDestinationsGetTest() throws Exception {
        mockMvc.perform(get("/services/js/core-tests/destinations-get-test.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}
