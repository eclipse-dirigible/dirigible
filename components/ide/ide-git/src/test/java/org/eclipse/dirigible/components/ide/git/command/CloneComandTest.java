/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.model.GitCloneModel;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Folder;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
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
 * The Class CloneComandTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class CloneComandTest {

    /** The clone command. */
    @Autowired
    private CloneCommand cloneCommand;

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * Creates the workspace test.
     *
     * @throws GitConnectorException the git connector exception
     */
    @Test
    public void createWorkspaceTest() throws GitConnectorException {
        String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
        if (gitEnabled != null) {
            Workspace workspace1 = workspaceService.getWorkspace("workspace1");
            GitCloneModel model = new GitCloneModel();
            model.setRepository("https://github.com/dirigiblelabs/sample_git_test.git");
            model.setBranch(IGitConnector.GIT_MASTER);
            model.setPublish(true);
            cloneCommand.execute(workspace1, model);
            assertNotNull(workspace1);
            assertTrue(workspace1.exists());
            Project project1 = workspace1.getProject("project1");
            assertNotNull(project1);
            assertTrue(project1.exists());
            Folder folder1 = project1.getFolder("folder1");
            assertNotNull(folder1);
            assertTrue(folder1.exists());
            File file1 = folder1.getFile("service1.js");
            assertNotNull(file1);
            assertTrue(file1.exists());
            workspace1.delete();
        }
    }

    /**
     * Creates the workspace test.
     *
     * @throws GitConnectorException the git connector exception
     */
    @Test
    public void createWorkspaceNoGitTest() throws GitConnectorException {
        String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
        if (gitEnabled != null) {
            Workspace workspace1 = workspaceService.getWorkspace("workspace1");
            GitCloneModel model = new GitCloneModel();
            model.setRepository("https://github.com/dirigiblelabs/sample_git_test");
            model.setBranch(IGitConnector.GIT_MASTER);
            model.setPublish(true);
            cloneCommand.execute(workspace1, model);
            assertNotNull(workspace1);
            assertTrue(workspace1.exists());
            Project project1 = workspace1.getProject("project1");
            assertNotNull(project1);
            assertTrue(project1.exists());
            Folder folder1 = project1.getFolder("folder1");
            assertNotNull(folder1);
            assertTrue(folder1.exists());
            File file1 = folder1.getFile("service1.js");
            assertNotNull(file1);
            assertTrue(file1.exists());
            workspace1.delete();
        }
    }

    /**
     * Creates the workspace test.
     *
     * @throws GitConnectorException the git connector exception
     */
    @Test
    public void createWorkspaceNoProjectTest() throws GitConnectorException {
        String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
        if (gitEnabled != null) {
            Workspace workspace1 = workspaceService.getWorkspace("workspace1");
            GitCloneModel model = new GitCloneModel();
            model.setRepository("https://github.com/dirigiblelabs/sample_git_no_project_test.git");
            model.setBranch(IGitConnector.GIT_MASTER);
            model.setPublish(true);
            cloneCommand.execute(workspace1, model);
            assertNotNull(workspace1);
            assertTrue(workspace1.exists());
            List<Project> projects = workspace1.getProjects();
            for (Project project : projects) {
                if (project.getName()
                           .startsWith("sample_git_no_project_test")) {
                    workspace1.delete();
                    return;
                }
            }
            workspace1.delete();
            fail("No project has been created implicitly");
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
