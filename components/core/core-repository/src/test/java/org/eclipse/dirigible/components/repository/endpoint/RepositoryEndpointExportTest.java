/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.repository.endpoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.repository.api.IRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the export representation of the repository endpoint: the {@code export} parameter selects
 * a download without shadowing the plain listing.
 */
@WithMockUser(roles = {"ADMINISTRATOR"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
class RepositoryEndpointExportTest {

    private static final String FOLDER = "/registry/public/export-test";

    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private IRepository repository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void createFolderWithASubfolder() {
        repository.createResource(FOLDER + "/top.txt", CONTENT, false, "text/plain", true);
        repository.createResource(FOLDER + "/nested/deep.txt", CONTENT, false, "text/plain", true);
    }

    @Test
    void exportsACollectionAsAnAttachedArchive() throws Exception {
        mockMvc.perform(get("/services/core/repository" + FOLDER).param("export", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")))
               .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString(".zip")));
    }

    @Test
    void exportsAResourceAsAnAttachedFile() throws Exception {
        mockMvc.perform(get("/services/core/repository" + FOLDER + "/top.txt").param("export", "true"))
               .andExpect(status().isOk())
               .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("top.txt")))
               .andExpect(content().bytes(CONTENT));
    }

    @Test
    void answersNotFoundForAPathThatDoesNotExist() throws Exception {
        mockMvc.perform(get("/services/core/repository" + FOLDER + "/there-is-no-such-file.txt").param("export", "true"))
               .andExpect(status().isNotFound());
    }

    @Test
    void stillListsACollectionWithoutTheExportParameter() throws Exception {
        mockMvc.perform(get("/services/core/repository" + FOLDER))
               .andExpect(status().isOk())
               .andExpect(content().string(Matchers.containsString("top.txt")));
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }
}
