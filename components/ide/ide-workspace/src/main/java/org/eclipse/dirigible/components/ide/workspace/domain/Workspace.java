/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.ide.workspace.json.WorkspaceGitHelper;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Workspace's Workspace.
 */
public class Workspace extends Folder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(Workspace.class);

    /**
     * Instantiates a new workspace.
     *
     * @param workspaceCollection the workspace collection
     */
    public Workspace(ICollection workspaceCollection) {
        super(workspaceCollection);
    }

    /**
     * Creates the project.
     *
     * @param name the name
     * @return the i project
     */
    public Project createProject(String name) {
        ICollection collection = this.createCollection(name);
        return new Project(collection);
    }

    /**
     * Gets the project.
     *
     * @param name the name
     * @return the project
     */
    public Project getProject(String name) {
        ICollection collection = this.getCollection(name);
        return new Project(collection);
    }

    /**
     * Gets the projects.
     *
     * @return the projects
     */
    public List<Project> getProjects() {
        List<Project> projects = new ArrayList<Project>();
        List<ICollection> collections = this.getCollections();
        for (ICollection collection : collections) {
            projects.add(new Project(collection));
        }
        return projects;
    }

    /**
     * Delete project.
     *
     * @param name the name
     */
    public void deleteProject(String name) {
        ICollection projectCollection = this.getCollection(name);
        java.io.File gitFolder = WorkspaceGitHelper.getGitFolderForProject(projectCollection.getRepository(), projectCollection.getPath());
        if (gitFolder != null && gitFolder.exists()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(gitFolder.getParentFile());
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        this.removeCollection(name);
    }

    /**
     * Copy project.
     *
     * @param sourceProject the source project
     * @param targetProject the target project
     */
    public void copyProject(String sourceProject, String targetProject) {
        ICollection collection = this.createCollection(sourceProject);
        collection.copyTo(this.getCollection(targetProject)
                              .getPath());
    }

    /**
     * Copy folder.
     *
     * @param sourceProject the source project
     * @param sourceFolderPath the source folder path
     * @param targetProject the target project
     * @param targetFolderPath the target folder path
     */
    public void copyFolder(String sourceProject, String sourceFolderPath, String targetProject, String targetFolderPath) {
        ICollection collection = this.createCollection(sourceProject);
        collection.getCollection(sourceFolderPath)
                  .copyTo(this.getCollection(targetProject)
                              .getCollection(targetFolderPath)
                              .getPath());
    }

    /**
     * Copy file.
     *
     * @param sourceProject the source project
     * @param sourceFilePath the source file path
     * @param targetProject the target project
     * @param targetFilePath the target file path
     */
    public void copyFile(String sourceProject, String sourceFilePath, String targetProject, String targetFilePath) {
        ICollection collection = this.createCollection(sourceProject);
        collection.getResource(sourceFilePath)
                  .copyTo(this.getCollection(targetProject)
                              .getResource(targetFilePath)
                              .getPath());
    }

    /**
     * Move project.
     *
     * @param sourceProject the source project
     * @param targetProject the target project
     */
    public void moveProject(String sourceProject, String targetProject) {
        ICollection collection = this.createCollection(sourceProject);
        collection.moveTo(this.getCollection(targetProject)
                              .getPath());
    }

    /**
     * Move folder.
     *
     * @param sourceProject the source project
     * @param sourceFolderPath the source folder path
     * @param targetProject the target project
     * @param targetFolderPath the target folder path
     */
    public void moveFolder(String sourceProject, String sourceFolderPath, String targetProject, String targetFolderPath) {
        ICollection collection = this.createCollection(sourceProject);
        collection.getCollection(sourceFolderPath)
                  .moveTo(this.getCollection(targetProject)
                              .getCollection(targetFolderPath)
                              .getPath());
    }

    /**
     * Move file.
     *
     * @param sourceProject the source project
     * @param sourceFilePath the source file path
     * @param targetProject the target project
     * @param targetFilePath the target file path
     */
    public void moveFile(String sourceProject, String sourceFilePath, String targetProject, String targetFilePath) {
        ICollection collection = this.createCollection(sourceProject);
        collection.getResource(sourceFilePath)
                  .moveTo(this.getCollection(targetProject)
                              .getResource(targetFilePath)
                              .getPath());
    }

    /**
     * Link project.
     *
     * @param sourceProject the source project
     * @param targetPath the target path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void linkProject(String sourceProject, String targetPath) throws IOException {
        this.getRepository()
            .linkPath(this.getPath() + IRepository.SEPARATOR + sourceProject, targetPath);

    }

}
