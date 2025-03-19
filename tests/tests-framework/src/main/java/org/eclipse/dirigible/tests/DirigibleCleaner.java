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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

class DirigibleCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCleaner.class);

    public static void deleteDirigibleFolder() {
        String dirigibleFolder = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue() + File.separator + "dirigible";
        LOGGER.info("Deleting dirigible folder [{}]...", dirigibleFolder);
        FileUtil.deleteFolder(dirigibleFolder);
    }

}
