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
import org.eclipse.dirigible.components.ide.git.model.GitCheckoutModel;
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
public class CheckoutCommand {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CheckoutCommand.class);

    /** The publisher core service. */
    private PublisherService publisherService;

    /** The project metadata manager. */
    private ProjectMetadataManager projectMetadataManager;

    /** The verifier. */
    private ProjectPropertiesVerifier projectPropertiesVerifier;

    /**
     * Instantiates a new checkout command.
     *
     * @param publisherService the publisher service
     * @param projectMetadataManager the project metadata manager
     * @param projectPropertiesVerifier the project properties verifier
     */
    @Autowired
    public CheckoutCommand(PublisherService publisherService, ProjectMetadataManager projectMetadataManager,
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
     * @param model the git checkout model
     * @throws GitConnectorException in case of exception
     */
    public void execute(final Workspace workspace, GitCheckoutModel model) throws GitConnectorException {
        boolean atLeastOne = false;
        if (projectPropertiesVerifier.verify(workspace.getName(), model.getProject())) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Start checkout %s repository and %s branch...", model.getProject(), model.getBranch()));
            }
            boolean checkedout = checkoutProjectFromGitRepository(workspace, model);
            atLeastOne = checkedout;
            logger.debug(String.format("Pull of the repository %s finished.", model.getProject()));
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("Project %s is local only. Select a previously cloned project for Checkout operation.",
                        model.getProject()));
            }
        }

        if (atLeastOne && model.isPublish()) {
            List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), model.getProject());
            publishProjects(workspace, projects);
        }

    }

    /**
     * Checkout project from git repository by executing several low level Git commands.
     *
     * @param workspace the workspace
     * @param model the git checkout model
     * @return true, if successful
     * @throws GitConnectorException in case of exception
     */
    private boolean checkoutProjectFromGitRepository(final Workspace workspace, GitCheckoutModel model) throws GitConnectorException {
        String errorMessage = String.format("Error occurred while pulling repository [%s].", model.getProject());

        List<String> projects = GitFileUtils.getGitRepositoryProjects(workspace.getName(), model.getProject());
        for (String projectName : projects) {
            projectMetadataManager.ensureProjectMetadata(workspace, projectName);
        }

        try {
            File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace.getName(), model.getProject());
            IGitConnector gitConnector = GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());

            if (logger.isDebugEnabled()) {
                logger.debug(
                        String.format("Starting checkout of the repository [%s] and branch %s ...", model.getProject(), model.getBranch()));
            }
            gitConnector.checkout(model.getBranch());
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Checkout of the repository %s and branch %s finished.", model.getProject(), model.getBranch()));
            }

            int numberOfConflictingFiles = gitConnector.status()
                                                       .getConflicting()
                                                       .size();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Number of conflicting files in the repository [%s]: %d.", model.getProject(),
                        numberOfConflictingFiles));
            }

            if (numberOfConflictingFiles > 0) {
                String message = String.format(
                        "Repository [%s] has %d conflicting file(s). You can use Push to submit your changes in a new branch for further merge or use Reset to abandon your changes.",
                        model.getProject(), numberOfConflictingFiles);
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new GitConnectorException(message);
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
    protected void publishProjects(Workspace workspace, List<String> pulledProjects) {
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
                            if (logger.isInfoEnabled()) {
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
