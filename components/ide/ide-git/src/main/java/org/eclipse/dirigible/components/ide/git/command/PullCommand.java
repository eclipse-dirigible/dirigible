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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.git.model.GitPullModel;
import org.eclipse.dirigible.components.ide.git.project.ProjectPropertiesVerifier;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.project.ProjectMetadataManager;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Pull project(s) from a Git repository and optionally publish it.
 */
@Component
public class PullCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PullCommand.class);

    /** The publisher core service. */
    private PublisherService publisherService;

    /** The project metadata manager. */
    private ProjectMetadataManager projectMetadataManager;

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new pull command.
     *
     * @param publisherService the publisher service
     * @param projectMetadataManager the project metadata manager
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public PullCommand(PublisherService publisherService, ProjectMetadataManager projectMetadataManager,
            ProjectPropertiesVerifier projectPropertiesVerifier) {
        this.publisherService = publisherService;
        this.projectMetadataManager = projectMetadataManager;
        this.projectPropertiesVerifier = projectPropertiesVerifier;
    }

    /**
     * Gets the publisher service.
     *
     * @return the publisher service
     */
    public PublisherService getPublisherService() {
        return publisherService;
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
     * Execute a Pull command.
     *
     * @param workspace the workspace
     * @param model the git pull model
     * @throws GitConnectorException in case of exception
     */
    public void execute(final Workspace workspace, GitPullModel model) throws GitConnectorException {
        if (model.getProjects() == null) {
            model.setProjects(GitFileUtils.getAllGitProjects(workspace));
        }

        if (model.getProjects()
                 .size() == 0) {
            logger.warn("No git aware project is selected for the Pull action");
        }

        List<String> pulledProjects = new ArrayList<String>();
        boolean atLeastOne = false;
        for (String repositoryName : model.getProjects()) {
            if (projectPropertiesVerifier.verify(workspace.getName(), repositoryName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Start pulling %s repository...", repositoryName));
                }
                boolean pulled = pullProjectFromGitRepository(workspace, repositoryName, model);
                atLeastOne = atLeastOne ? atLeastOne : pulled;
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Pull of the repository %s finished.", repositoryName));
                }
                List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), repositoryName);
                pulledProjects.addAll(projects);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format("Project %s is local only. Select a previously cloned project for Pull operation.",
                            repositoryName));
                }
            }
        }

        if (atLeastOne && model.isPublish()) {
            publishProjects(workspace, pulledProjects);
        }

    }

    /**
     * Pull project from git repository by executing several low level Git commands.
     *
     * @param workspace the workspace
     * @param repositoryName the selected project
     * @param model the git pull model
     * @return true, if successful
     * @throws GitConnectorException in case of exception
     */
    private boolean pullProjectFromGitRepository(final Workspace workspace, String repositoryName, GitPullModel model)
            throws GitConnectorException {
        String errorMessage = String.format("Error occurred while pulling repository [%s].", repositoryName);

        List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), repositoryName);
        for (String projectName : projects) {
            projectMetadataManager.ensureProjectMetadata(workspace, projectName);
        }

        try {

            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace.getName(), repositoryName);
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());

            String gitRepositoryBranch = gitConnector.getBranch();
            if (logger.isDebugEnabled()) {
                logger.debug(
                        String.format("Starting pull of the repository [%s] for the branch %s...", repositoryName, gitRepositoryBranch));
            }
            gitConnector.pull(model.getUsername(), model.getPassword());
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Pull of the repository %s finished.", repositoryName));
            }

            int numberOfConflictingFiles = gitConnector.status()
                                                       .getConflicting()
                                                       .size();
            if (logger.isDebugEnabled()) {
                logger.debug(
                        String.format("Number of conflicting files in the repository [%s]: %d.", repositoryName, numberOfConflictingFiles));
            }

            if (numberOfConflictingFiles > 0) {
                String message = String.format(
                        "Repository [%s] has %d conflicting file(s). You can use Push to submit your changes in a new branch for further merge or use Reset to abandon your changes.",
                        repositoryName, numberOfConflictingFiles);
                if (logger.isErrorEnabled()) {
                    logger.error(message);
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
        return true;
    }

    /**
     * Publish projects.
     *
     * @param workspace the workspace
     * @param pulledProjects the pulled projects
     */
    private void publishProjects(Workspace workspace, List<String> pulledProjects) {
        if (pulledProjects.size() > 0) {
            for (String pulledProject : pulledProjects) {
                List<Project> projects = workspace.getProjects();
                for (Project project : projects) {
                    if (project.getName()
                               .equals(pulledProject)) {
                        try {
                            publisherService.publish(workspace.getName(), pulledProject, "");
                            if (logger.isInfoEnabled()) {
                                logger.info(String.format("Project [%s] has been published", project.getName()));
                            }
                        } catch (Exception e) {
                            if (logger.isErrorEnabled()) {
                                logger.error(String.format("An error occurred while publishing the pulled project [%s]", project.getName()),
                                        e);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

}
