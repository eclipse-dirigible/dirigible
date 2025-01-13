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

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.project.ProjectPropertiesVerifier;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatus;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Get status of the Git repository.
 */
@Component
public class StatusCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(StatusCommand.class);

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new status command.
     *
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public StatusCommand(ProjectPropertiesVerifier projectPropertiesVerifier) {
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
     * Execute the Status command.
     *
     * @param workspace the workspace
     * @param project the project
     * @return project status
     * @throws GitConnectorException in case of exception
     */
    public ProjectStatus execute(String workspace, String project) throws GitConnectorException {

        if (projectPropertiesVerifier.verify(workspace, project)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Start getting Status for project [%s]...", project));
            }
            ProjectStatus status = getStatus(workspace, project);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Status of the project [%s] finished.", project));
            }
            return status;
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("Project [%s] is local only. Select a previously cloned project for Status operation.", project));
            }
        }
        return null;
    }

    /**
     * Getting status low level git commands.
     *
     * @param workspace the workspace
     * @param project the project
     * @return project status
     * @throws GitConnectorException in case of exception
     */
    private ProjectStatus getStatus(String workspace, String project) throws GitConnectorException {
        String errorMessage = String.format("Error occurred whilegetting the status for project [%s].", project);

        try {
            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace, project)
                                            .getCanonicalFile();
            String git = gitDirectory.getCanonicalPath() + File.separator;

            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());
            ProjectStatus projectStatus = null;
            try {
                Status status = gitConnector.status();
                projectStatus =
                        new ProjectStatus(project, git, status.getAdded(), status.getChanged(), status.getRemoved(), status.getMissing(),
                                status.getModified(), status.getConflicting(), status.getUntracked(), status.getUntrackedFolders());
                return projectStatus;
            } catch (GitAPIException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e.getMessage());
                }
            }

            String message = String.format("Repository [%s] successfully reset.", project);
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        } catch (IOException | GitConnectorException e) {
            if (logger.isErrorEnabled()) {
                logger.error(errorMessage, e);
            }
            throw new GitConnectorException(errorMessage, e);
        }
        return null;
    }

}
