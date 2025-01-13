/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatus;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;
import org.eclipse.dirigible.components.ide.workspace.domain.Status;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Workspace Json Helper.
 */
@Component
public class WorkspaceJsonHelper implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceJsonHelper.class);

    /** The workspace json helper instance. */
    private static WorkspaceJsonHelper INSTANCE;

    /** The project status provider. */
    private ProjectStatusProvider projectStatusProvider;

    /**
     * Instantiates a new workspace json helper.
     *
     * @param projectStatusProvider the project status provider
     */
    @Autowired
    private WorkspaceJsonHelper(ProjectStatusProvider projectStatusProvider) {
        this.projectStatusProvider = projectStatusProvider;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static WorkspaceJsonHelper get() {
        return INSTANCE;
    }

    /**
     * Gets the project status provider.
     *
     * @return the project status provider
     */
    public ProjectStatusProvider getProjectStatusProvider() {
        return projectStatusProvider;
    }

    /**
     * Describe workspace.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the workspace descriptor
     */
    public static WorkspaceDescriptor describeWorkspace(ICollection collection, String removePathPrefix, String addPathPrefix) {
        WorkspaceDescriptor workspacePojo = new WorkspaceDescriptor();
        workspacePojo.setName(collection.getName());
        workspacePojo.setPath(addPathPrefix + collection.getPath()
                                                        .substring(removePathPrefix.length()));
        List<ICollection> collections = collection.getCollections();
        Map<String, ProjectStatus> projectStatusCache = new HashMap<String, ProjectStatus>();
        for (ICollection childCollection : collections) {
            workspacePojo.getProjects()
                         .add(describeProject(workspacePojo.getName(), childCollection, removePathPrefix, addPathPrefix,
                                 projectStatusCache));
        }

        return workspacePojo;
    }

    /**
     * Describe workspace projects.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the workspace descriptor
     */
    public static WorkspaceDescriptor describeWorkspaceProjects(ICollection collection, String removePathPrefix, String addPathPrefix) {
        WorkspaceDescriptor workspacePojo = new WorkspaceDescriptor();
        workspacePojo.setName(collection.getName());
        workspacePojo.setPath(addPathPrefix + collection.getPath()
                                                        .substring(removePathPrefix.length()));
        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            workspacePojo.getProjects()
                         .add(describeProjectOnly(childCollection, removePathPrefix, addPathPrefix));
        }

        return workspacePojo;
    }

    /**
     * Describe project.
     *
     * @param workspace the workspace
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the project descriptor
     */
    public static ProjectDescriptor describeProject(String workspace, ICollection collection, String removePathPrefix,
            String addPathPrefix) {
        return describeProject(workspace, collection, removePathPrefix, addPathPrefix, new HashMap<String, ProjectStatus>());
    }

    /**
     * Describe project.
     *
     * @param workspace the workspace
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @param projectStatusCache the project status cache
     * @return the project descriptor
     */
    public static ProjectDescriptor describeProject(String workspace, ICollection collection, String removePathPrefix, String addPathPrefix,
            Map<String, ProjectStatus> projectStatusCache) {
        ProjectDescriptor projectPojo = new ProjectDescriptor();
        projectPojo.setName(collection.getName());
        projectPojo.setPath(addPathPrefix + collection.getPath()
                                                      .substring(removePathPrefix.length()));
        RepositoryPath repositoryPath = new RepositoryPath(collection.getPath());

        ProjectStatus status = getProjectStatus(workspace, collection, projectPojo, repositoryPath, projectStatusCache);

        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            projectPojo.getFolders()
                       .add(describeFolder(childCollection, removePathPrefix, addPathPrefix, status));
        }

        List<IResource> resources = collection.getResources();
        for (IResource childResource : resources) {
            projectPojo.getFiles()
                       .add(describeFile(childResource, removePathPrefix, addPathPrefix, status));
        }

        return projectPojo;
    }

    /**
     * Gets the project status.
     *
     * @param workspace the workspace
     * @param collection the collection
     * @param projectPojo the project pojo
     * @param repositoryPath the repository path
     * @return the project status
     */
    private static ProjectStatus getProjectStatus(String workspace, ICollection collection, ProjectDescriptor projectPojo,
            RepositoryPath repositoryPath) {
        return getProjectStatus(workspace, collection, projectPojo, repositoryPath, new HashMap<String, ProjectStatus>());
    }

    /**
     * Gets the project status.
     *
     * @param workspace the workspace
     * @param collection the collection
     * @param projectPojo the project pojo
     * @param repositoryPath the repository path
     * @param projectStatusCache the project status cache
     * @return the project status
     */
    private static ProjectStatus getProjectStatus(String workspace, ICollection collection, ProjectDescriptor projectPojo,
            RepositoryPath repositoryPath, Map<String, ProjectStatus> projectStatusCache) {
        Pair<Boolean, String> gitInfo = WorkspaceGitHelper.getGitAware(collection.getRepository(), repositoryPath.toString());
        projectPojo.setGit(gitInfo.getLeft());
        projectPojo.setGitName(gitInfo.getRight());
        if (projectPojo.isGit()) {
            String git = "";
            try {
                git = WorkspaceJsonHelper.get()
                                         .getProjectStatusProvider()
                                         .getProjectGitFolder(workspace, projectPojo.getName());
                if (git != null) {
                    git = findRootGit(git);
                }
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(git);
                }
            }
            ProjectStatus status = null;
            if (projectStatusCache.containsKey(projectPojo.getGitName())) {
                status = projectStatusCache.get(projectPojo.getGitName());
                status = remapPaths(projectPojo.getName(), git, status);
            } else if (projectPojo.isGit()) {
                status = WorkspaceJsonHelper.get()
                                            .getProjectStatusProvider()
                                            .getProjectStatus(collection.getParent()
                                                                        .getName(),
                                                    collection.getName());
                projectStatusCache.put(projectPojo.getGitName(), status);
                status = remapPaths(projectPojo.getName(), git, status);
            }
            return status;
        }
        return null;
    }

    /**
     * Find root git.
     *
     * @param git the git
     * @return the string
     */
    private static String findRootGit(String git) {
        if (Paths.get(git, FileSystemUtils.DOT_GIT)
                 .toFile()
                 .exists()) {
            return Paths.get(git)
                        .toString();
        }
        if (Paths.get(git)
                 .toFile() != null) {
            return findRootGit(Paths.get(git)
                                    .toFile()
                                    .getParent());
        }
        return "";
    }

    /**
     * Remap paths.
     *
     * @param project the project
     * @param git the git
     * @param status the status
     * @return the project status
     */
    private static ProjectStatus remapPaths(String project, String git, ProjectStatus status) {
        ProjectStatus result = new ProjectStatus(project, git, canonizePaths(git, project, status.getAdded()),
                canonizePaths(git, project, status.getChanged()), canonizePaths(git, project, status.getRemoved()),
                canonizePaths(git, project, status.getMissing()), canonizePaths(git, project, status.getModified()),
                canonizePaths(git, project, status.getConflicting()), canonizePaths(git, project, status.getUntracked()),
                canonizePaths(git, project, status.getUntrackedFolders()));
        return result;
    }

    /**
     * Canonize paths.
     *
     * @param git the heading
     * @param project the project
     * @param names the names
     * @return the sets the
     */
    private static Set<String> canonizePaths(String git, String project, Set<String> names) {
        Set<String> paths = new HashSet<>();
        for (String name : names) {
            if (name.indexOf(project) >= 0) {
                paths.add(git + File.separator + name);
            }
        }
        return paths;
    }

    /**
     * Describe project.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the project descriptor
     */
    public static ProjectDescriptor describeProjectOnly(ICollection collection, String removePathPrefix, String addPathPrefix) {
        ProjectDescriptor projectPojo = new ProjectDescriptor();
        projectPojo.setName(collection.getName());
        projectPojo.setPath(addPathPrefix + collection.getPath()
                                                      .substring(removePathPrefix.length()));
        RepositoryPath repositoryPath = new RepositoryPath(collection.getPath());

        Pair<Boolean, String> gitInfo = WorkspaceGitHelper.getGitAware(collection.getRepository(), repositoryPath.toString());

        projectPojo.setGit(gitInfo.getLeft());
        projectPojo.setGitName(gitInfo.getRight());

        return projectPojo;
    }

    /**
     * Describe folder.
     *
     * @param workspace the workspace
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the folder descriptor
     */
    public static FolderDescriptor describeFolder(String workspace, ICollection collection, String removePathPrefix, String addPathPrefix) {
        ProjectDescriptor projectPojo = getProjectForStatus(collection, removePathPrefix, addPathPrefix);
        RepositoryPath repositoryPath = new RepositoryPath(removePathPrefix + projectPojo.getPath());

        ProjectStatus status = getProjectStatus(workspace, collection.getRepository()
                                                                     .getCollection(repositoryPath.toString()),
                projectPojo, repositoryPath);
        return describeFolder(collection, removePathPrefix, addPathPrefix, status);
    }

    /**
     * Gets the project for status.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the project for status
     */
    private static ProjectDescriptor getProjectForStatus(ICollection collection, String removePathPrefix, String addPathPrefix) {
        String path = collection.getPath()
                                .substring(removePathPrefix.length());
        String[] paths = path.split(IRepositoryStructure.SEPARATOR);
        String workspaceName = paths[1];
        String projectName = paths[2];
        ProjectDescriptor projectPojo = new ProjectDescriptor();
        projectPojo.setName(projectName);
        projectPojo.setPath(addPathPrefix + IRepositoryStructure.SEPARATOR + workspaceName + IRepositoryStructure.SEPARATOR + projectName);
        return projectPojo;
    }

    /**
     * Describe folder.
     *
     * @param collection the collection
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @param status the project status
     * @return the folder descriptor
     */
    public static FolderDescriptor describeFolder(ICollection collection, String removePathPrefix, String addPathPrefix,
            ProjectStatus status) {
        FolderDescriptor folderPojo = new FolderDescriptor();
        folderPojo.setName(collection.getName());
        folderPojo.setPath(addPathPrefix + collection.getPath()
                                                     .substring(removePathPrefix.length()));
        if (status != null) {

            try {
                String path = new File(collection.getRepository()
                                                 .getInternalResourcePath(collection.getPath())).getCanonicalPath();

                if (status.getUntrackedFolders()
                          .contains(path)) {
                    folderPojo.setStatus(Status.U.name());
                }
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage());
                }
            }
        }
        List<ICollection> collections = collection.getCollections();
        for (ICollection childCollection : collections) {
            folderPojo.getFolders()
                      .add(describeFolder(childCollection, removePathPrefix, addPathPrefix, status));
        }

        List<IResource> resources = collection.getResources();
        for (IResource childResource : resources) {
            folderPojo.getFiles()
                      .add(describeFile(childResource, removePathPrefix, addPathPrefix, status));
        }

        return folderPojo;
    }

    /**
     * Describe file.
     *
     * @param workspace the workspace
     * @param resource the resource
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @return the file descriptor
     */
    public static FileDescriptor describeFile(String workspace, IResource resource, String removePathPrefix, String addPathPrefix) {
        ProjectDescriptor projectPojo = getProjectForStatus(resource.getParent(), removePathPrefix, addPathPrefix);
        RepositoryPath repositoryPath = new RepositoryPath(removePathPrefix + projectPojo.getPath());

        ProjectStatus status = getProjectStatus(workspace, resource.getRepository()
                                                                   .getCollection(repositoryPath.toString()),
                projectPojo, repositoryPath);
        return describeFile(resource, removePathPrefix, addPathPrefix, status);
    }

    /**
     * Describe file.
     *
     * @param resource the resource
     * @param removePathPrefix the remove path prefix
     * @param addPathPrefix the add path prefix
     * @param status the project status
     * @return the file descriptor
     */
    public static FileDescriptor describeFile(IResource resource, String removePathPrefix, String addPathPrefix, ProjectStatus status) {
        FileDescriptor resourcePojo = new FileDescriptor();
        resourcePojo.setName(resource.getName());
        resourcePojo.setPath(addPathPrefix + resource.getPath()
                                                     .substring(removePathPrefix.length()));
        resourcePojo.setContentType(resource.getContentType());
        if (status != null) {

            try {
                String path = new File(resource.getRepository()
                                               .getInternalResourcePath(resource.getPath())).getCanonicalPath();

                if (status.getAdded()
                          .contains(path)) {
                    resourcePojo.setStatus(Status.A.name());
                } else if (status.getChanged()
                                 .contains(path)
                        || status.getModified()
                                 .contains(path)
                        || status.getMissing()
                                 .contains(path)) {
                    resourcePojo.setStatus(Status.M.name());
                } else if (status.getConflicting()
                                 .contains(path)) {
                    resourcePojo.setStatus(Status.C.name());
                } else if (status.getRemoved()
                                 .contains(path)) {
                    resourcePojo.setStatus(Status.D.name());
                } else if (status.getUntracked()
                                 .contains(path)) {
                    resourcePojo.setStatus(Status.U.name());
                }
            } catch (IOException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage());
                }
            }
        }
        return resourcePojo;
    }

}
