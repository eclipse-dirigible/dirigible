/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.project;

import java.io.File;

import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.components.ide.git.utils.GitFileUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.springframework.stereotype.Component;

/**
 * Verify that the given project is Git aware.
 */
@Component
public class ProjectPropertiesVerifier {

    /** The Constant DOT_GIT. */
    private static final String DOT_GIT = ".git";

    /** The repository. */
    private IRepository repository = null;

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    protected synchronized IRepository getRepository() {
        if (repository == null) {
            repository = (IRepository) StaticObjects.get(StaticObjects.REPOSITORY);
        }
        return repository;
    }

    /**
     * Verify.
     *
     * @param workspace the workspace
     * @param repositoryName the repositoryName
     * @return true, if successful
     */
    public boolean verify(String workspace, String repositoryName) {
        try {
            if (getRepository() instanceof FileSystemRepository) {
                File gitRepository = GitFileUtils.getGitDirectoryByRepositoryName(workspace, repositoryName);
                return (gitRepository != null);
            }
        } catch (Exception e) {
            // do nothing
        }
        return false;
    }

}
