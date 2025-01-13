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
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.model.GitPushModel;
import org.eclipse.dirigible.components.ide.git.project.ProjectPropertiesVerifier;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.project.ProjectMetadataManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Push the changes of a project from the local repository to remote Git repository.
 */
@Component
public class PushCommand {

    /** The Constant SHOULD_BE_EMPTY_REPOSITORY. */
    private static final String SHOULD_BE_EMPTY_REPOSITORY = "Should be empty repository: {}";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PushCommand.class);

    /** The project metadata manager. */
    private ProjectMetadataManager projectMetadataManager;

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new push command.
     *
     * @param projectMetadataManager the project metadata manager
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public PushCommand(ProjectMetadataManager projectMetadataManager, ProjectPropertiesVerifier projectPropertiesVerifier) {
        this.projectMetadataManager = projectMetadataManager;
        this.projectPropertiesVerifier = projectPropertiesVerifier;
    }

    /**
     * Gets the project metadata manager.
     *
     * @return the project metadata manager
     */
    public ProjectMetadataManager getProjectMetadataManager() {
        return projectMetadataManager;
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
     * Execute the Push command.
     *
     * @param workspace the workspace
     * @param model the git push modeladd
     * @throws GitConnectorException in case of exception
     */
    public void execute(final Workspace workspace, GitPushModel model) throws GitConnectorException {
        if (model.getProjects() == null) {
            model.setProjects(GitFileUtils.getAllGitProjects(workspace));
        }

        if (model.getProjects()
                 .size() == 0) {
            logger.warn("No git aware project is selected for the Push action");
        }

        for (String repositoryName : model.getProjects()) {
            if (projectPropertiesVerifier.verify(workspace.getName(), repositoryName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Start pushing repository [%s]...", repositoryName));
                }
                pushProjectToGitRepository(workspace, repositoryName, model);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Push of the repository [%s] finished.", repositoryName));
                }
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("Project [%s] is local only. Select a previously clonned project for Push operation.",
                            repositoryName));
                }
            }
        }

    }

    /**
     * Push project to git repository by executing several low level Git commands.
     *
     * @param workspace the workspace
     * @param repositoryName the repository name
     * @param model the git push model
     * @throws GitConnectorException in case of exception
     */
    private void pushProjectToGitRepository(final Workspace workspace, String repositoryName, GitPushModel model)
            throws GitConnectorException {

        String errorMessage = String.format("Error occurred while pushing repository [%s]. ", repositoryName);

        try {
            List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), repositoryName);
            for (String projectName : projects) {
                projectMetadataManager.ensureProjectMetadata(workspace, projectName);
            }

            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace.getName(), repositoryName);
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());

            String gitRepositoryBranch = gitConnector.getBranch();
            if (model.isAutoAdd()) {
                for (String projectName : projects) {
                    gitConnector.add(projectName);
                }
            }
            if (model.isAutoCommit()) {
                gitConnector.commit(model.getCommitMessage(), model.getUsername(), model.getEmail(), true);
            }
            try {
                gitConnector.pull(model.getUsername(), model.getPassword());
            } catch (GitAPIException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(SHOULD_BE_EMPTY_REPOSITORY, e.getMessage());
                }
            }
            int numberOfConflictingFiles = gitConnector.status()
                                                       .getConflicting()
                                                       .size();
            if (numberOfConflictingFiles == 0) {

                gitConnector.push(model.getUsername(), model.getPassword());

                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Repository [%s] has been pushed to remote repository.", repositoryName));
                }
            } else {
                String statusLineMessage = String.format("Project has %d conflicting file(s).", numberOfConflictingFiles);
                if (logger.isWarnEnabled()) {
                    logger.warn(statusLineMessage);
                }
                String message =
                        String.format("Project has %d conflicting file(s). Please merge to [%s] and then continue working on project.",
                                numberOfConflictingFiles, gitRepositoryBranch);
                if (logger.isWarnEnabled()) {
                    logger.warn(message);
                }
            }
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
