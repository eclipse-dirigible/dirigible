/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.DecoderException;
import org.eclipse.dirigible.components.api.utils.UrlFacade;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class TransportService.
 */
@Service
public class TransportService {

    /** The workspace service. */
    @Autowired
    private WorkspaceService workspaceService;

    /** The repository. */
    @Autowired
    private IRepository repository;

    /**
     * Gets the workspace service.
     *
     * @return the workspace service
     */
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Import project.
     *
     * @param workspaceName the workspace name
     * @param content the content
     */
    public void importProjectInWorkspace(String workspaceName, InputStream content) {
        Workspace workspace = getWorkspace(workspaceName);
        String workspacePath = workspace.getPath();
        importProject(workspacePath, content);
    }

    /**
     * Import files from zip to folder.
     *
     * @param workspaceName the workspace name
     * @param projectName the project name
     * @param pathInProject the path in project
     * @param content the content
     * @param override the override
     */
    public void importZipToPath(String workspaceName, String projectName, String pathInProject, byte[] content, Boolean override) {
        if (override == null)
            override = true;
        Workspace workspace = getWorkspace(workspaceName);
        String projectPath = workspace.getProject(projectName)
                                      .getPath();
        String importPath = projectPath + IRepositoryStructure.SEPARATOR + pathInProject;
        getRepository().importZip(content, importPath, override, false, null);
    }

    /**
     * Import project in path.
     *
     * @param path the path
     * @param content the content
     */
    public void importProjectInPath(String path, InputStream content) {
        ICollection importedZipFolder = getOrCreateCollection(path);
        String importedZipFolderPath = importedZipFolder.getPath();
        importProject(importedZipFolderPath, content);
    }

    /**
     * Import project.
     *
     * @param path the path
     * @param content the content
     */
    private void importProject(String path, InputStream content) {
        ZipInputStream str = new ZipInputStream(content);
        getRepository().importZip(str, path, true);
    }

    /**
     * Gets the or create collection.
     *
     * @param path the path
     * @return the or create collection
     */
    private ICollection getOrCreateCollection(String path) {
        ICollection repositoryRootCollection = getRepository().getRoot();
        ICollection importedZipFolder = repositoryRootCollection.getCollection(path);

        if (!importedZipFolder.exists()) {
            importedZipFolder = repositoryRootCollection.createCollection(path);
        }

        return importedZipFolder;
    }

    /**
     * Export project.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the byte[]
     */
    public byte[] exportProject(String workspace, String project) {
        Workspace workspaceApi = getWorkspace(workspace);
        Project projectApi = getProject(workspaceApi, project);
        return getRepository().exportZip(projectApi.getPath(), true);
    }

    /**
     * Export workspace.
     *
     * @param workspace the workspace
     * @return the byte[]
     */
    public byte[] exportWorkspace(String workspace) {
        Workspace workspaceApi = getWorkspace(workspace);
        return getRepository().exportZip(workspaceApi.getPath(), false);
    }

    /**
     * Export folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @param folder the project
     * @return the byte[]
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws DecoderException the decoder exception
     */
    public byte[] exportFolder(String workspace, String project, String folder) throws UnsupportedEncodingException, DecoderException {
        Workspace workspaceApi = getWorkspace(workspace);
        Project projectApi = getProject(workspaceApi, project);
        String decodedPath = UrlFacade.decode(folder, null);
        return getRepository().exportZip(projectApi.getPath() + IRepositoryStructure.SEPARATOR + decodedPath, true);
    }

    /**
     * Gets the workspace.
     *
     * @param workspace the workspace
     * @return the workspace
     */
    private Workspace getWorkspace(String workspace) {
        return getWorkspaceService().getWorkspace(workspace);
    }

    /**
     * Gets the project.
     *
     * @param workspaceApi the workspace api
     * @param project the project
     * @return the project
     */
    private Project getProject(Workspace workspaceApi, String project) {
        return workspaceApi.getProject(project);
    }

    /**
     * Import snapshot.
     *
     * @param content the content
     */
    public void importSnapshot(byte[] content) {
        getRepository().importZip(content, IRepositoryStructure.SEPARATOR, true, false, null);
    }

    /**
     * Export snapshot.
     *
     * @return the byte[]
     */
    public byte[] exportSnapshot() {
        return getRepository().exportZip(IRepositoryStructure.SEPARATOR, true);
    }

    /**
     * Import files to folder.
     *
     * @param workspaceName the workspace name
     * @param projectName the project name
     * @param pathInProject the path in project
     * @param content the content
     */
    public void importFileToPath(String workspaceName, String projectName, String pathInProject, byte[] content) {
        Workspace workspace = getWorkspace(workspaceName);
        String projectPath = workspace.getProject(projectName)
                                      .getPath();
        String importPath = projectPath + IRepositoryStructure.SEPARATOR + pathInProject;
        if (getRepository().hasResource(importPath)) {
            getRepository().getResource(importPath)
                           .setContent(content);
        } else {
            getRepository().createResource(importPath, content);
        }
    }

}
