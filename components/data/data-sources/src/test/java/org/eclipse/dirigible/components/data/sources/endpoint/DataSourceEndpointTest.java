/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.endpoint;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepositoryTest;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Class DataSourceEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DataSourceEndpointTest {

    /** The entity manager. */
    @Autowired
    private EntityManager entityManager;

    /** The datasource service. */
    @Autowired
    private DataSourceService datasourceService;

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The test data source. */
    private DataSource testDataSource;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /**
     * Setup.
     *
     * @throws Exception the exception
     */
    @BeforeEach
    public void setup() throws Exception {

        cleanup();

        // create test DataSources
        datasourceService.save(
                DataSourceRepositoryTest.createDataSource(datasourceRepository, "/a/b/c/ds1.datasource", "ds1", "description", ""));
        datasourceService.save(
                DataSourceRepositoryTest.createDataSource(datasourceRepository, "/a/b/c/ds2.datasource", "ds2", "description", ""));
        datasourceService.save(
                DataSourceRepositoryTest.createDataSource(datasourceRepository, "/a/b/c/ds3.datasource", "ds3", "description", ""));
        datasourceService.save(
                DataSourceRepositoryTest.createDataSource(datasourceRepository, "/a/b/c/ds4.datasource", "ds4", "description", ""));
        datasourceService.save(
                DataSourceRepositoryTest.createDataSource(datasourceRepository, "/a/b/c/ds5.datasource", "ds5", "description", ""));

        Page<DataSource> datasources = datasourceService.getPages(PageRequest.of(0, BaseEndpoint.DEFAULT_PAGE_SIZE));
        assertNotNull(datasources);
        assertEquals(5L, datasources.getTotalElements());

        testDataSource = datasources.getContent()
                                    .get(0);

        entityManager.refresh(testDataSource);
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        datasourceRepository.deleteAll();
    }

    /**
     * Find all data sources.
     */
    @Test
    public void findAllDataSources() {
        Integer size = 10;
        Integer page = 0;
        Pageable pageable = PageRequest.of(page, size);
        assertNotNull(datasourceService.getPages(pageable));
    }

    /**
     * Gets the data source by id.
     *
     * @return the data source by id
     * @throws Exception the exception
     */
    @Test
    public void getDataSourceById() throws Exception {
        Long id = testDataSource.getId();

        mockMvc.perform(get("/services/data/sources/{id}", id))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the data source by name.
     *
     * @return the data source by name
     * @throws Exception the exception
     */
    @Test
    public void getDataSourceByName() throws Exception {
        String name = testDataSource.getName();

        mockMvc.perform(get("/services/data/sources/search?name={name}", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the pages data sources.
     *
     * @return the pages data sources
     * @throws Exception the exception
     */
    @Test
    public void getPagesDataSources() throws Exception {
        String name = testDataSource.getName();

        mockMvc.perform(get("/services/data/sources/pages", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Gets the all data sources.
     *
     * @return the all data sources
     * @throws Exception the exception
     */
    @Test
    public void getAllDataSources() throws Exception {
        String name = testDataSource.getName();

        mockMvc.perform(get("/services/data/sources", name))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
