/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.workspace.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.project.ProjectMetadata;
import org.eclipse.dirigible.components.project.ProjectMetadataDependency;
import org.eclipse.dirigible.components.project.ProjectMetadataUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.springframework.stereotype.Component;

/**
 * The ProjectMetadataManager is the controller of reading and writing the project.json files.
 */
@Component
public class ProjectMetadataManager {

    /** The default branch. */
    public static final String BRANCH_MASTER = "master"; //$NON-NLS-1$

    /**
     * Ensure project metadata.
     *
     * @param workspace the workspace
     * @param projectName the project name
     */
    public void ensureProjectMetadata(Workspace workspace, String projectName) {
        Project project = workspace.getProject(projectName);
        File projectFile = project.getFile(ProjectMetadata.PROJECT_METADATA_FILE_NAME);
        if (!projectFile.exists()) {
            ProjectMetadata projectMetadata = new ProjectMetadata();
            projectMetadata.setGuid(project.getName());
            String projectMetadataJson = ProjectMetadataUtils.toJson(projectMetadata);
            project.createFile(ProjectMetadata.PROJECT_METADATA_FILE_NAME, projectMetadataJson.getBytes(StandardCharsets.UTF_8));
        } else {
            // TODO update branch info?
        }

    }

    /**
     * Gets the dependencies.
     *
     * @param selectedProject the selected project
     * @return the dependencies
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static ProjectMetadataDependency[] getDependencies(Project selectedProject) throws IOException {
        ProjectMetadata projectMetadata = getProjectMetadata(selectedProject);
        if (projectMetadata == null) {
            return new ProjectMetadataDependency[] {};
        }
        return projectMetadata.getDependencies();
    }

    /**
     * Returns whether the file is "project.json".
     *
     * @param file the file
     * @return true if it's "project.json" file
     */
    public static boolean isProjectMetadata(java.io.File file) {
        return file.exists() && file.isFile() && file.getName()
                                                     .equals(ProjectMetadata.PROJECT_METADATA_FILE_NAME);
    }

    /**
     * Gets the project metadata.
     *
     * @param selectedProject the selected project
     * @return the project metadata
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static ProjectMetadata getProjectMetadata(Project selectedProject) throws IOException {
        File projectFile = selectedProject.getFile(ProjectMetadata.PROJECT_METADATA_FILE_NAME);
        if (!projectFile.exists()) {
            return null;
        }
        String content = IOUtils.toString(projectFile.getContent());
        return ProjectMetadataUtils.fromJson(content);
    }
}
