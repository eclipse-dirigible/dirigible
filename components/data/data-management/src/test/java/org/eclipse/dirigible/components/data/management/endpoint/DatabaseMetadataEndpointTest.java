/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.endpoint;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

/**
 * The Class DatabaseMetadataEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DatabaseMetadataEndpointTest {

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The datasources manager. */
    @Autowired
    private DataSourcesManager datasourcesManager;

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
     * Setup.
     */
    @BeforeEach
    public void setup() {
        DataSource datasource = new DataSource("/test/TestDB.datasource", "TestDB", "", "org.h2.Driver", "jdbc:h2:~/test", "sa", "");
        datasourceRepository.save(datasource);
    }

    /**
     * Gets the data source by name.
     *
     * @return the data source by name
     * @throws Exception the exception
     */
    @Test
    public void getDataSourceByName() throws Exception {

        mockMvc.perform(get("/services/data/metadata/{name}/{schema}/{structure}", "TestDB", "INFORMATION_SCHEMA", "INDEXES"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the data source by name encoded.
     *
     * @return the data source by name
     * @throws Exception the exception
     */
    @Test
    public void getDataSourceByNameEncoded() throws Exception {

        mockMvc.perform(get("/services/data/metadata/{name}/{schema}/{structure}", "TestDB", "INFORMATION_SCHEMA", Base64.getEncoder()
                                                                                                                         .encodeToString(
                                                                                                                                 "INDEXES".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }
}
