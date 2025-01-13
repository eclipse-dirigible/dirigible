/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
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

/**
 * The Class WorkspacesCoreServiceTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class WorkspacesServiceTest {

    /** The workspaces core service. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Creates the workspace test.
     */
    @Test
    public void createWorkspaceTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        assertNotNull(workspace1);
        assertNotNull(workspace1.getInternal());
        assertEquals("TestWorkspace1", workspace1.getName());
        assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                              .getPath());
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the workspace test.
     *
     * @return the workspace test
     */
    @Test
    public void getWorkspaceTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        assertNotNull(workspace1);
        assertNotNull(workspace1.getInternal());
        assertEquals("TestWorkspace1", workspace1.getName());
        assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                              .getPath());
        Workspace workspace = workspaceService.getWorkspace("TestWorkspace1");
        assertNotNull(workspace);
        assertNotNull(workspace.getInternal());
        assertEquals("TestWorkspace1", workspace.getName());
        assertEquals("/users/guest/TestWorkspace1", workspace.getInternal()
                                                             .getPath());
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the workspaces test.
     *
     * @return the workspaces test
     */
    @Test
    public void getWorkspacesTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Workspace workspace2 = workspaceService.createWorkspace("TestWorkspace2");
        List<Workspace> workspaces = workspaceService.getWorkspaces();
        assertNotNull(workspaces);
        // assertEquals(2, workspaces.size());
        Workspace worskapce3 = workspaces.get(0);
        assertNotNull(worskapce3.getInternal());
        if (worskapce3.getName()
                      .equals("TestWorkspace1")) {
            assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                                  .getPath());
        } else {
            assertEquals("/users/guest/TestWorkspace2", workspace2.getInternal()
                                                                  .getPath());
        }
        workspaceService.deleteWorkspace("TestWorkspace1");
        workspaceService.deleteWorkspace("TestWorkspace2");
    }

    /**
     * Delete workspace test.
     */
    @Test
    public void deleteWorkspaceTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        assertNotNull(workspace1);
        assertNotNull(workspace1.getInternal());
        assertEquals("TestWorkspace1", workspace1.getName());
        assertEquals("/users/guest/TestWorkspace1", workspace1.getInternal()
                                                              .getPath());
        workspaceService.deleteWorkspace("TestWorkspace1");
        Workspace workspace2 = workspaceService.getWorkspace("TestWorkspace1");
        assertNotNull(workspace2);
        assertNotNull(workspace2.getInternal());
        assertEquals(false, workspace2.exists());
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
