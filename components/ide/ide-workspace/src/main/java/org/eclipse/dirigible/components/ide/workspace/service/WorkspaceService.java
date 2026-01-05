/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.extensions.ExtensionsFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.api.utils.UrlFacade;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Folder;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.json.*;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class WorkspaceService.
 */
@Service
public class WorkspaceService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);

    /** The Constant DEFAULT_WORKSPACE_NAME. */
    private static final String DEFAULT_WORKSPACE_NAME = "workspace";

    /** The Constant EXTENSION_PARAMETER_PATH. */
    private static final String EXTENSION_PARAMETER_PATH = "path";

    /** The Constant EXTENSION_PARAMETER_PROJECT. */
    private static final String EXTENSION_PARAMETER_PROJECT = "project";

    /** The Constant EXTENSION_PARAMETER_WORKSPACE. */
    private static final String EXTENSION_PARAMETER_WORKSPACE = "workspace";

    /** The Constant EXTENSION_POINT_IDE_WORKSPACE_ON_SAVE. */
    private static final String EXTENSION_POINT_IDE_WORKSPACE_ON_SAVE = "ide-workspace-on-save";

    /** The Constant WORKSPACES_SERVICE_PREFIX. */
    private static final String WORKSPACES_SERVICE_PREFIX = "ide/workspaces";

    /** The repository. */
    private final IRepository repository;

    /** The javascript service. */
    private final JavascriptService javascriptService;

    /**
     * Instantiates a new workspace service.
     *
     * @param repository the repository
     * @param javascriptService the javascript service
     */
    @Autowired
    public WorkspaceService(IRepository repository, JavascriptService javascriptService) {
        this.repository = repository;
        this.javascriptService = javascriptService;
    }

    /**
     * Gets the javascript service.
     *
     * @return the javascript service
     */
    public JavascriptService getJavascriptService() {
        return javascriptService;
    }

    /**
     * Creates the workspace.
     *
     * @param name the name
     * @return the i workspace
     */
    public Workspace createWorkspace(String name) {
        ICollection collection = getWorkspace(name);
        collection.create();
        if (logger.isInfoEnabled()) {
            logger.info("Workspace created [{}]", collection.getPath());
        }
        return new Workspace(collection);
    }

    // Workspace

    /**
     * Gets the workspace.
     *
     * @param name the name
     * @return the workspace
     */
    public Workspace getWorkspace(String name) {
        StringBuilder workspacePath = generateWorkspacePath(name, null, null);
        ICollection collection = getRepository().getCollection(workspacePath.toString());
        return new Workspace(collection);
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    protected IRepository getRepository() {
        return repository;
    }

    /**
     * Generate workspace path.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the string builder
     */
    private StringBuilder generateWorkspacePath(String workspace, String project, String path) {
        StringBuilder relativePath = new StringBuilder(IRepositoryStructure.PATH_USERS).append(IRepositoryStructure.SEPARATOR)
                                                                                       .append(UserFacade.getName());
        if (workspace != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(workspace);
        }
        if (project != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(project);
        }
        if (path != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(path);
        }
        return relativePath;
    }

    /**
     * Delete workspace.
     *
     * @param name the name
     */
    public void deleteWorkspace(String name) {
        ICollection collection = getWorkspace(name);
        collection.delete();
        if (logger.isInfoEnabled()) {
            logger.info("Workspace deleted [{}]", collection.getPath());
        }
    }

    /**
     * List workspaces.
     *
     * @return the list
     */
    public List<Workspace> listWorkspaces() {
        return getWorkspaces();
    }

    /**
     * Gets the workspaces.
     *
     * @return the workspaces
     */
    public List<Workspace> getWorkspaces() {
        StringBuilder workspacePath = generateWorkspacePath(null, null, null);
        ICollection root = getRepository().getCollection(workspacePath.toString());
        List<Workspace> workspaces = new ArrayList<Workspace>();
        if (!root.exists()) {
            root.create();
        }
        List<ICollection> collections = root.getCollections();
        for (ICollection collection : collections) {
            workspaces.add(new Workspace(collection));
        }
        if (workspaces.isEmpty()) {
            ICollection collection = root.createCollection(DEFAULT_WORKSPACE_NAME);
            workspaces.add(new Workspace(collection));
        }
        return workspaces;
    }

    /**
     * Exists workspace.
     *
     * @param workspace the workspace
     * @return true, if successful
     */
    public boolean existsWorkspace(String workspace) {
        Workspace workspaceObject = getWorkspace(workspace);
        return workspaceObject.exists();
    }

    // Project

    /**
     * Gets the project.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project
     */
    public Project getProject(String workspace, String project) {
        Workspace workspaceObject = getWorkspace(workspace);
        return workspaceObject.getProject(project);
    }

    /**
     * Creates the project.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project
     */
    public Project createProject(String workspace, String project) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.createProject(project);
        boolean isTS = Boolean.parseBoolean(Configuration.get("DIRIGIBLE_PROJECT_TYPESCRIPT", "true"));
        if (isTS) {
            try {
                projectObject.createFile("project.json",
                        String.format(new String(IOUtils.toByteArray(WorkspaceService.class.getResourceAsStream("/project.json_"))),
                                project)
                              .getBytes());
            } catch (IOException e) {
                logger.error("Error on creating 'project.json'", e);
            }
        }
        return projectObject;
    }

    /**
     * Delete project.
     *
     * @param workspace the workspace
     * @param project the project
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void deleteProject(String workspace, String project) throws IOException {
        String user = UserFacade.getName();
        List<String> gitRepositories = FileSystemUtils.getGitRepositories(user, workspace);
        List<String> gitProjects = new ArrayList<String>();
        for (String repositoryName : gitRepositories) {
            gitProjects.addAll(FileSystemUtils.getGitRepositoryProjects(user, workspace, repositoryName));
        }
        boolean isGitProject = gitProjects.contains(project);

        Workspace workspaceObject = getWorkspace(workspace);
        Project workspaceProject = workspaceObject.getProject(project);
        String repositoryRootFolder = workspaceObject.getRepository()
                                                     .getParameter("REPOSITORY_ROOT_FOLDER");

        java.io.File projectFile = null;
        if (isGitProject && repositoryRootFolder != null && workspaceProject.exists()) {
            projectFile = new java.io.File(repositoryRootFolder + workspaceProject.getPath());
            isGitProject = projectFile.exists() && FileUtils.isSymlink(projectFile);
        }

        if (isGitProject && projectFile != null) {
            Files.delete(projectFile.toPath());
        } else {
            workspaceObject.deleteProject(project);
        }
    }

    /**
     * Exists project.
     *
     * @param workspace the workspace
     * @param project the project
     * @return true, if successful
     */
    public boolean existsProject(String workspace, String project) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.exists();
    }

    // Folder

    /**
     * Gets the folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the folder
     */
    public Folder getFolder(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.getFolder(path);
    }

    /**
     * Exists folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return true, if successful
     */
    public boolean existsFolder(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.existsFolder(path);
    }

    /**
     * Creates the folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the folder
     */
    public Folder createFolder(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.createFolder(path);
    }

    /**
     * Delete folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     */
    public void deleteFolder(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        projectObject.deleteFolder(path);
    }

    // File

    /**
     * Gets the file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the file
     */
    public File getFile(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.getFile(path);
    }

    /**
     * Exists file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return true, if successful
     */
    public boolean existsFile(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        return projectObject.existsFile(path);
    }

    /**
     * Creates the file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param content the content
     * @param contentType the content type
     * @return the file
     */
    public File createFile(String workspace, String project, String path, byte[] content, String contentType) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        if (contentType == null) {
            contentType = ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(path));
        }
        boolean isBinary = ContentTypeHelper.isBinary(contentType);
        File fileObject = projectObject.createFile(path, content, isBinary, contentType);
        triggerOnSaveExtensions(workspace, project, path);
        return fileObject;
    }

    /**
     * Triggers the post save file extensions.
     *
     * @param workspace the workspace
     * @param project the project name
     * @param path the file path
     */
    private void triggerOnSaveExtensions(String workspace, String project, String path) {
        try {
            Map<Object, Object> context = new HashMap<Object, Object>();
            context.put(EXTENSION_PARAMETER_WORKSPACE, workspace);
            context.put(EXTENSION_PARAMETER_PROJECT, project);
            context.put(EXTENSION_PARAMETER_PATH, path);
            String[] modules = ExtensionsFacade.getExtensions(EXTENSION_POINT_IDE_WORKSPACE_ON_SAVE);
            for (String module : modules) {
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Workspace On Save Extension: {} triggered...", module);
                    }
                    if (module != null) {
                        RepositoryPath repositoryPath = new RepositoryPath(module);
                        javascriptService.handleRequest(repositoryPath.getSegments()[0], repositoryPath.constructPathFrom(1), "", context,
                                false);
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Workspace On Save Extension: {} finshed.", module);
                    }
                } catch (Exception | Error e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Workspace On Save Extension {} failed with: {}.", module, e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Update file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param content the content
     * @return the file
     */
    public File updateFile(String workspace, String project, String path, byte[] content) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        File fileObject = projectObject.getFile(path);
        fileObject.getInternal()
                  .setContent(content);
        triggerOnSaveExtensions(workspace, project, path);
        return fileObject;
    }

    /**
     * Delete file.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     */
    public void deleteFile(String workspace, String project, String path) {
        Workspace workspaceObject = getWorkspace(workspace);
        Project projectObject = workspaceObject.getProject(project);
        projectObject.deleteFile(path);
    }

    /**
     * Gets the uri.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the uri
     * @throws URISyntaxException the URI syntax exception
     */
    public URI getURI(String workspace, String project, String path) throws URISyntaxException {
        StringBuilder relativePath = new StringBuilder(WORKSPACES_SERVICE_PREFIX).append(IRepositoryStructure.SEPARATOR)
                                                                                 .append(workspace);
        if (project != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(project);
        }
        if (path != null) {
            relativePath.append(IRepositoryStructure.SEPARATOR)
                        .append(path);
        }
        return new URI(UrlFacade.escape(relativePath.toString()));
    }

    /**
     * Render workspace tree.
     *
     * @param workspace the workspace
     * @return the workspace descriptor
     */
    public WorkspaceDescriptor renderWorkspaceTree(Workspace workspace) {
        return WorkspaceJsonHelper.describeWorkspace(workspace,
                IRepositoryStructure.PATH_USERS + IRepositoryStructure.SEPARATOR + UserFacade.getName(), "");
    }

    /**
     * Render project tree.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project descriptor
     */
    public ProjectDescriptor renderProjectTree(String workspace, Project project) {
        return WorkspaceJsonHelper.describeProject(workspace, project,
                IRepositoryStructure.PATH_USERS + IRepositoryStructure.SEPARATOR + UserFacade.getName(), "");
    }

    /**
     * Render folder tree.
     *
     * @param workspace the workspace
     * @param folder the folder
     * @return the folder descriptor
     */
    public FolderDescriptor renderFolderTree(String workspace, Folder folder) {
        return WorkspaceJsonHelper.describeFolder(workspace, folder,
                IRepositoryStructure.PATH_USERS + IRepositoryStructure.SEPARATOR + UserFacade.getName(), "");
    }

    /**
     * Render file descriptions.
     *
     * @param files the files
     * @return the file descriptors
     */
    public List<FileDescriptor> renderFileDescriptions(List<File> files) {
        List<FileDescriptor> fileDescriptors = new ArrayList<>();
        for (File file : files) {
            fileDescriptors.add(renderFileDescription("", file));
        }
        return fileDescriptors;
    }

    /**
     * Render file description.
     *
     * @param workspace the workspace
     * @param file the file
     * @return the file descriptor
     */
    public FileDescriptor renderFileDescription(String workspace, File file) {
        return WorkspaceJsonHelper.describeFile(workspace, file,
                IRepositoryStructure.PATH_USERS + IRepositoryStructure.SEPARATOR + UserFacade.getName(), "");
    }

    /**
     * Copy project.
     *
     * @param sourceWorkspace the source workspace
     * @param targetWorkspace the target workspace
     * @param sourceProject the source project
     * @param targetProject the target project
     */
    public void copyProject(String sourceWorkspace, String targetWorkspace, String sourceProject, String targetProject) {
        if (existsProject(targetWorkspace, targetProject)) {
            int inc = 1;
            String projectName = targetProject + " (copy " + inc + ")";
            while (existsProject(targetWorkspace, projectName)) {
                projectName = targetProject + " (copy " + ++inc + ")";
            }
            targetProject = projectName;
        }
        if (sourceWorkspace.equals(targetWorkspace)) {
            Workspace workspaceObject = getWorkspace(targetWorkspace);
            workspaceObject.copyProject(sourceProject, targetProject);
        } else { // This is a temporary workaround
            Workspace sourceWorkspaceObject = getWorkspace(sourceWorkspace);
            Project sourceProjectObject = sourceWorkspaceObject.getProject(sourceProject);
            String basePath = sourceProjectObject.getPath();
            Project targetProjectObject = createProject(targetWorkspace, targetProject);
            List<Pair<String, String>> allFilesFolders = getAllFilesFolders(sourceProjectObject, false);
            for (Pair<String, String> path : allFilesFolders) {
                if (path.getKey()
                        .equals("file")) {
                    String filePath = path.getValue()
                                          .replace(basePath, "");
                    File sourceFile = sourceProjectObject.getFile(filePath);
                    targetProjectObject.createFile(filePath, sourceFile.getContent(), sourceFile.isBinary(), sourceFile.getContentType());
                } else {
                    targetProjectObject.createFolder(path.getValue()
                                                         .replace(basePath + IRepository.SEPARATOR, "")
                            + IRepository.SEPARATOR);
                }
            }
        }
    }

    /**
     * Copy folder.
     *
     * @param sourceWorkspace the source workspace
     * @param targetWorkspace the target workspace
     * @param sourceProject the source project
     * @param sourceFolderPath the source folder path
     * @param targetProject the target project
     * @param targetBasePath the target folder path
     * @param targetFolderName the target folder name
     */
    public void copyFolder(String sourceWorkspace, String targetWorkspace, String sourceProject, String sourceFolderPath,
            String targetProject, String targetBasePath, String targetFolderName) {
        String targetFolderPath = targetBasePath + targetFolderName;
        if (existsFolder(targetWorkspace, targetProject, targetFolderPath)
                || existsFile(targetWorkspace, targetProject, targetFolderPath)) {
            int inc = 1;
            String folderName = targetFolderName + " (copy " + inc + ")";
            while (existsFolder(targetWorkspace, targetProject, targetBasePath + folderName)) {
                folderName = targetFolderName + " (copy " + ++inc + ")";
            }
            targetFolderPath = targetBasePath + folderName;
        }
        if (sourceWorkspace.equals(targetWorkspace)) {
            Workspace workspaceObject = getWorkspace(targetWorkspace);
            workspaceObject.copyFolder(sourceProject, sourceFolderPath, targetProject, targetFolderPath);
        } else { // This is a temporary workaround
            Workspace sourceWorkspaceObject = getWorkspace(sourceWorkspace);
            Workspace targetWorkspaceObject = getWorkspace(targetWorkspace);
            Project sourceProjectObject = sourceWorkspaceObject.getProject(sourceProject);
            Project targetProjectObject = targetWorkspaceObject.getProject(targetProject);
            Folder baseFolder = sourceProjectObject.getFolder(sourceFolderPath);
            String basePath = baseFolder.getPath();
            List<Pair<String, String>> allFilesFolders = getAllFilesFolders(baseFolder, false);
            targetProjectObject.createFolder(targetFolderPath + IRepository.SEPARATOR);
            for (Pair<String, String> path : allFilesFolders) {
                if (path.getKey()
                        .equals("file")) {
                    String filePath = path.getValue()
                                          .replace(basePath, "");
                    File sourceFile = baseFolder.getFile(filePath);
                    targetProjectObject.createFile(targetFolderPath + filePath, sourceFile.getContent(), sourceFile.isBinary(),
                            sourceFile.getContentType());
                } else {
                    targetProjectObject.createFolder(targetFolderPath + IRepository.SEPARATOR + path.getValue()
                                                                                                    .replace(basePath
                                                                                                            + IRepository.SEPARATOR, "")
                            + IRepository.SEPARATOR);
                }
            }
        }
    }

    /**
     * Copy file.
     *
     * @param sourceWorkspace the source workspace
     * @param targetWorkspace the target workspace
     * @param sourceProject the source project
     * @param sourceFilePath the source file path
     * @param targetProject the target project
     * @param targetFilePath the target file path
     */
    public void copyFile(String sourceWorkspace, String targetWorkspace, String sourceProject, String sourceFilePath, String targetProject,
            String targetFilePath) {
        if (sourceWorkspace.equals(targetWorkspace) && !existsFile(targetWorkspace, targetProject, targetFilePath)
                && !existsFolder(targetWorkspace, targetProject, targetFilePath)) {
            Workspace workspaceObject = getWorkspace(targetWorkspace);
            workspaceObject.copyFile(sourceProject, sourceFilePath, targetProject, targetFilePath);
        } else { // This is a temporary workaround
            Workspace sourceWorkspaceObject = getWorkspace(sourceWorkspace);
            Workspace targetWorkspaceObject = getWorkspace(targetWorkspace);
            File sourceFile = sourceWorkspaceObject.getProject(sourceProject)
                                                   .getFile(sourceFilePath);
            String baseTargetPath = "";
            if (targetFilePath.endsWith(IRepository.SEPARATOR)) {
                baseTargetPath = targetFilePath;
                targetFilePath += sourceFile.getName();
            } else {
                baseTargetPath = IRepository.SEPARATOR + targetFilePath;
                targetFilePath += IRepository.SEPARATOR + sourceFile.getName();
            }
            if (existsFile(targetWorkspace, targetProject, targetFilePath)
                    || existsFolder(targetWorkspace, targetProject, targetFilePath)) {
                String fileName = sourceFile.getName();
                String fileTitle = "";
                String fileExt = "";
                if (fileName.indexOf('.') != -1) {
                    fileTitle = fileName.substring(0, fileName.lastIndexOf("."));
                    fileExt = fileName.substring(fileName.lastIndexOf("."));
                } else {
                    fileTitle = fileName;
                }
                int inc = 1;
                fileName = fileTitle + " (copy " + inc + ")" + fileExt;
                while (existsFile(targetWorkspace, targetProject, baseTargetPath + IRepository.SEPARATOR + fileName)) {
                    fileName = fileTitle + " (copy " + ++inc + ")" + fileExt;
                }
                targetFilePath = baseTargetPath + IRepository.SEPARATOR + fileName;
            }
            targetWorkspaceObject.getProject(targetProject)
                                 .createFile(targetFilePath, sourceFile.getContent(), sourceFile.isBinary(), sourceFile.getContentType());
        }
    }

    /**
     * Move project.
     *
     * @param workspace the workspace
     * @param sourceProject the source project
     * @param targetProject the target project
     */
    public void moveProject(String workspace, String sourceProject, String targetProject) {
        Workspace workspaceObject = getWorkspace(workspace);
        workspaceObject.moveProject(sourceProject, targetProject);
    }

    /**
     * Move folder.
     *
     * @param workspace the workspace
     * @param sourceProject the source project
     * @param sourceFolderPath the source folder path
     * @param targetProject the target project
     * @param targetFolderPath the target folder path
     */
    public void moveFolder(String workspace, String sourceProject, String sourceFolderPath, String targetProject, String targetFolderPath) {
        Workspace workspaceObject = getWorkspace(workspace);
        workspaceObject.moveFolder(sourceProject, sourceFolderPath, targetProject, targetFolderPath);
    }

    // Search

    /**
     * Move file.
     *
     * @param workspace the workspace
     * @param sourceProject the source project
     * @param sourceFilePath the source file path
     * @param targetProject the target project
     * @param targetFilePath the target file path
     */
    public void moveFile(String workspace, String sourceProject, String sourceFilePath, String targetProject, String targetFilePath) {
        Workspace workspaceObject = getWorkspace(workspace);
        workspaceObject.moveFile(sourceProject, sourceFilePath, targetProject, targetFilePath);
    }

    // Find

    /**
     * Free-text search in the files content.
     *
     * @param workspace the workspace
     * @param term the term
     * @return the files list
     */
    public List<File> search(String workspace, String term) {
        Workspace workspaceObject = getWorkspace(workspace);
        return workspaceObject.search(term);
    }

    /**
     * Find files by pattern.
     *
     * @param pattern the pattern
     * @return the files list
     */
    public List<File> find(String pattern) {
        List<File> allFiles = new ArrayList<File>();
        List<Workspace> workspaces = getWorkspaces();
        for (Workspace workspace : workspaces) {
            Workspace workspaceObject = getWorkspace(workspace.getName());
            List<File> files = workspaceObject.find(pattern);
            allFiles.addAll(files);
        }
        return allFiles;
    }

    /**
     * Find files by pattern within a given workspace.
     *
     * @param workspace the workspace
     * @param pattern the pattern
     * @return the files list
     */
    public List<File> find(String workspace, String pattern) {
        Workspace workspaceObject = getWorkspace(workspace);
        return workspaceObject.find(pattern);
    }

    /**
     * Link project.
     *
     * @param workspace the workspace
     * @param sourceProject the source project
     * @param targetPath the target path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void linkProject(String workspace, String sourceProject, String targetPath) throws IOException {
        Workspace workspaceObject = getWorkspace(workspace);
        workspaceObject.linkProject(sourceProject, targetPath);
    }

    // Other

    /**
     * Gets the all files folders.
     *
     * @param baseFolder the base folder
     * @param includeBaseFolder the include base folder
     * @return the all files folders
     */
    private List<Pair<String, String>> getAllFilesFolders(Folder baseFolder, Boolean includeBaseFolder) {
        List<Pair<String, String>> allFilesFolders = new ArrayList<>();
        List<File> files = baseFolder.getFiles();
        List<Folder> folders = baseFolder.getFolders();
        if (files.size() == 0 && includeBaseFolder) {
            allFilesFolders.add(Pair.of("folder", baseFolder.getPath()));
        }
        for (File file : files) {
            allFilesFolders.add(Pair.of("file", file.getPath()));
        }
        for (Folder folder : folders) {
            allFilesFolders.addAll(getAllFilesFolders(folder, true));
        }
        return allFilesFolders;
    }

}
