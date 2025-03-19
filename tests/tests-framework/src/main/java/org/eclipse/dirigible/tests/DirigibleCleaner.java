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
import org.eclipse.dirigible.tests.util.FileUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
class DirigibleCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCleaner.class);

    private final Scheduler scheduler;

    DirigibleCleaner(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void clean() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Cleaning up Dirigible resources...");

        stopQuartz();
        deleteDirigibleFolder();

        LOGGER.info("Dirigible resources have been cleaned up. It took [{}] ms", System.currentTimeMillis() - startTime);
    }

    private void stopQuartz() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException | RuntimeException ex) {
            LOGGER.warn("Failed to shutdown quartz", ex);
        }
    }

    public static void deleteDirigibleFolder() {
        String dirigibleFolder = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue() + File.separator + "dirigible";
        String skippedDirPath = dirigibleFolder + File.separator + "repository" + File.separator + "index";
        LOGGER.info("Deleting dirigible folder [{}] by skipping [{}]", dirigibleFolder, skippedDirPath);
        try {
            FileUtil.deleteFolder(dirigibleFolder, skippedDirPath);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete dirigible folder [" + dirigibleFolder + "] by skipping [" + skippedDirPath + "]", ex);
        }
    }

}
