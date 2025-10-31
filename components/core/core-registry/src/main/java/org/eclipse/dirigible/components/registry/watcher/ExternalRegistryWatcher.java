/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Scope("singleton")
public class ExternalRegistryWatcher extends RecursiveFolderWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalRegistryWatcher.class);

    private final IRepository repository;

    @Autowired
    public ExternalRegistryWatcher(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Initialize.
     */
    public void initialize() {
        String source = DirigibleConfig.REGISTRY_EXTERNAL_FOLDER.getStringValue();
        if (null != source) {
            String target = this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
            boolean sourceAsSubfolder = DirigibleConfig.REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER.getBooleanValue();

            Set<String> ignoredFolders = getIgnoredFolders();

            initialize(source, target, sourceAsSubfolder, ignoredFolders);
        } else {
            LOGGER.info("External registry is NOT configured.");
        }

    }

    private Set<String> getIgnoredFolders() {
        String ignoredFolders = DirigibleConfig.REGISTRY_EXTERNAL_IGNORED_FOLDERS.getStringValue();
        if (null == ignoredFolders) {
            return Collections.emptySet();
        }
        String[] folders = ignoredFolders.split(",");
        return Arrays.stream(folders)
                     .map(String::trim)
                     .collect(Collectors.toSet());

    }

}
