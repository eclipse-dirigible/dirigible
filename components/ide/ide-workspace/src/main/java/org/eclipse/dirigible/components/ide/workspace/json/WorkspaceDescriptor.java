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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.local.LocalWorkspaceMapper;

/**
 * The Workspace Descriptor transport object.
 */
public class WorkspaceDescriptor {

    /** The name. */
    private String name;

    /** The path. */
    private String path;

    /** The type. */
    private String type = "workspace";

    /** The projects. */
    private List<ProjectDescriptor> projects = new ArrayList<ProjectDescriptor>();

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the new path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the projects.
     *
     * @return the projects
     */
    public List<ProjectDescriptor> getProjects() {
        return projects;
    }

    /**
     * Get the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the.
     *
     * @param projects the projects
     */
    public void set(List<ProjectDescriptor> projects) {
        this.projects = projects;
    }

    /**
     * Get the git folder.
     *
     * @param repository the repository
     * @param repositoryPath the path
     * @return the canonical file per project path
     */
    public static File getCanonicalFilePerProjectPath(IRepository repository, String repositoryPath) {
        try {
            if (repository instanceof FileSystemRepository) {
                String path = LocalWorkspaceMapper.getMappedName((FileSystemRepository) repository, repositoryPath);
                return new File(path).getCanonicalFile();
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

}
