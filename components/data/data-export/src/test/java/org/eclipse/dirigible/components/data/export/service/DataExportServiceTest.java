/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import java.util.List;

import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Class DataExportServiceTest.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration
@WebAppConfiguration
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@Transactional
public class DataExportServiceTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The data export service. */
    @Autowired
    private DataExportService dataExportService;

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The wac. */
    @Autowired
    protected WebApplicationContext wac;

    /** The spring security filter chain. */
    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        DataSource datasource = new DataSource("/test/TestDB.datasource", "TestDB", "", "org.h2.Driver", "jdbc:h2:~/test", "sa", "");
        datasourceRepository.save(datasource);
    }


    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanUp() {
        datasourceRepository.deleteAll();
    }

    /**
     * Export metadata as project test.
     *
     * @throws SQLException the SQL exception
     */
    @Test
    public void exportMetadataAsProjectTest() throws SQLException {
        String filePath = dataExportService.exportMetadataAsProject("TestDB", "INFORMATION_SCHEMA");
        Workspace workspace = workspaceService.getWorkspace("workspace");
        Project project = workspace.getProject("TestDB");
        List<File> files = project.getFiles();
        File foundFile = null;

        for (File file : files) {
            if (file.getWorkspacePath()
                    .equals(filePath)) {
                foundFile = file;
                break;
            }
        }

        assertNotNull(foundFile);
        assertEquals(foundFile.getWorkspacePath(), filePath);
    }

    /**
     * Export schema as model test.
     */
    @Test
    public void exportSchemaAsModelTest() {
        dataExportService.exportSchemaAsModel("TestDB", "INFORMATION_SCHEMA");
        Workspace workspace = workspaceService.getWorkspace("workspace");
        Project project = workspace.getProject("TestDB");
        List<File> files = project.getFiles();
        File foundFile = null;

        for (File file : files) {
            if (file.getName()
                    .equals("testdb_information_schema.model")) {
                foundFile = file;
                break;
            }
        }

        assertNotNull(foundFile);
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
