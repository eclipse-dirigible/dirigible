/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class DirigibleCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCleaner.class);

    private final DataSourcesManager dataSourcesManager;
    private final IRepository dirigibleRepo;
    private final BpmService bpmService;

    DirigibleCleaner(DataSourcesManager dataSourcesManager, IRepository dirigibleRepo, BpmService bpmService) {
        this.dataSourcesManager = dataSourcesManager;
        this.dirigibleRepo = dirigibleRepo;
        this.bpmService = bpmService;
    }

    public static void beforeTestClassCleanup() {
        LOGGER.info("Executing before test class cleanup...");
        closeFlowable();
        deleteDirigibleFolder();
    }

    private static void closeFlowable() {
        // ProcessEngines.destroy();
    }

    private static void deleteDirigibleFolder() {
        String repoFolderPath = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue() + File.separator + "dirigible";
        FileUtil.deleteFolder(repoFolderPath);
    }

    public void afterEachMethodCleanup() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Cleaning after test method execution...");
        closeFlowable();

        deleteDirigibleFolder();
        LOGGER.info("Dirigible has been cleaned up. It took [{}] ms", System.currentTimeMillis() - startTime);
    }

}
