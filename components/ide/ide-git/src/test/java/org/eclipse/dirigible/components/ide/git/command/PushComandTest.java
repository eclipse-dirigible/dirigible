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

import java.util.Arrays;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.model.GitCloneModel;
import org.eclipse.dirigible.components.ide.git.model.GitPushModel;
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
 * The Class PushComandTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
public class PushComandTest {

    /** The Constant DIRIGIBLE_TEST_GIT_EMAIL. */
    private static final String DIRIGIBLE_TEST_GIT_EMAIL = "DIRIGIBLE_TEST_GIT_EMAIL";

    /** The Constant DIRIGIBLE_TEST_GIT_USERNAME. */
    private static final String DIRIGIBLE_TEST_GIT_USERNAME = "DIRIGIBLE_TEST_GIT_USERNAME";

    /** The Constant DIRIGIBLE_TEST_GIT_PASSWORD. */
    private static final String DIRIGIBLE_TEST_GIT_PASSWORD = "DIRIGIBLE_TEST_GIT_PASSWORD";

    /** The clone command. */
    @Autowired
    private CloneCommand cloneCommand;

    /** The push command. */
    @Autowired
    private PushCommand pushCommand;

    /** The workspaces service. */
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
            GitCloneModel cloneModel = new GitCloneModel();
            cloneModel.setRepository("https://github.com/dirigiblelabs/sample_git_test.git");
            cloneModel.setBranch(IGitConnector.GIT_MASTER);
            cloneModel.setPublish(true);
            cloneCommand.execute(workspace1, cloneModel);
            assertNotNull(workspace1);
            assertTrue(workspace1.exists());
            Project project1 = workspace1.getProject("project1");
            assertNotNull(project1);
            assertTrue(project1.exists());
            String username = System.getProperty(DIRIGIBLE_TEST_GIT_USERNAME);
            String password = System.getProperty(DIRIGIBLE_TEST_GIT_PASSWORD);
            String email = System.getProperty(DIRIGIBLE_TEST_GIT_EMAIL);
            if (username != null && password != null && email != null) {
                GitPushModel pushModel = new GitPushModel();
                pushModel.setProjects(Arrays.asList("sample_git_test"));
                pushModel.setUsername(username);
                pushModel.setPassword(password);
                pushModel.setEmail(email);
                pushModel.setAutoAdd(true);
                pushModel.setAutoCommit(true);
                pushCommand.execute(workspace1, pushModel);
            }
        }
    }

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

}
