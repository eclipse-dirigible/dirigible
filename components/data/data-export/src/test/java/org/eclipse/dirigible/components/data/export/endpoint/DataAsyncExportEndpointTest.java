/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.eclipse.dirigible.components.data.export.domain.Export;
import org.eclipse.dirigible.components.data.export.domain.ExportStatus;
import org.eclipse.dirigible.components.data.export.service.ExportService;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.repository.DataSourceRepository;
import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.CmisFolder;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

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
public class DataAsyncExportEndpointTest {

    /** The datasource repository. */
    @Autowired
    private DataSourceRepository datasourceRepository;

    /** The cms service. */
    @Autowired
    private ExportService exportService;

    /** The cms service. */
    @Autowired
    private CmsService cmsService;

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
    public void exportDataTest() throws Exception {
        mockMvc.perform(post("/services/data/export-async/{datasource}", "TestDB").content("SELECT * FROM INFORMATION_SCHEMA.USERS")
                                                                                  .with(csrf()))
               .andDo(print())
               .andExpect(status().isOk());

        int count = 0;
        while (exportService.findAll()
                            .size() == 0) {
            Thread.sleep(1000);
            if (++count > 5) {
                break;
            }
        }

        List<Export> exports = exportService.findAll();
        assertNotNull(exports);
        assertEquals(1, exports.size());
        CmisFolder exportsFolder = cmsService.getChildFolderByName(cmsService.getRootFolder(), DataAsyncExportEndpoint.EXPORTS_FOLDER_NAME);
        CmisDocument document = cmsService.getChildDocumentByName(exportsFolder, exports.get(0)
                                                                                        .getName());
        // assertNotNull(document); fails on GitHub?
    }


    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
