/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.domain;

import java.io.File;
import java.io.IOException;

import org.eclipse.dirigible.components.ide.git.command.StatusCommand;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatus;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Class GitProjectStatusProvider.
 */
public class GitProjectStatusProvider implements ProjectStatusProvider {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GitProjectStatusProvider.class);

    /** The status command. */
    private StatusCommand statusCommand;

    /**
     * Instantiates a new git project status provider.
     *
     * @param statusCommand the status command
     */
    @Autowired
    public GitProjectStatusProvider(StatusCommand statusCommand) {
        this.statusCommand = statusCommand;
    }

    /**
     * Gets the project status.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project status
     */
    @Override
    public ProjectStatus getProjectStatus(String workspace, String project) {
        try {
            return statusCommand.execute(workspace, project);
        } catch (GitConnectorException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Gets the project git folder.
     *
     * @param workspace the workspace
     * @param project the project
     * @return the project git folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public String getProjectGitFolder(String workspace, String project) throws IOException {
        File gitDirectory = GitFileUtils.getGitDirectoryByRepositoryName(workspace, project)
                                        .getCanonicalFile();
        String git = gitDirectory.getCanonicalPath() + File.separator;
        return git;
    }

}
