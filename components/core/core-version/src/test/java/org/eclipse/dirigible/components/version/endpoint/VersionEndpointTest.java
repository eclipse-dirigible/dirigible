/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.version.endpoint;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class VersionEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@Transactional
public class VersionEndpointTest {

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;
    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;
    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

    /**
     * Test get version.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetVersion() throws Exception {
        Configuration.reloadConfigurations();

        // TODO: Update expected value when engines added.
        String json = """
                {
                    "productName": "dirigible",
                    "productVersion": "0.0.1",
                    "productCommitId": "test",
                    "productRepository": "https://github.com/eclipse/dirigible",
                    "productType": "all",
                    "instanceName": "server-spring-boot",
                    "repositoryProvider": "local",
                    "databaseProvider": "local",
                    "engines": []
                }
                """;
        mockMvc.perform(get("/services/core/version"))
               .andExpect(status().isOk())
               .andExpect(content().json(json));
    }


    /**
     * The application a deployment declares travels beside the platform's identity, which stays as it
     * was (#8502).
     *
     * @throws Exception the exception
     */
    @Test
    public void theDeclaredApplicationIsServedBesideThePlatform() throws Exception {
        Configuration.reloadConfigurations();
        Configuration.set("DIRIGIBLE_APPLICATION_NAME", "BusinessIntents");
        Configuration.set("DIRIGIBLE_APPLICATION_VERSION", " 1.38.0 ");
        try {
            mockMvc.perform(get("/services/core/version"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.applicationName").value("BusinessIntents"))
                   .andExpect(jsonPath("$.applicationVersion").value("1.38.0"))
                   .andExpect(jsonPath("$.productName").value("dirigible"))
                   .andExpect(jsonPath("$.productVersion").value("0.0.1"))
                   .andExpect(jsonPath("$.productCommitId").value("test"));
        } finally {
            Configuration.remove("DIRIGIBLE_APPLICATION_NAME");
            Configuration.remove("DIRIGIBLE_APPLICATION_VERSION");
        }
    }

    /**
     * A deployment that declares no application - or a blank one - serves none, and the platform's
     * identity alone.
     *
     * @throws Exception the exception
     */
    @Test
    public void anUndeclaredApplicationIsServedAsNone() throws Exception {
        Configuration.reloadConfigurations();
        Configuration.set("DIRIGIBLE_APPLICATION_VERSION", "  ");
        try {
            mockMvc.perform(get("/services/core/version"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.applicationName").doesNotExist())
                   .andExpect(jsonPath("$.applicationVersion").doesNotExist())
                   .andExpect(jsonPath("$.productVersion").value("0.0.1"));
        } finally {
            Configuration.remove("DIRIGIBLE_APPLICATION_VERSION");
        }
    }

}
