/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.local;

import java.io.File;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryWriteException;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;

/**
 * The Local Workspace Mapper.
 */
public class LocalWorkspaceMapper {


    /**
     * Gets the mapped name.
     *
     * @param repository the repository
     * @param repositoryName the repository name
     * @return the mapped name
     * @throws RepositoryWriteException the repository write exception
     */
    public static String getMappedName(FileSystemRepository repository, String repositoryName) throws RepositoryWriteException {
        String workspaceName = null;

        if (repositoryName != null) {
            if (repositoryName.startsWith(repository.getRepositoryPath())) {
                workspaceName = repositoryName;
            } else {
                workspaceName = repository.getRepositoryPath() + repositoryName;
            }
        }

        if (workspaceName != null && !IRepository.SEPARATOR.contentEquals(File.separator)) {
            workspaceName = workspaceName.replace(IRepository.SEPARATOR, File.separator);
        }

        return workspaceName;
    }

}
