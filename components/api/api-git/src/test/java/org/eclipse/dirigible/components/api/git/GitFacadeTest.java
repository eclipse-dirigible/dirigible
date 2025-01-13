/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.components.api.platform.WorkspaceFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.ide.git.domain.GitCommitInfo;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.json.ProjectDescriptor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * The Class GitFacadeTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(Lifecycle.PER_CLASS)
public class GitFacadeTest {

    /** The username. */
    private String username = "dirigible";

    /** The email. */
    private String email = "dirigible@eclipse.com";

    /** The project name. */
    private String projectName = "project1";

    /** The workspace name. */
    private String workspaceName = "workspace";

    /** The repository. */
    private String repository = "project1-repo";

    /** The gson. */
    private final Gson gson = new Gson();

    /**
     * Test init repository and commit.
     *
     * @throws GitAPIException the git API exception
     * @throws GitConnectorException the git connector exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testInitRepositoryAndCommit() throws GitAPIException, GitConnectorException, IOException {
        String user = UserFacade.getName();
        File tempGitDirectory = GitFileUtils.getGitDirectory(user, workspaceName, repository);
        boolean isExistingGitRepository = tempGitDirectory != null;
        if (isExistingGitRepository) {
            GitFacade.deleteRepository(workspaceName, repository);
        }

        Workspace workspace = WorkspaceFacade.createWorkspace(workspaceName);
        Project project = workspace.createProject(projectName);
        project.createFile("www/oldFile", "str".getBytes(StandardCharsets.UTF_8));
        GitFacade.initRepository(user, email, workspaceName, projectName, repository, "Initial commit");
        List<ProjectDescriptor> repos = GitFacade.getGitRepositories(workspaceName);
        assertTrue(repos.size() == 1);

        project.createFile("www/newFile", "str".getBytes(StandardCharsets.UTF_8));
        String message = "Second commit";
        GitFacade.commit(username, email, workspaceName, repository, message, true);
        List<GitCommitInfo> history = GitFacade.getHistory(repository, workspaceName, projectName);
        assertTrue(history.size() == 2);
        assertTrue(history.get(0)
                          .getMessage()
                          .equals(message));
        assertProjectJsonExists(project);

        GitFacade.deleteRepository(workspaceName, repository);
        assertTrue(GitFileUtils.getGitDirectory(user, workspaceName, repository) == null);

        FileUtils.deleteDirectory(new File("./target/.git"));
    }

    /**
     * Assert project json exists.
     *
     * @param project the project
     */
    private void assertProjectJsonExists(Project project) {
        org.eclipse.dirigible.components.ide.workspace.domain.File maybeProjectJson = project.getFile("project.json");
        assertTrue(maybeProjectJson.exists());

        String projectJsonContent = new String(maybeProjectJson.getContent(), StandardCharsets.UTF_8);
        Map<String, String> parsedProjectJsonContent = gson.fromJson(projectJsonContent, new TypeToken<Map<String, String>>() {}.getType());

        assertEquals(project.getName(), parsedProjectJsonContent.get("guid"));
        assertEquals(1, parsedProjectJsonContent.size());
    }

    @SpringBootApplication
    static class TestConfiguration {
    }

}

