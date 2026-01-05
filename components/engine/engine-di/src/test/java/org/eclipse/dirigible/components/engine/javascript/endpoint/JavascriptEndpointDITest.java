/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.javascript.endpoint;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.dirigible.components.data.sources.config.DataSourceConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Class JavascriptEndpointTest.
 */
@WithMockUser
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataSourceConfig.class}, loader = AnnotationConfigContextLoader.class)
@SpringBootTest
@AutoConfigureMockMvc
public class JavascriptEndpointDITest {

    /** The javascript service. */
    @Autowired
    private JavascriptService javascriptService;

    /** The mock mvc. */
    // @Autowired
    // private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /** The repository. */
    @Autowired
    private IRepository repository;

    @Autowired
    private DataSourcesManager datasourcesManager;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        // create test javascript service
        repository.createResource("/registry/public/test/hello-world.js",
                "org.eclipse.dirigible.components.base.http.access.UserResponseVerifier.getResponse().getOutputStream().println(\"Hello World!\");".getBytes());
        repository.createResource("/registry/public/test1/test2/hello-world.js",
                "org.eclipse.dirigible.components.base.http.access.UserResponseVerifier.getResponse().getOutputStream().println(\"Hello World!\");".getBytes());
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {

        // delete test javascript service
        repository.removeResource("/registry/public/test/hello-world.js");
        repository.removeResource("/registry/public/test1/test2/hello-world.js");
    }

    // @Test
    // public void handleRequest() {
    // assertNotNull(javascriptService.handleRequest("test", "hello-world.js", null, null, false));
    // }

    // /**
    // * Gets the status.
    // *
    // * @return the status
    // * @throws Exception the exception
    // */
    // @Test
    // public void getStatus() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test/hello-world.js"))
    // .andDo(print())
    // .andExpect(status().is2xxSuccessful());
    //
    // }
    //
    // /**
    // * Gets the status deep.
    // *
    // * @return the status deep
    // * @throws Exception the exception
    // */
    // @Test
    // public void getStatusDeep() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test1/test2/hello-world.js"))
    // .andDo(print())
    // .andExpect(status().is2xxSuccessful());
    //
    // }
    //
    // /**
    // * Gets the status deep query.
    // *
    // * @return the status deep query
    // * @throws Exception the exception
    // */
    // @Test
    // public void getStatusDeepQuery() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test1/test2/hello-world.js?a=1&b=2"))
    // .andDo(print())
    // .andExpect(status().is2xxSuccessful());
    //
    // }
    //
    // /**
    // * Gets the result.
    // *
    // * @return the result
    // * @throws Exception the exception
    // */
    // @Test
    // public void getResult() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test/hello-world.js"))
    // .andDo(print())
    // .andExpect(content().string(containsString("Hello World!")));
    //
    // }
    //
    // /**
    // * Gets the result deep.
    // *
    // * @return the result deep
    // * @throws Exception the exception
    // */
    // @Test
    // public void getResultDeep() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test1/test2/hello-world.js"))
    // .andDo(print())
    // .andExpect(content().string(containsString("Hello World!")));
    //
    // }
    //
    // /**
    // * Gets the result deep query.
    // *
    // * @return the result deep query
    // * @throws Exception the exception
    // */
    // @Test
    // public void getResultDeepQuery() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test1/test2/hello-world.js?a=1&b=2"))
    // .andDo(print())
    // .andExpect(content().string(containsString("Hello World!")));
    //
    // }
    //
    // /**
    // * Gets the result deep path.
    // *
    // * @return the result deep path
    // * @throws Exception the exception
    // */
    // @Test
    // public void getResultDeepPath() throws Exception {
    //
    // mockMvc.perform(get("/services/js/test1/test2/hello-world.js/1/2"))
    // .andDo(print())
    // .andExpect(content().string(containsString("Hello World!")));
    //
    // }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
