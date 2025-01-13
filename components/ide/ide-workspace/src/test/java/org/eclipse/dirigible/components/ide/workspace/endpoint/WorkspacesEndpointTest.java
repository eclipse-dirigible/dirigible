/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * The Class WorkspacesEndpointTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class WorkspacesEndpointTest {

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

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
    }

    /**
     * Cleanup.
     *
     * @throws Exception the exception
     */
    @AfterEach
    public void cleanup() throws Exception {
        mockMvc.perform(delete("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print());
    }

    /**
     * Gets the all workspaces.
     *
     * @return the all workspaces
     * @throws Exception the exception
     */
    @Test
    public void getAllWorkspaces() throws Exception {
        mockMvc.perform(get("/services/ide/workspaces"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Creates the get delete workspace.
     *
     * @throws Exception the exception
     */
    @Test
    public void createGetDeleteWorkspace() throws Exception {
        mockMvc.perform(post("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        Workspace workspace = workspaceService.getWorkspace("workspace1");
        assertNotNull(workspace);
        assertEquals("workspace1", workspace.getName());
        assertTrue(workspace.exists());
        mockMvc.perform(get("/services/ide/workspaces/workspace1"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Creates the get delete project.
     *
     * @throws Exception the exception
     */
    @Test
    public void createGetDeleteProject() throws Exception {
        mockMvc.perform(post("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/services/ide/workspaces/workspace1/project1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        Workspace workspace = workspaceService.getWorkspace("workspace1");
        assertNotNull(workspace);
        Project project = workspace.getProject("project1");
        assertNotNull(project);
        assertEquals("project1", project.getName());
        assertTrue(project.exists());
        mockMvc.perform(get("/services/ide/workspaces/workspace1/project1"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1/project1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        project = workspace.getProject("project1");
        assertNotNull(project);
        assertFalse(project.exists());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    /**
     * Creates the get delete file.
     *
     * @throws Exception the exception
     */
    @Test
    public void createGetDeleteFile() throws Exception {
        mockMvc.perform(post("/services/ide/workspaces/workspace1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/services/ide/workspaces/workspace1/project1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/services/ide/workspaces/workspace1/project1/file1.txt").content("test1".getBytes())
                                                                                      .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                                                      .with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        Workspace workspace = workspaceService.getWorkspace("workspace1");
        assertNotNull(workspace);
        Project project = workspace.getProject("project1");
        assertNotNull(project);
        File file = project.getFile("file1.txt");
        assertNotNull(file);
        assertEquals("file1.txt", file.getName());
        assertTrue(file.exists());
        mockMvc.perform(get("/services/ide/workspaces", "workspace1", "project1", "file1.txt").header("describe", "application/json"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1/project1/file1.txt").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        file = project.getFile("file1.txt");
        assertNotNull(file);
        assertFalse(file.exists());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1/project1").with(csrf()))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(delete("/services/ide/workspaces/workspace1").with(csrf()))
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
