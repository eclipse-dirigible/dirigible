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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.ide.workspace.domain.File;
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
 * The Class WorkspaceTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class WorkspaceTest {

    /** The workspaces core service. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Creates the project test.
     */
    @Test
    public void createProjectTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());

        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the project test.
     *
     * @return the project test
     */
    @Test
    public void getProjectTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());

        Project project1_1 = workspace1.getProject("Project1");
        assertNotNull(project1_1);
        assertNotNull(project1_1.getInternal());
        assertEquals("Project1", project1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1_1.getInternal()
                                                                       .getPath());

        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the projects test.
     *
     * @return the projects test
     */
    @Test
    public void getProjectsTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        workspace1.createProject("Project2");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());

        List<Project> projects = workspace1.getProjects();
        assertNotNull(projects);
        assertEquals(2, projects.size());
        Project project3 = projects.get(0);
        assertNotNull(project3.getInternal());
        if (project3.getName()
                    .equals("Project1")) {
            assertEquals("/users/guest/TestWorkspace1/Project1", project3.getInternal()
                                                                         .getPath());
        } else {
            assertEquals("/users/guest/TestWorkspace1/Project2", project3.getInternal()
                                                                         .getPath());
        }

        workspace1.deleteProject("Project1");
        workspace1.deleteProject("Project2");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Delete project test.
     */
    @Test
    public void deleteProjectTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());
        assertTrue(project1.exists());
        workspace1.deleteProject("Project1");
        Project project2 = workspace1.getProject("Project1");
        assertNotNull(project2);
        assertNotNull(project2.getInternal());
        assertFalse(project2.exists());

        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Copy project test.
     */
    @Test
    public void copyProjectTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        project1.createFolder("Folder1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());

        workspace1.copyProject("Project1", "Project2");

        Project project1_1 = workspace1.getProject("Project1");
        assertNotNull(project1_1);
        assertNotNull(project1_1.getInternal());
        assertEquals("Project1", project1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1_1.getInternal()
                                                                       .getPath());
        assertTrue(project1_1.exists());
        Folder folder1_1 = project1_1.getFolder("Folder1");
        assertNotNull(folder1_1);
        assertNotNull(folder1_1.getInternal());
        assertEquals("Folder1", folder1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1_1.getInternal()
                                                                              .getPath());
        assertTrue(folder1_1.exists());

        Project project1_2 = workspace1.getProject("Project2");
        assertNotNull(project1_2);
        assertNotNull(project1_2.getInternal());
        assertEquals("Project2", project1_2.getName());
        assertEquals("/users/guest/TestWorkspace1/Project2", project1_2.getInternal()
                                                                       .getPath());
        assertTrue(project1_2.exists());
        Folder folder1_2 = project1_2.getFolder("Folder1");
        assertNotNull(folder1_2);
        assertNotNull(folder1_2.getInternal());
        assertEquals("Folder1", folder1_2.getName());
        assertEquals("/users/guest/TestWorkspace1/Project2/Folder1", folder1_2.getInternal()
                                                                              .getPath());
        assertTrue(folder1_2.exists());

        workspace1.deleteProject("Project1");
        workspace1.deleteProject("Project2");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Move project test.
     */
    @Test
    public void moveProjectTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        project1.createFolder("Folder1");
        assertNotNull(project1);
        assertNotNull(project1.getInternal());
        assertEquals("Project1", project1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1.getInternal()
                                                                     .getPath());

        workspace1.moveProject("Project1", "Project2");

        Project project1_1 = workspace1.getProject("Project1");
        assertNotNull(project1_1);
        assertNotNull(project1_1.getInternal());
        assertEquals("Project1", project1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1", project1_1.getInternal()
                                                                       .getPath());
        Folder folder1_1 = project1_1.getFolder("Folder1");
        assertNotNull(folder1_1);
        assertNotNull(folder1_1.getInternal());
        assertEquals("Folder1", folder1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1_1.getInternal()
                                                                              .getPath());
        assertFalse(folder1_1.exists());

        Project project1_2 = workspace1.getProject("Project2");
        assertNotNull(project1_2);
        assertNotNull(project1_2.getInternal());
        assertEquals("Project2", project1_2.getName());
        assertEquals("/users/guest/TestWorkspace1/Project2", project1_2.getInternal()
                                                                       .getPath());
        Folder folder1_2 = project1_2.getFolder("Folder1");
        assertNotNull(folder1_2);
        assertNotNull(folder1_2.getInternal());
        assertEquals("Folder1", folder1_2.getName());
        assertEquals("/users/guest/TestWorkspace1/Project2/Folder1", folder1_2.getInternal()
                                                                              .getPath());
        assertTrue(folder1_2.exists());

        workspace1.deleteProject("Project2");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

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
        Folder folder1 = project1.createFolder("Folder1");
        project1.createFolder("Folder2");
        assertNotNull(folder1);
        assertNotNull(folder1.getInternal());
        assertEquals("Folder1", folder1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/Folder1", folder1.getInternal()
                                                                            .getPath());

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
        assertTrue(folder1.exists());
        project1.deleteFolder("Folder1");
        Folder folder2 = project1.getFolder("Folder1");
        assertNotNull(folder2);
        assertNotNull(folder2.getInternal());
        assertFalse(folder2.exists());

        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Creates the file test.
     */
    @Test
    public void createFileTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        File file1 = project1.createFile("File1.txt", "test".getBytes());
        assertNotNull(file1);
        assertNotNull(file1.getInternal());
        assertEquals("File1.txt", file1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file1.getInternal()
                                                                            .getPath());

        project1.deleteFile("File1.txt");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the file test.
     *
     * @return the file test
     */
    @Test
    public void getFileTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        File file1 = project1.createFile("File1.txt", "test".getBytes());
        assertNotNull(file1);
        assertNotNull(file1.getInternal());
        assertEquals("File1.txt", file1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file1.getInternal()
                                                                            .getPath());

        File file1_1 = project1.getFile("File1.txt");
        assertNotNull(file1_1);
        assertNotNull(file1_1.getInternal());
        assertEquals("File1.txt", file1_1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file1_1.getInternal()
                                                                              .getPath());
        assertTrue("test".equals(new String(file1_1.getContent())));

        project1.deleteFile("File1.txt");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Gets the files test.
     *
     * @return the files test
     */
    @Test
    public void getFilesTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        File file1 = project1.createFile("File1.txt", "test".getBytes());
        project1.createFile("File2.txt", "test".getBytes());
        assertNotNull(file1);
        assertNotNull(file1.getInternal());
        assertEquals("File1.txt", file1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file1.getInternal()
                                                                            .getPath());

        List<File> files = project1.getFiles();
        assertNotNull(files);
        assertEquals(2, files.size());
        File file3 = files.get(0);
        assertNotNull(file3.getInternal());
        if (file3.getName()
                 .equals("File1.txt")) {
            assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file3.getInternal()
                                                                                .getPath());
        } else {
            assertEquals("/users/guest/TestWorkspace1/Project1/File2.txt", file3.getInternal()
                                                                                .getPath());
        }

        project1.deleteFile("File1.txt");
        project1.deleteFile("File2.txt");
        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * Delete file test.
     */
    @Test
    public void deleteFileTest() {
        Workspace workspace1 = workspaceService.createWorkspace("TestWorkspace1");
        Project project1 = workspace1.createProject("Project1");
        File file1 = project1.createFile("File1.txt", "test".getBytes());
        assertNotNull(file1);
        assertNotNull(file1.getInternal());
        assertEquals("File1.txt", file1.getName());
        assertEquals("/users/guest/TestWorkspace1/Project1/File1.txt", file1.getInternal()
                                                                            .getPath());
        assertTrue(file1.exists());
        project1.deleteFile("File1.txt");
        File file2 = project1.getFile("File1.txt");
        assertNotNull(file2);
        assertNotNull(file2.getInternal());
        assertFalse(file2.exists());

        workspace1.deleteProject("Project1");
        workspaceService.deleteWorkspace("TestWorkspace1");
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
