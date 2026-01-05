/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.command;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorException;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.model.GitCloneModel;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.Folder;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.project.ProjectMetadataManager;
import org.eclipse.dirigible.components.ide.workspace.service.PublisherService;
import org.eclipse.dirigible.components.project.ProjectMetadataDependency;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Clone project(s) from a Git repository and optionally publish it.
 */
@Component
public class CloneCommand {

    /** The Constant PACKAGE_JSON. */
    private static final String PACKAGE_JSON = "package.json";

    /** The Constant NODE_MODULES. */
    private static final String NODE_MODULES = "node_modules";

    /** The Constant NPM_INSTALL. */
    private static final String NPM_INSTALL = "npm install";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CloneCommand.class);
    /** The command service. */
    private final GitCommandService commandService;
    /** The publisher core service. */
    private final PublisherService publisherService;
    /** The project metadata manager. */
    private final ProjectMetadataManager projectMetadataManager;

    /**
     * Instantiates a new clone command.
     *
     * @param publisherService the publisher service
     * @param projectMetadataManager the project metadata manager
     * @param commandService the command service
     */
    @Autowired
    public CloneCommand(PublisherService publisherService, ProjectMetadataManager projectMetadataManager,
            GitCommandService commandService) {
        this.publisherService = publisherService;
        this.projectMetadataManager = projectMetadataManager;
        this.commandService = commandService;
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
     * Execute a Clone command.
     *
     * @param workspace the workspace
     * @param model the git clone model
     * @throws GitConnectorException the git connector exception
     */
    public void execute(Workspace workspace, GitCloneModel model) throws GitConnectorException {
        String repositoryUri = model.getRepository();
        try {
            if (repositoryUri != null && !repositoryUri.endsWith(GitFileUtils.DOT_GIT)) {
                repositoryUri += GitFileUtils.DOT_GIT;
            }
            Set<String> clonedProjects = new HashSet<>();
            logger.debug("Start cloning repository [{}] ...", repositoryUri);
            String user = UserFacade.getName();
            File gitDirectory = GitFileUtils.createGitDirectory(user, workspace.getName(), repositoryUri);
            try {
                cloneProject(user, repositoryUri, model.getBranch(), model.getUsername(), model.getPassword(), gitDirectory, workspace,
                        clonedProjects);
            } catch (GitConnectorException e) {
                throw e;
            }
            logger.debug("Cloning repository [{}] into folder [{}] finished successfully.", repositoryUri, gitDirectory.getCanonicalPath());
            if (model.isPublish()) {
                publishProjects(workspace, clonedProjects);
            }
            logger.info("Project(s) has been cloned successfully from repository: [{}]", repositoryUri);
        } catch (IOException e) {
            throw new GitConnectorException(String.format("An error occurred while cloning repository: [%s]", repositoryUri), e);
        }
    }

    /**
     * Clone project execute several low level Git commands.
     *
     * @param user logged in user
     * @param repositoryURI the repository URI
     * @param repositoryBranch the repository branch
     * @param username the username
     * @param password the password
     * @param gitDirectory the git directory
     * @param workspace the workspace
     * @param clonedProjects the cloned projects
     * @throws GitConnectorException the git connector exception
     */
    protected void cloneProject(final String user, final String repositoryURI, String repositoryBranch, final String username,
            final String password, File gitDirectory, Workspace workspace, Set<String> clonedProjects) throws GitConnectorException {
        try {
            logger.debug("Cloning repository [{}], with username [{}] for branch [{}] in the directory [{}] ...", repositoryURI, username,
                    repositoryBranch, gitDirectory.getCanonicalPath());
            GitConnectorFactory.cloneRepository(gitDirectory.getCanonicalPath(), repositoryURI, username, password, repositoryBranch);
            logger.debug("Cloning repository [{}] finished.", repositoryURI);

            String workspacePath = String.format(GitFileUtils.PATTERN_USERS_WORKSPACE, user, workspace.getName());

            logger.debug("Start importing projects for repository directory [{}] ...", gitDirectory.getCanonicalPath());
            List<String> importedProjects = GitFileUtils.importProject(gitDirectory, workspacePath, user, workspace.getName());
            logger.debug("Importing projects for repository directory [{}] finished", gitDirectory.getCanonicalPath());

            for (String importedProject : importedProjects) {
                logger.info("Project [{}] was cloned", importedProject);
            }

            for (String projectName : importedProjects) {
                projectMetadataManager.ensureProjectMetadata(workspace, projectName);
                clonedProjects.add(projectName);
            }
            logger.debug("Start cloning dependencies ...");
            for (String projectName : importedProjects) {
                logger.debug("Start cloning dependencies of the project [{}]...", projectName);
                cloneDependencies(user, username, password, workspace, clonedProjects, projectName);
                cloneNPMDependencies(user, workspace, projectName);
                logger.debug("Cloning of dependencies of the project [{}] finished", projectName);
            }
            logger.debug("Cloning of dependencies finished");

        } catch (IOException | GitAPIException | GitConnectorException e) {
            String errorMessage = "An error occurred while cloning repository.";
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
            logger.error(errorMessage, e);
            throw new GitConnectorException(errorMessage, e);
        }
    }

    /**
     * Clone project's dependencies if any along with the main project.
     *
     * @param user the logged in user
     * @param username the username
     * @param password the password
     * @param workspace the workspace
     * @param clonedProjects the cloned projects
     * @param projectName the project name
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    protected void cloneDependencies(final String user, final String username, final String password, Workspace workspace,
            Set<String> clonedProjects, String projectName) throws IOException, GitConnectorException {
        Project selectedProject = workspace.getProject(projectName);
        ProjectMetadataDependency[] dependencies = ProjectMetadataManager.getDependencies(selectedProject);
        for (ProjectMetadataDependency dependency : dependencies) {
            String projectGuid = dependency.getGuid();
            if (!clonedProjects.contains(projectGuid)) {
                Project alreadyClonedProject = workspace.getProject(projectGuid);
                String projectRepositoryURI = dependency.getUrl();
                String projectRepositoryBranch = dependency.getBranch();
                if (!alreadyClonedProject.exists()) {
                    File projectGitDirectory = GitFileUtils.createGitDirectory(user, workspace.getName(), projectRepositoryURI);
                    logger.debug("Start cloning of the project [{}] from the repository [{}] and branch [{}] into the directory [{}] ...",
                            projectGuid, projectRepositoryURI, projectRepositoryBranch, projectGitDirectory.getCanonicalPath());
                    try {
                        cloneProject(user, projectRepositoryURI, projectRepositoryBranch, username, password, projectGitDirectory,
                                workspace, clonedProjects); // assume
                    } catch (GitConnectorException e) {
                        throw e;
                    }
                } else {
                    logger.debug("Project [{}] has been already cloned, hence do pull instead.", projectGuid);
                }
                clonedProjects.add(projectGuid);

            } else {
                logger.debug("Project [{}] has been already cloned during this session.", projectGuid);
            }
        }
    }

    /**
     * Clone NPM dependencies.
     *
     * @param user the user
     * @param workspace the workspace
     * @param projectName the project name
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws GitConnectorException the git connector exception
     */
    protected void cloneNPMDependencies(final String user, Workspace workspace, String projectName)
            throws IOException, GitConnectorException {
        Project selectedProject = workspace.getProject(projectName);
        org.eclipse.dirigible.components.ide.workspace.domain.File packageJson = selectedProject.getFile(PACKAGE_JSON);
        if (!packageJson.exists()) {
            logger.info("Will not get the NPM dependencies of the project [{}] since there is NO [{}] file.", projectName, PACKAGE_JSON);
            return;
        }
        logger.info("Retrieving the NPM dependencies of the project [{}] since there is [{}] file...", projectName, PACKAGE_JSON);
        String root = commandService.getRepositoryRoot();
        String workingDirectory = root + packageJson.getParent()
                                                    .getPath();
        try {
            String output = commandService.executeCommandLine(workingDirectory, NPM_INSTALL, null, null, null);
            if (!output.isEmpty() && output.indexOf("error") >= 0) {
                logger.error("Retrieving the NPM dependencies of the project [{}] failed: \n{}\n", projectName, output);
                return;
            }
            Folder nodeModules = selectedProject.getFolder(NODE_MODULES);
            if (nodeModules.exists()) {
                for (Folder folder : nodeModules.getFolders()) {
                    if (folder.getName()
                              .startsWith("@")) {
                        for (Folder subFolder : folder.getFolders()) {
                            subFolder.copyTo(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + subFolder.getName());
                            logger.trace("Retrieving the NPM dependency [{}]", subFolder.getName());
                        }
                    } else {
                        folder.copyTo(IRepositoryStructure.PATH_REGISTRY_PUBLIC + IRepository.SEPARATOR + folder.getName());
                        logger.trace("Retrieving the NPM dependency [{}]", folder.getName());
                    }
                }
                nodeModules.delete();
            }
        } catch (Exception e) {
            logger.error("Retrieving the NPM dependencies of the project [{}] failed", projectName, e);
        }
        logger.info("Retrieving the NPM dependencies of the project [{}] finished", projectName);
    }

    /**
     * Publish projects.
     *
     * @param workspace the workspace
     * @param clonedProjects the cloned projects
     */
    protected void publishProjects(Workspace workspace, Set<String> clonedProjects) {
        if (!clonedProjects.isEmpty()) {
            for (String projectName : clonedProjects) {
                List<Project> projects = workspace.getProjects();
                for (Project project : projects) {
                    if (project.getName()
                               .equals(projectName)) {
                        try {
                            publisherService.publish(generateWorkspacePath(workspace.getName()), projectName, "");
                            logger.info("Project [{}] has been published", project.getName());
                        } catch (Exception e) {
                            logger.error("An error occurred while publishing the cloned project [{}]", project.getName(), e);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Generate workspace path.
     *
     * @param workspace the workspace
     * @return the string builder
     */
    private String generateWorkspacePath(String workspace) {
        return IRepositoryStructure.PATH_USERS + IRepositoryStructure.SEPARATOR + UserFacade.getName() + IRepositoryStructure.SEPARATOR
                + workspace;
    }

}
