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

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.model.GitPushModel;
import org.eclipse.dirigible.components.ide.git.project.ProjectPropertiesVerifier;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Push the changes of a project from the local repository to remote Git repository.
 */
@Component
public class CommitCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CommitCommand.class);

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new commit command.
     *
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public CommitCommand(ProjectPropertiesVerifier projectPropertiesVerifier) {
        this.projectPropertiesVerifier = projectPropertiesVerifier;
    }

    /**
     * Gets the project properties verifier.
     *
     * @return the project properties verifier
     */
    public ProjectPropertiesVerifier getProjectPropertiesVerifier() {
        return projectPropertiesVerifier;
    }

    /**
     * Execute the Commit command.
     *
     * @param workspace the workspace
     * @param model the git push model
     * @throws GitConnectorException in case of exception
     */
    public void execute(final Workspace workspace, GitPushModel model) throws GitConnectorException {
        if (model.getProjects()
                 .size() == 0) {
            logger.warn("No repository is selected for the Commit action");
        }
        for (String repositoryName : model.getProjects()) {
            if (projectPropertiesVerifier.verify(workspace.getName(), repositoryName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Start committing repository [%s]...", repositoryName));
                }
                commitProjectToGitRepository(workspace, repositoryName, model);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Commit of the repository [%s] finished.", repositoryName));
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("Project [%s] is local only. Select a previously clonned project for Commit operation.",
                            repositoryName));
                }
            }
        }

    }

    /**
     * Commit project to git repository by executing several low level Git commands.
     *
     * @param workspace the workspace
     * @param repositoryName the selected project
     * @param model the git push model
     * @throws GitConnectorException in case of exception
     */
    private void commitProjectToGitRepository(final Workspace workspace, String repositoryName, GitPushModel model)
            throws GitConnectorException {

        String errorMessage = String.format("Error occurred while committing repository [%s]. ", repositoryName);

        try {
            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace.getName(), repositoryName);
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());

            if (model.isAutoAdd()) {
                List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), repositoryName);
                for (String projectName : projects) {
                    gitConnector.add(projectName);
                }
            }
            gitConnector.commit(model.getCommitMessage(), model.getUsername(), model.getEmail(), model.isAutoAdd());
        } catch (IOException | GitAPIException | GitConnectorException e) {
            Throwable rootCause = e.getCause();
            if (rootCause != null) {
                rootCause = rootCause.getCause();
                if (rootCause instanceof UnknownHostException) {
                    errorMessage += " Please check your network, or if proxy settings are set properly";
                } else {
                    errorMessage += " Doublecheck the correctness of the [Username] and/or [Password] or [Git Repository URI]";
                }
            } else {
                errorMessage += " " + e.getMessage();
            }
            if (logger.isErrorEnabled()) {
                logger.error(errorMessage);
            }
            throw new GitConnectorException(errorMessage, e);
        }
    }
}
