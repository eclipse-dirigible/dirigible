/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.ide.git.domain.GitConnectorFactory;
import org.eclipse.dirigible.components.ide.git.domain.IGitConnector;
import org.eclipse.dirigible.components.ide.workspace.domain.Folder;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.local.LocalWorkspaceMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * The Git file utility.
 */
@Component
public class GitFileUtils implements InitializingBean {

    /** The Constant USER_WORKSPACE_PROJECT_SEGMENTS_COUNT. */
    private static final int USER_WORKSPACE_PROJECT_SEGMENTS_COUNT = 4;

    /** The Constant USER_WORKSPACE_SEGMENTS_COUNT. */
    private static final int USER_WORKSPACE_SEGMENTS_COUNT = 3;

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GitFileUtils.class);

    /** The Constant DIRIGIBLE_GIT_ROOT_FOLDER. */
    private static final String DIRIGIBLE_GIT_ROOT_FOLDER = "DIRIGIBLE_GIT_ROOT_FOLDER"; //$NON-NLS-1$

    /** The Constant DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER. */
    private static final String DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER = "DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER"; //$NON-NLS-1$

    /** The Constant SLASH. */
    public static final String SLASH = "/"; //$NON-NLS-1$

    /** The Constant DOT_GIT. */
    public static final String DOT_GIT = ".git"; //$NON-NLS-1$

    /** The Constant PATTERN_USERS_WORKSPACE. */
    public static final String PATTERN_USERS_WORKSPACE = IRepositoryStructure.PATH_USERS + "/%s/%s/"; // /users/john/workspace1

    /** The Constant REPOSITORY_GIT_FOLDER. */
    private static final String REPOSITORY_GIT_FOLDER = "dirigible" + File.separator + "repository" + File.separator + DOT_GIT;

    /** The Constant DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER. */
    private static final String DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER = "target" + File.separator + REPOSITORY_GIT_FOLDER; //$NON-NLS-1$

    /** The Constant MINIMUM_URL_LENGTH. */
    private static final int MINIMUM_URL_LENGTH = 25;

    /** The Constant TEMP_DIRECTORY_PREFIX. */
    public static final String TEMP_DIRECTORY_PREFIX = "dirigible_git_";

    /** The git root folder. */
    private static String GIT_ROOT_FOLDER;

    /** The repository. */
    private IRepository repository;

    /** The database facade. */
    private static GitFileUtils INSTANCE;

    /**
     * Instantiates a new git file utils.
     *
     * @param repository the repository
     */
    private GitFileUtils(IRepository repository) {
        this.repository = repository;
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
    public static GitFileUtils get() {
        return INSTANCE;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    static {
        if (!StringUtils.isEmpty(Configuration.get(DIRIGIBLE_GIT_ROOT_FOLDER))) {
            GIT_ROOT_FOLDER = Configuration.get(DIRIGIBLE_GIT_ROOT_FOLDER);
        } else if (!StringUtils.isEmpty(Configuration.get(DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER))) {
            GIT_ROOT_FOLDER = Configuration.get(DIRIGIBLE_REPOSITORY_LOCAL_ROOT_FOLDER) + File.separator + REPOSITORY_GIT_FOLDER;
        } else {
            GIT_ROOT_FOLDER = DEFAULT_DIRIGIBLE_GIT_ROOT_FOLDER;
        }
    }

    /**
     * Checks if is valid repository URI.
     *
     * @param repositoryURI the repository URI
     * @return true, if is valid repository URI
     */
    public static boolean isValidRepositoryURI(String repositoryURI) {
        return (repositoryURI.endsWith(DOT_GIT)) && (repositoryURI.length() > MINIMUM_URL_LENGTH);
    }

    /**
     * Create the directory for git.
     *
     * @param user the logged-in user
     * @param workspace the workspace
     * @param repositoryURI the repository URI
     * @return the directory
     * @throws IOException IO error
     */
    public static File createGitDirectory(String user, String workspace, String repositoryURI) throws IOException {
        String repositoryName = generateGitRepositoryName(repositoryURI);
        return FileSystemUtils.forceCreateDirectory(GIT_ROOT_FOLDER, user, workspace, repositoryName);
    }

    /**
     * Delete git directory.
     *
     * @param user the user
     * @param workspace the workspace
     * @param repositoryURI the repository URI
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void deleteGitDirectory(String user, String workspace, String repositoryURI) throws IOException {
        String repositoryName = generateGitRepositoryName(repositoryURI);

        FileSystemUtils.deleteDirectory(GIT_ROOT_FOLDER, user, workspace, repositoryName);
    }

    /**
     * Import project.
     *
     * @param workspace the workspace
     * @param repository the repository
     * @return the imported project
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<String> importProject(String workspace, String repository) throws IOException {
        String user = UserFacade.getName();
        File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace, repository);
        String workspacePath = String.format(GitFileUtils.PATTERN_USERS_WORKSPACE, user, workspace);
        return GitFileUtils.importProject(gitDirectory, workspacePath, user, workspace);
    }

    /**
     * Import project.
     *
     * @param gitRepository the git directory
     * @param basePath the base path
     * @param user the user
     * @param workspace the workspace
     * @return the list
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<String> importProject(File gitRepository, String basePath, String user, String workspace) throws IOException {
        List<File> projects = FileSystemUtils.getGitRepositoryProjects(gitRepository);
        List<String> importedProjects = new ArrayList<String>();
        if (projects.size() == 0) {
            // Empty Git repository, using it as a root project
            projects.add(gitRepository);
        }
        for (File file : projects) {
            String project = file.getName();
            if (file.isDirectory() && !project.startsWith(".")) {
                importProjectFromGitRepositoryToWorkspace(file, basePath + project);
                importedProjects.add(project);
            }
        }
        return importedProjects;
    }

    /**
     * Import project from git repository to workspace.
     *
     * @param gitRepositoryFile the git repository file
     * @param path the path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void importProjectFromGitRepositoryToWorkspace(File gitRepositoryFile, String path) throws IOException {
        // Skip already linked paths
        if (!GitFileUtils.get()
                         .getRepository()
                         .isLinkedPath(path)) {
            GitFileUtils.get()
                        .getRepository()
                        .linkPath(path, gitRepositoryFile.getCanonicalPath());
        }
    }

    /**
     * Delete repository project.
     *
     * @param project the project
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void deleteRepositoryProject(Project project) throws IOException {
        project.delete();
    }

    /**
     * Returns the local absolute path of a Repository path.
     *
     * @param path the Repository path
     * @return the local absolute path
     */
    public String getAbsolutePath(String path) {
        if (GitFileUtils.get()
                        .getRepository() instanceof FileSystemRepository) {
            String absolutePath = LocalWorkspaceMapper.getMappedName((FileSystemRepository) getRepository(), path);
            return absolutePath;
        }
        throw new IllegalArgumentException("Repository must be file based to use git utilities: " + path);
    }

    /**
     * Copy project to directory.
     *
     * @param source the source
     * @param tempGitDirectory the temp git directory
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyProjectToDirectory(Folder source, File tempGitDirectory) throws IOException {
        copyProjectToDirectory(source, tempGitDirectory, false);
    }

    /**
     * Copy project to directory.
     *
     * @param source the source
     * @param tempGitDirectory the temp git directory
     * @param shareInRootFolder the whether to share project in the repository root folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyProjectToDirectory(Folder source, File tempGitDirectory, boolean shareInRootFolder) throws IOException {
        if (!source.exists()) {
            return;
        }
        for (org.eclipse.dirigible.components.ide.workspace.domain.File file : source.getFiles()) {
            RepositoryPath path = new RepositoryPath(file.getPath());
            StringBuilder resourcePath = new StringBuilder();
            int startSegmentIndex = shareInRootFolder ? USER_WORKSPACE_PROJECT_SEGMENTS_COUNT : USER_WORKSPACE_SEGMENTS_COUNT;
            String[] pathSegments = path.getSegments();
            for (int i = startSegmentIndex; i < pathSegments.length; i++) {
                resourcePath.append(File.separator)
                            .append(pathSegments[i]);
            }
            File fileResource = new File(tempGitDirectory, resourcePath.toString());
            FileUtils.forceMkdirParent(fileResource.getCanonicalFile());

            InputStream in = null;
            FileOutputStream out = null;
            try {
                in = new ByteArrayInputStream(file.getContent());
                File outputFile = new File(tempGitDirectory, resourcePath.toString());
                FileUtils.forceMkdir(outputFile.getParentFile()
                                               .getCanonicalFile());
                boolean fileCreated = outputFile.createNewFile();
                if (!fileCreated) {
                    throw new IOException("Error in creating the file: " + outputFile.getCanonicalPath());
                }
                out = new FileOutputStream(outputFile);
                IOUtils.copy(in, out);
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        }
        for (Folder folder : source.getFolders()) {
            copyProjectToDirectory(folder, tempGitDirectory, shareInRootFolder);
        }

    }

    /**
     * Gets the valid project folders.
     *
     * @param gitRepository the git directory
     * @return the valid project folders
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String[] getValidProjectFolders(File gitRepository) throws IOException {
        List<File> projects = FileSystemUtils.getGitRepositoryProjects(gitRepository);
        List<String> projectsNames = projects.stream()
                                             .map(e -> e.getName())
                                             .collect(Collectors.toList());
        return projectsNames.toArray(new String[] {});
    }

    /**
     * Repository exists.
     *
     * @param directory the directory
     * @return true, if successful
     */
    public static boolean repositoryExists(File directory) {
        try {
            Git git = Git.open(directory);
            Repository repo = git.getRepository();
            for (Ref ref : repo.getRefDatabase()
                               .getRefs()) {
                if (ref.getObjectId() == null)
                    continue;
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if is git project.
     *
     * @param repository the repository
     * @param repositoryPath the repository path
     * @return true, if is git project
     */
    public static boolean isGitProject(IRepository repository, String repositoryPath) {
        try {
            if (repository instanceof FileSystemRepository) {

                String path = LocalWorkspaceMapper.getMappedName((FileSystemRepository) repository, repositoryPath);
                File gitDirectory = new File(path).getCanonicalFile();
                // IGitConnector gitConnector =
                // GitConnectorFactory.getConnector(gitDirectory.getCanonicalPath());
                // gitConnector.getBranch();
                return repositoryExists(gitDirectory);
            }
            if (logger.isErrorEnabled()) {
                logger.error("Not a file system based repository used with git");
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            return false;
        }
        return false;
    }

    /**
     * Gets the all git projects.
     *
     * @param workspace the workspace
     * @return the all git projects
     */
    public static final List<String> getAllGitProjects(Workspace workspace) {
        return workspace.getProjects()
                        .stream()
                        .filter(p -> isGitProject(p.getRepository(), p.getInternal()
                                                                      .getPath()))
                        .map(Project::getName)
                        .collect(Collectors.toList());
    }

    /**
     * Generate the local repository name.
     *
     * @param repositoryURI the URI odf the repository
     * @return the generated local name
     */
    public static String generateGitRepositoryName(String repositoryURI) {
        return FileSystemUtils.generateGitRepositoryName(repositoryURI);
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @return the directory
     */
    public static File getGitDirectory(String user, String workspace) {
        return FileSystemUtils.getGitDirectory(user, workspace);
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryURI the repository URI
     * @return the directory
     */
    public static File getGitDirectory(String user, String workspace, String repositoryURI) {
        return FileSystemUtils.getGitDirectory(user, workspace, repositoryURI);
    }

    /**
     * Get the directory for git.
     *
     * @param workspace the workspace
     * @param repositoryName the repository URI
     * @return the directory
     */
    public static File getGitDirectoryByRepositoryName(String workspace, String repositoryName) {
        return getGitDirectoryByRepositoryName(UserFacade.getName(), workspace, repositoryName);
    }

    /**
     * Get the directory for git.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryName the repository URI
     * @return the directory
     */
    public static File getGitDirectoryByRepositoryName(String user, String workspace, String repositoryName) {
        // File gitDirectoryByRepositoryName =
        // FileSystemUtils.getGitDirectoryByRepositoryName(user,
        // workspace, repositoryName + IRepository.SEPARATOR + repositoryName);
        File gitDirectoryByRepositoryName = FileSystemUtils.getGitDirectoryByRepositoryName(user, workspace, repositoryName);
        if (gitDirectoryByRepositoryName == null) {
            return getGitDeepDirectoryByRepositoryName(UserFacade.getName(), workspace, repositoryName);
        }
        return gitDirectoryByRepositoryName;
    }

    /**
     * Get the directory for git for deep projects.
     *
     * @param user logged-in user
     * @param workspace the workspace
     * @param repositoryName the repository URI
     * @return the directory
     */
    private static File getGitDeepDirectoryByRepositoryName(String user, String workspace, String repositoryName) {
        return FileSystemUtils.getGitDeepDirectoryByRepositoryName(user, workspace, repositoryName);
    }

    /**
     * Get the projects in that git repository.
     *
     * @param workspace the workspace
     * @param repositoryName the repository
     * @return the projects
     */
    public static List<String> getGitRepositoryProjects(String workspace, String repositoryName) {
        return FileSystemUtils.getGitRepositoryProjects(UserFacade.getName(), workspace, repositoryName);
    }

    /**
     * Get the projects in that git repository.
     *
     * @param workspace the workspace
     * @param repositoryName the repository
     * @return the projects
     */
    public static List<File> getGitRepositoryProjectsFiles(String workspace, String repositoryName) {
        return FileSystemUtils.getGitRepositoryProjectsFiles(UserFacade.getName(), workspace, repositoryName);
    }
}
