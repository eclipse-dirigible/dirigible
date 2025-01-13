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
import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.project.ProjectPropertiesVerifier;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reset the state of the local project, clear local changes.
 */
@Component
public class ResetCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ResetCommand.class);

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new reset command.
     *
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public ResetCommand(ProjectPropertiesVerifier projectPropertiesVerifier) {
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
     * Execute the Reset command.
     *
     * @param workspace the workspace
     * @param repositories the repositories
     * @throws GitConnectorException in case of exception
     */
    public void execute(String workspace, List<String> repositories) throws GitConnectorException {
        if (repositories.size() == 0) {
            if (logger.isWarnEnabled()) {
                logger.warn("No repository is selected for the Reset action");
            }
        }

        for (String repositoryName : repositories) {
            if (projectPropertiesVerifier.verify(workspace, repositoryName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Start reseting repository [%s]...", repositoryName));
                }
                hardReset(workspace, repositoryName);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Reset of the repository [%s] finished.", repositoryName));
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("Project [%s] is local only. Select a previously cloned repository for Reset operation.",
                            repositoryName));
                }
            }
        }

    }

    /**
     * Performing a hard reset low level git commands.
     *
     * @param workspace the workspace
     * @param repositoryName the project
     * @throws GitConnectorException in case of exception
     */
    private void hardReset(String workspace, String repositoryName) throws GitConnectorException {
        String errorMessage = String.format("Error occurred while hard reseting repository [%s].", repositoryName);

        try {
            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace, repositoryName)
                                            .getCanonicalFile();
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());
            try {
                gitConnector.hardReset();
            } catch (GitAPIException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e.getMessage());
                }
            }

            String message = String.format("Repository [%s] successfully reset.", repositoryName);
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        } catch (IOException | GitConnectorException e) {
            if (logger.isErrorEnabled()) {
                logger.error(errorMessage, e);
            }
            throw new GitConnectorException(errorMessage, e);
        }
    }

}
