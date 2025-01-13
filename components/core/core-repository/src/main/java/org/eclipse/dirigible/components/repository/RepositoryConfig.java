/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.repository;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Class RepositoryConfig.
 */
@org.springframework.context.annotation.Configuration
public class RepositoryConfig {

    /**
     * Repository.
     *
     * @return the i repository
     */
    @Bean
    public IRepository repository() {
        String repoFolderPath = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue();
        Path path = Paths.get(repoFolderPath);
        boolean absolutePath = path.isAbsolute();
        boolean versioningEnabled = Boolean.parseBoolean(Configuration.get("DIRIGIBLE_REPOSITORY_VERSIONING_ENABLED", "false"));
        LocalRepository localRepository = new LocalRepository(repoFolderPath, absolutePath, versioningEnabled);

        // To be removed once moved to Spring entirely
        StaticObjects.set(StaticObjects.REPOSITORY, localRepository);

        return localRepository;
    }
}
