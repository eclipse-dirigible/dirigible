/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.BeforeAll;
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

/**
 * The Class DatabaseSuiteTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(Lifecycle.PER_CLASS)
public class DatabaseSuiteTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The javascript service. */
    @Autowired
    private JavascriptService javascriptService;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /**
     * Setup.
     */
    @BeforeAll
    public void setup() {
        DataSource datasource = new DataSource("/test/DefaultDB.datasource", "DefaultDB", "", "org.h2.Driver", "jdbc:h2:~/test", "sa", "");
        datasourceRepository.save(datasource);
    }

    /**
     * Execute database test.
     *
     * @throws Exception the exception
     */
    @Test
    public void executeDatabaseTest() throws Exception {
        javascriptService.handleRequest("db-tests", "database-get-connection.js", null, null, false);
        javascriptService.handleRequest("db-tests", "database-get-datasources.js", null, null, false);
        javascriptService.handleRequest("db-tests", "database-get-metadata.js", null, null, false);
    }

    /**
     * Execute query test.
     *
     * @throws Exception the exception
     */
    @Test
    public void executeQueryTest() throws Exception {
        javascriptService.handleRequest("db-tests", "query-execute.js", null, null, false);
    }

    /**
     * Execute update test.
     *
     * @throws Exception the exception
     */
    @Test
    public void executeUpdateTest() throws Exception {
        javascriptService.handleRequest("db-tests", "update-execute.js", null, null, false);
    }

    /**
     * Execute sequence test.
     *
     * @throws Exception the exception
     */
    @Test
    public void executeSequenceTest() throws Exception {
        javascriptService.handleRequest("db-tests", "sequence-nextval.js", null, null, false);
    }


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
