/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.cli.server;

import org.apache.commons.io.FileUtils;
import org.eclipse.dirigible.cli.DirigibleServerException;
import org.eclipse.dirigible.cli.util.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Component
public class DirigibleServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleServer.class);

    private final ProcessManager processManager;

    DirigibleServer(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public int start(DirigibleServerConfig serverConfig) {
        deleteServerFolder();

        if (serverConfig.isWatchMode()) {
            return startInWatchMode(serverConfig);
        }

        copyProjectToRegistry(serverConfig);

        LOGGER.info("Starting Eclipse Dirigible server...");
        return processManager.startSynchronously("java", "-jar", serverConfig.getServerJarPath()
                                                                             .toString());
    }

    private int startInWatchMode(DirigibleServerConfig serverConfig) {
        LOGGER.info("Starting Eclipse Dirigible in watch mode...");
        Path projectPath = serverConfig.getProjectPath();
        Map<String, String> envVariables = Map.of(//
                "DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER_AS_SUBFOLDER", "true", //
                "DIRIGIBLE_REGISTRY_EXTERNAL_IGNORED_FOLDERS", "target,node_modules", //
                "DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER", projectPath.toString());

        return processManager.startSynchronously(envVariables, "java", "-jar", serverConfig.getServerJarPath()
                                                                                           .toString());
    }

    private void copyProjectToRegistry(DirigibleServerConfig serverConfig) throws DirigibleServerException {
        Path projectPath = serverConfig.getProjectPath();
        LOGGER.info("Copying project from path [{}] to the server's registry folder...", projectPath);
        File source = projectPath.toFile();

        Path registryProjectPath = Path.of(getServerDir().toString(), "repository", "root", "registry", "public", projectPath.getFileName()
                                                                                                                             .toString());
        File target = registryProjectPath.toFile();

        try {
            FileUtils.copyDirectory(source, target);
        } catch (IOException ex) {
            throw new DirigibleServerException(
                    "Unable to copy project to server registry. Failed to copy [" + source + "] to [" + target + "]", ex);
        }
    }

    private void deleteServerFolder() {
        Path serverDir = getServerDir();
        deleteServerFolder(serverDir);
    }

    private Path getServerDir() {
        // server target folder is located in the folder where the command is executed
        String userDir = System.getProperty("user.dir");
        return Path.of(userDir, "target", "dirigible");
    }

    private void deleteServerFolder(Path serverDir) {
        try {
            LOGGER.info("Deleting server folder at path [{}]...", serverDir);
            FileUtils.deleteDirectory(serverDir.toFile());
        } catch (IOException ex) {
            LOGGER.warn("Failed to delete server dir [{}]", serverDir, ex);
        }
    }
}
