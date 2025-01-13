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

import org.eclipse.dirigible.components.ide.workspace.domain.Folder;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
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
 * The Class ProjectTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class ProjectTest {

    /** The workspaces core service. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Creates the folder test.
     */
    @Test
    public void createFolderTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        Folder folder1 = project1.createFolder("Folder1");
        assertNotNull(folder1);
        assertNotNull(folder1.getInternal());
        assertEquals("Folder1", folder1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1.getInternal()
                                                                            .getPath());
        project1.deleteFolder("Folder1");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Creates the folder deep test.
     */
    @Test
    public void createFolderDeepTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        Folder folder1 = project1.createFolder("Folder1/Folder2/Folder3");
        assertNotNull(folder1);
        assertNotNull(folder1.getInternal());
        assertEquals("Folder3", folder1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1/Folder2/Folder3", folder1.getInternal()
                                                                                            .getPath());
        project1.deleteFolder("Folder1");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the folder test.
     *
     * @return the folder test
     */
    @Test
    public void getFolderTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        Folder folder1 = project1.createFolder("Folder1");
        assertNotNull(folder1);
        assertNotNull(folder1.getInternal());
        assertEquals("Folder1", folder1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1.getInternal()
                                                                            .getPath());

        Folder folder1_1 = project1.getFolder("Folder1");
        assertNotNull(folder1_1);
        assertNotNull(folder1_1.getInternal());
        assertEquals("Folder1", folder1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1_1.getInternal()
                                                                              .getPath());

        project1.deleteFolder("Folder1");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the folders test.
     *
     * @return the folders test
     */
    @Test
    public void getFoldersTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        project1.createFolder("Folder1");
        project1.createFolder("Folder2");

        List<Folder> folders = project1.getFolders();
        assertNotNull(folders);
        assertEquals(2, folders.size());
        Folder folder3 = folders.get(0);
        assertNotNull(folder3.getInternal());
        if (folder3.getName()
                   .equals("Folder1")) {
            assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder3.getInternal()
                                                                                .getPath());
        } else {
            assertEquals("/users/guest/TestWorkspace1/Project1/Folder2", folder3.getInternal()
                                                                                .getPath());
        }

        project1.deleteFolder("Folder1");
        project1.deleteFolder("Folder2");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Delete folder test.
     */
    @Test
    public void deleteFolderTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        Folder folder1 = project1.createFolder("Folder1");
        assertNotNull(folder1);
        assertNotNull(folder1.getInternal());
        assertEquals("Folder1", folder1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1.getInternal()
                                                                            .getPath());
        project1.deleteFolder("Folder1");
        Folder folder2 = workspace1.getProject("Folder1");
        assertNotNull(folder2);
        assertNotNull(folder2.getInternal());
        assertEquals(false, folder2.exists());
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
