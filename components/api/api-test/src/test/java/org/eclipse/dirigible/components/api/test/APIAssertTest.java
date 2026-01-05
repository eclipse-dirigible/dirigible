/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.test;

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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
public class APIAssertTest {

    @Autowired
    protected WebApplicationContext wac;
    @Autowired
    private JavascriptService javascriptService;
    @Autowired
    private MockMvc mockMvc;

    // @Autowired
    // private FilterChainProxy springSecurityFilterChain;
    //
    // @Autowired
    // private IRepository repository;


    @SpringBootApplication
    static class TestConfiguration {
    }

    @Test
    public void successful() throws Exception {
        // javascriptService.handleRequest("test", "successful.js", null, null, false);

        mockMvc.perform(get("/services/js/test/successful.js"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void failed() throws Exception {
        try {
            javascriptService.handleRequest("test", "failed.js", null, null, false);

            // mockMvc.perform(get("/services/js/test/failed.js"))
            // .andDo(print())
            // .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // successfully failed
            assertNotNull(e.getCause());
            assertTrue(e.getCause()
                        .getMessage()
                        .endsWith("Assertion 'assertTrue' failed"));
        }
    }
}
