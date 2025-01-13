/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.endpoint;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Class DataExportEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class DataExportEndpointTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The mock mvc. */
    @Autowired
    private MockMvc mockMvc;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        DataSource datasource = new DataSource("/test/TestDB.datasource", "TestDB", "", "org.h2.Driver", "jdbc:h2:~/test", "sa", "");
        datasourceRepository.save(datasource);
    }

    /**
     * Export data as project test.
     *
     * @throws Exception the exception
     */
    @Test
    public void exportDataAsProjectTest() throws Exception {
        mockMvc.perform(put("/services/data/project/csv/{datasource}/{schema}", "TestDB", "INFORMATION_SCHEMA").with(csrf()))
               .andDo(print())
               .andExpect(status().isOk());
        Workspace workspace = workspaceService.getWorkspace("workspace");
        assertNotNull(workspace);
        Project project = workspace.getProject("INFORMATION_SCHEMA");
        assertNotNull(project);
    }

    /**
     * Export metadata as project test.
     *
     * @throws Exception the exception
     */
    @Test
    public void exportMetadataAsProjectTest() throws Exception {
        mockMvc.perform(put("/services/data/project/metadata/{datasource}/{schema}", "TestDB", "INFORMATION_SCHEMA").with(csrf()))
               .andDo(print())
               .andExpect(status().isOk());
        Workspace workspace = workspaceService.getWorkspace("workspace");
        assertNotNull(workspace);
        Project project = workspace.getProject("INFORMATION_SCHEMA");
        assertNotNull(project);
    }

    /**
     * Export schema as model test.
     *
     * @throws Exception the exception
     */
    @Test
    public void exportSchemaAsModelTest() throws Exception {
        mockMvc.perform(put("/services/data/project/model/{datasource}/{schema}", "TestDB", "INFORMATION_SCHEMA").with(csrf()))
               .andDo(print())
               .andExpect(status().isOk());
        Workspace workspace = workspaceService.getWorkspace("workspace");
        assertNotNull(workspace);
        Project project = workspace.getProject("INFORMATION_SCHEMA");
        assertNotNull(project);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
